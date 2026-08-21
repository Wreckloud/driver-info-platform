param(
    [string]$AdminUsername = 'admin',
    [string]$ViewerUsername = 'HYHTLLWLYXGS'
)

$ErrorActionPreference = 'Stop'
$projectPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runtimePath = Join-Path $projectPath 'runtime'
$mysqlDataPath = Join-Path $runtimePath 'mysql-data'
$logPath = Join-Path $runtimePath 'logs'
$passwordHashPath = Join-Path $runtimePath 'admin-password.hash'
$viewerPasswordHashPath = Join-Path $runtimePath 'viewer-password.hash'
$mysqlPort = 3307
$backendPort = 8080
$frontendPort = 5173

function Assert-PortAvailable([int]$Port) {
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
        throw "Port $Port is already in use. Run scripts/stop-local.ps1 or close the existing service."
    }
}

function Wait-Port([int]$Port, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
            return
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for port $Port. Check runtime/logs for details."
}

Assert-PortAvailable $mysqlPort
Assert-PortAvailable $backendPort
Assert-PortAvailable $frontendPort

$mysqldPath = (Get-Command mysqld.exe -ErrorAction Stop).Source
$mysqlPath = (Get-Command mysql.exe -ErrorAction Stop).Source
$mysqlBasePath = Split-Path -Parent (Split-Path -Parent $mysqldPath)
New-Item -ItemType Directory -Path $runtimePath, $logPath -Force | Out-Null

if (-not (Test-Path -LiteralPath (Join-Path $mysqlDataPath 'mysql'))) {
    New-Item -ItemType Directory -Path $mysqlDataPath -Force | Out-Null
    & $mysqldPath --no-defaults --initialize-insecure --basedir="$mysqlBasePath" --datadir="$mysqlDataPath" --console
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to initialize the isolated local MySQL data directory.'
    }
}

if (-not (Test-Path -LiteralPath $passwordHashPath)) {
    $securePassword = Read-Host 'Set the local administrator password (at least 12 characters)' -AsSecureString
    $credential = [System.Net.NetworkCredential]::new('', $securePassword)
    $plainPassword = $credential.Password
    if ($plainPassword.Length -lt 12) {
        throw 'Administrator password must contain at least 12 characters.'
    }
    try {
        $env:ADMIN_PASSWORD_PLAIN = $plainPassword
        Push-Location (Join-Path $projectPath 'backend')
        try {
            $passwordHash = mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.1:java '-Dexec.mainClass=com.wreckloud.driver.tool.PasswordHashTool'
        } finally {
            Pop-Location
        }
        Set-Content -LiteralPath $passwordHashPath -Value $passwordHash.Trim() -Encoding utf8NoBOM
    } finally {
        Remove-Item Env:ADMIN_PASSWORD_PLAIN -ErrorAction SilentlyContinue
        $plainPassword = $null
        $credential = $null
    }
}

if (-not (Test-Path -LiteralPath $viewerPasswordHashPath)) {
    $securePassword = Read-Host 'Set the local read-only account password (at least 12 characters)' -AsSecureString
    $credential = [System.Net.NetworkCredential]::new('', $securePassword)
    $plainPassword = $credential.Password
    if ($plainPassword.Length -lt 12) {
        throw 'Read-only account password must contain at least 12 characters.'
    }
    try {
        $env:ADMIN_PASSWORD_PLAIN = $plainPassword
        Push-Location (Join-Path $projectPath 'backend')
        try {
            $passwordHash = mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.1:java '-Dexec.mainClass=com.wreckloud.driver.tool.PasswordHashTool'
        } finally {
            Pop-Location
        }
        Set-Content -LiteralPath $viewerPasswordHashPath -Value $passwordHash.Trim() -Encoding utf8NoBOM
    } finally {
        Remove-Item Env:ADMIN_PASSWORD_PLAIN -ErrorAction SilentlyContinue
        $plainPassword = $null
        $credential = $null
    }
}

$mysqlArguments = @(
    '--no-defaults',
    "--basedir=`"$mysqlBasePath`"",
    "--datadir=$mysqlDataPath",
    "--port=$mysqlPort",
    '--bind-address=127.0.0.1',
    '--character-set-server=utf8mb4',
    '--collation-server=utf8mb4_0900_ai_ci',
    '--default-time-zone=+00:00',
    "--log-error=$(Join-Path $logPath 'mysql.log')"
)
$mysqlProcess = Start-Process -FilePath $mysqldPath -ArgumentList $mysqlArguments -WindowStyle Hidden -PassThru
Wait-Port $mysqlPort 30

& $mysqlPath --protocol=tcp --host=127.0.0.1 --port=$mysqlPort --user=root --execute="CREATE DATABASE IF NOT EXISTS driver_info CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to prepare the local driver_info database.'
}

$env:DB_URL = "jdbc:mysql://127.0.0.1:$mysqlPort/driver_info?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = ''
$env:ADMIN_USERNAME = $AdminUsername
$env:ADMIN_PASSWORD_BCRYPT = (Get-Content -LiteralPath $passwordHashPath -Raw).Trim()
$env:VIEWER_USERNAME = $ViewerUsername
$env:VIEWER_PASSWORD_BCRYPT = (Get-Content -LiteralPath $viewerPasswordHashPath -Raw).Trim()
$env:PHOTO_STORAGE_PATH = (Join-Path $runtimePath 'uploads')
$env:TENCENT_MAP_KEY = ''
$env:SESSION_COOKIE_SECURE = 'false'

try {
    $backendProcess = Start-Process -FilePath 'mvn.cmd' -ArgumentList @('-q', 'spring-boot:run') `
        -WorkingDirectory (Join-Path $projectPath 'backend') -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logPath 'backend.log') `
        -RedirectStandardError (Join-Path $logPath 'backend-error.log')
} finally {
    Remove-Item Env:DB_URL, Env:DB_USERNAME, Env:DB_PASSWORD, Env:ADMIN_USERNAME, `
        Env:ADMIN_PASSWORD_BCRYPT, Env:VIEWER_USERNAME, Env:VIEWER_PASSWORD_BCRYPT, Env:PHOTO_STORAGE_PATH, `
        Env:TENCENT_MAP_KEY, Env:SESSION_COOKIE_SECURE -ErrorAction SilentlyContinue
}
Wait-Port $backendPort 60

$frontendProcess = Start-Process -FilePath 'npm.cmd' -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1', '--port', $frontendPort) `
    -WorkingDirectory (Join-Path $projectPath 'frontend') -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $logPath 'frontend.log') `
    -RedirectStandardError (Join-Path $logPath 'frontend-error.log')
Wait-Port $frontendPort 30

$processInfo = [ordered]@{
    mysqlPid = $mysqlProcess.Id
    backendLauncherPid = $backendProcess.Id
    frontendLauncherPid = $frontendProcess.Id
    startedAt = (Get-Date).ToString('o')
}
$processInfo | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimePath 'local-processes.json') -Encoding utf8NoBOM

Write-Output 'Local environment is ready.'
Write-Output "Driver page: http://127.0.0.1:$frontendPort/driver"
Write-Output "Admin page:  http://127.0.0.1:$frontendPort/admin/login"
Write-Output "Admin user:  $AdminUsername"
Write-Output "Viewer user: $ViewerUsername"
Write-Output "Logs:        $logPath"
