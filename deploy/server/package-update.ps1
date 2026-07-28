param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$projectDir = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$releaseRoot = Join-Path $projectDir 'release'
$updateName = 'driver-info-location-address-update'
$updateDir = Join-Path $releaseRoot $updateName
$archiveFile = Join-Path $releaseRoot "$updateName.tar.gz"

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
}
finally {
    Pop-Location
}

$resolvedReleaseRoot = [System.IO.Path]::GetFullPath($releaseRoot)
$resolvedUpdateDir = [System.IO.Path]::GetFullPath($updateDir)
if (-not $resolvedUpdateDir.StartsWith($resolvedReleaseRoot + [System.IO.Path]::DirectorySeparatorChar,
        [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Refusing to recreate an update outside the release directory.'
}

if (Test-Path -LiteralPath $updateDir) {
    Remove-Item -LiteralPath $updateDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Join-Path $updateDir 'artifacts\backend') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $updateDir 'artifacts\frontend') | Out-Null

Copy-Item -LiteralPath (Join-Path $projectDir 'backend\target\driver-info-platform-1.0.0.jar') `
    -Destination (Join-Path $updateDir 'artifacts\backend\app.jar')
Copy-Item -Path (Join-Path $projectDir 'frontend\dist\*') `
    -Destination (Join-Path $updateDir 'artifacts\frontend') -Recurse

if (Test-Path -LiteralPath $archiveFile) {
    Remove-Item -LiteralPath $archiveFile -Force
}
Push-Location $updateDir
try {
    Invoke-CheckedCommand -Command 'tar' -Arguments @('-czf', $archiveFile, 'artifacts')
}
finally {
    Pop-Location
}

Write-Host "Update archive created: $archiveFile"
Write-Host 'This update archive does not contain .env or other deployment secrets.'
