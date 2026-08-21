param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$projectDir = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$envFile = Join-Path $projectDir '.env'
$releaseRoot = Join-Path $projectDir 'release'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw '.env does not exist.'
}

function Get-DotEnvValue {
    param([Parameter(Mandatory)][string]$Name)

    $line = Get-Content -LiteralPath $envFile |
        Where-Object { $_ -match "^$([regex]::Escape($Name))=" } |
        Select-Object -Last 1
    if (-not $line) {
        return $null
    }
    $value = $line.Substring($line.IndexOf('=') + 1).Trim()
    if (($value.StartsWith("'") -and $value.EndsWith("'")) -or
        ($value.StartsWith('"') -and $value.EndsWith('"'))) {
        return $value.Substring(1, $value.Length - 2)
    }
    return $value
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory)][string]$Command,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed with exit code $LASTEXITCODE."
    }
}

$appVersion = Get-DotEnvValue -Name 'APP_VERSION'
$publicBaseUrl = Get-DotEnvValue -Name 'PUBLIC_BASE_URL'
$adminPasswordBcrypt = Get-DotEnvValue -Name 'ADMIN_PASSWORD_BCRYPT'
$viewerPasswordBcrypt = Get-DotEnvValue -Name 'VIEWER_PASSWORD_BCRYPT'
if ([string]::IsNullOrWhiteSpace($appVersion)) {
    $appVersion = '1.2.0'
}
if ($publicBaseUrl -notmatch '^https://[^/\s]+$') {
    throw 'PUBLIC_BASE_URL must be a complete HTTPS origin without a trailing path.'
}
if ($adminPasswordBcrypt -notmatch '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$') {
    throw 'ADMIN_PASSWORD_BCRYPT must contain a BCrypt hash, not the administrator plaintext password.'
}
if ($viewerPasswordBcrypt -notmatch '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$') {
    throw 'VIEWER_PASSWORD_BCRYPT must contain a BCrypt hash, not the viewer plaintext password.'
}

Get-Command mvn -ErrorAction Stop | Out-Null
Get-Command npm -ErrorAction Stop | Out-Null
Get-Command tar -ErrorAction Stop | Out-Null

Push-Location (Join-Path $projectDir 'backend')
try {
    $mavenArguments = if ($SkipTests) { @('-q', '-DskipTests', 'package') } else { @('-q', 'test', 'package') }
    Invoke-CheckedCommand -Command 'mvn' -Arguments $mavenArguments
}
finally {
    Pop-Location
}

Push-Location (Join-Path $projectDir 'frontend')
try {
    Invoke-CheckedCommand -Command 'npm' -Arguments @('ci', '--no-audit', '--no-fund')
    if (-not $SkipTests) {
        Invoke-CheckedCommand -Command 'npm' -Arguments @('test')
    }
    Invoke-CheckedCommand -Command 'npm' -Arguments @('run', 'build')
    $previousPublicBaseUrl = $env:PUBLIC_BASE_URL
    try {
        $env:PUBLIC_BASE_URL = $publicBaseUrl
        Invoke-CheckedCommand -Command 'npm' -Arguments @('run', 'generate:qr')
    }
    finally {
        $env:PUBLIC_BASE_URL = $previousPublicBaseUrl
    }
}
finally {
    Pop-Location
}

$bundleName = "driver-info-platform-$appVersion"
$bundleDir = Join-Path $releaseRoot $bundleName
$resolvedReleaseRoot = [System.IO.Path]::GetFullPath($releaseRoot)
$resolvedBundleDir = [System.IO.Path]::GetFullPath($bundleDir)
if (-not $resolvedBundleDir.StartsWith($resolvedReleaseRoot + [System.IO.Path]::DirectorySeparatorChar,
        [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Refusing to recreate a bundle outside the release directory.'
}

if (Test-Path -LiteralPath $bundleDir) {
    Remove-Item -LiteralPath $bundleDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path (Join-Path $bundleDir 'artifacts\backend') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $bundleDir 'artifacts\frontend') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $bundleDir 'artifacts\qrcode') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $bundleDir 'deploy') | Out-Null

Copy-Item -LiteralPath (Join-Path $projectDir 'backend\target\driver-info-platform-1.2.0.jar') `
    -Destination (Join-Path $bundleDir 'artifacts\backend\app.jar')
Copy-Item -Path (Join-Path $projectDir 'frontend\dist\*') `
    -Destination (Join-Path $bundleDir 'artifacts\frontend') -Recurse
Copy-Item -Path (Join-Path $projectDir 'frontend\dist-qr\*') `
    -Destination (Join-Path $bundleDir 'artifacts\qrcode') -Recurse
Copy-Item -LiteralPath (Join-Path $projectDir 'deploy\server') `
    -Destination (Join-Path $bundleDir 'deploy\server') -Recurse
Copy-Item -LiteralPath (Join-Path $projectDir 'docker-compose.server.yml') `
    -Destination (Join-Path $bundleDir 'docker-compose.yml')

$secretNames = @('MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD', 'ADMIN_PASSWORD_BCRYPT', 'VIEWER_PASSWORD_BCRYPT', 'TENCENT_MAP_KEY')
$deploymentEnv = foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $line
        continue
    }
    $name = $Matches[1]
    if ($name -notin $secretNames) {
        $line
        continue
    }
    $value = Get-DotEnvValue -Name $name
    if ($value.Contains("'")) {
        throw "$name contains a single quote and cannot be written safely to the deployment .env."
    }
    "$name='$value'"
}
$deploymentEnv | Set-Content -LiteralPath (Join-Path $bundleDir '.env') -Encoding utf8NoBOM

$archiveFile = Join-Path $releaseRoot "$bundleName.tar.gz"
if (Test-Path -LiteralPath $archiveFile) {
    Remove-Item -LiteralPath $archiveFile -Force
}
Push-Location $releaseRoot
try {
    Invoke-CheckedCommand -Command 'tar' -Arguments @('-czf', $archiveFile, $bundleName)
}
finally {
    Pop-Location
}

Write-Host "Release archive created: $archiveFile"
Write-Host 'The archive contains .env secrets. Keep it private and delete obsolete copies.'
