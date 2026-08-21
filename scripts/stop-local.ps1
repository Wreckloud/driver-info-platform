$ErrorActionPreference = 'Stop'
$projectPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runtimePath = Join-Path $projectPath 'runtime'
$processInfoPath = Join-Path $runtimePath 'local-processes.json'

function Stop-LocalProcessTree([int]$ProcessId) {
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-LocalProcessTree -ProcessId $child.ProcessId
    }
    if (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
}

$mysqlAdmin = Get-Command mysqladmin.exe -ErrorAction SilentlyContinue
if ($mysqlAdmin) {
    & $mysqlAdmin.Source --protocol=tcp --host=127.0.0.1 --port=3307 --user=root shutdown 2>$null
}

if (Test-Path $processInfoPath) {
    $processInfo = Get-Content $processInfoPath -Raw | ConvertFrom-Json
    foreach ($launcherPid in @($processInfo.backendLauncherPid, $processInfo.frontendLauncherPid)) {
        if ($launcherPid) {
            Stop-LocalProcessTree -ProcessId ([int]$launcherPid)
        }
    }
}

foreach ($port in @(8080, 5173)) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine -like "*$projectPath*") {
            Stop-Process -Id $listener.OwningProcess -Force
        }
    }
}

Write-Output 'Local DriverInfoPlatform services have been stopped.'
