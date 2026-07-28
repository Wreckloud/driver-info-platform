param(
    [Parameter(Mandatory = $true)]
    [string]$AdminPassword
)

$ErrorActionPreference = 'Stop'
$baseUrl = 'http://127.0.0.1:5173'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw $Message
    }
}

$driverPage = Invoke-WebRequest -Uri "$baseUrl/driver" -UseBasicParsing
Assert-True ($driverPage.StatusCode -eq 200) 'Driver page is unavailable.'

$token = [guid]::NewGuid().ToString()
$marker = '本地测试' + $token.Substring(0, 8)
$createBody = @{
    submissionToken = $token
    driverName = $marker
    phone = '13800138000'
    licensePlate = '京A12345'
    vehicleType = '厢式货车'
    destination = '本地测试目的地'
    locationStatus = 'NOT_REQUESTED'
} | ConvertTo-Json

$created = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/driver/records" -ContentType 'application/json' -Body $createBody
$repeated = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/driver/records" -ContentType 'application/json' -Body $createBody
Assert-True ($created.data.id -eq $repeated.data.id) 'Idempotent submission test failed.'

$csrf = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/auth/csrf" -WebSession $session
$csrfHeaders = @{}
$csrfHeaders[$csrf.data.headerName] = $csrf.data.token
$loginBody = @{ username = 'admin'; password = $AdminPassword } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/admin/auth/login" -WebSession $session `
    -Headers $csrfHeaders -ContentType 'application/json' -Body $loginBody
Assert-True ($login.data.username -eq 'admin') 'Administrator login test failed.'

$encodedMarker = [System.Uri]::EscapeDataString($marker)
$records = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/records?page=1&pageSize=20&keyword=$encodedMarker" -WebSession $session
Assert-True ($records.data.total -ge 1) 'Administrator record query test failed.'
Assert-True ($null -ne $records.data.serverTime) 'Server time is missing from the record page.'

$updatedBody = @{
    driverName = $marker
    phone = '13800138000'
    licensePlate = '京A12345'
    vehicleType = '新能源厢式货车'
    destination = '修改后的测试目的地'
} | ConvertTo-Json
$csrf = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/auth/csrf" -WebSession $session
$csrfHeaders = @{}
$csrfHeaders[$csrf.data.headerName] = $csrf.data.token
$updated = Invoke-RestMethod -Method Put -Uri "$baseUrl/api/admin/records/$($created.data.id)" -WebSession $session `
    -Headers $csrfHeaders -ContentType 'application/json' -Body $updatedBody
Assert-True ($updated.data.destination -eq '修改后的测试目的地') 'Record update test failed.'

$runtimePath = Join-Path ([System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))) 'runtime'
$exportPath = Join-Path $runtimePath 'local-test-export.xlsx'
Invoke-WebRequest -Method Get -Uri "$baseUrl/api/admin/records/export?keyword=$encodedMarker" `
    -WebSession $session -OutFile $exportPath
Assert-True ((Get-Item -LiteralPath $exportPath).Length -gt 0) 'Excel export test failed.'
$exportBytes = [System.IO.File]::ReadAllBytes($exportPath)
Assert-True ($exportBytes.Length -gt 4 -and $exportBytes[0] -eq 0x50 -and $exportBytes[1] -eq 0x4B) `
    'Excel export is not a valid XLSX zip container.'

$csrf = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/auth/csrf" -WebSession $session
$csrfHeaders = @{}
$csrfHeaders[$csrf.data.headerName] = $csrf.data.token
Invoke-RestMethod -Method Delete -Uri "$baseUrl/api/admin/records/$($created.data.id)" -WebSession $session -Headers $csrfHeaders | Out-Null
$afterDelete = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/records?page=1&pageSize=20&keyword=$encodedMarker" -WebSession $session
Assert-True ($afterDelete.data.total -eq 0) 'Soft-delete visibility test failed.'

$csrf = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/auth/csrf" -WebSession $session
$csrfHeaders = @{}
$csrfHeaders[$csrf.data.headerName] = $csrf.data.token
Invoke-RestMethod -Method Post -Uri "$baseUrl/api/admin/auth/logout" -WebSession $session -Headers $csrfHeaders | Out-Null
try {
    Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/auth/me" -WebSession $session | Out-Null
    throw 'Administrator session remained valid after logout.'
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) {
        throw
    }
}

Write-Output 'LOCAL_TESTS_PASSED'
Write-Output "Created and soft-deleted record ID: $($created.data.id)"
Write-Output "Excel output: $exportPath"
