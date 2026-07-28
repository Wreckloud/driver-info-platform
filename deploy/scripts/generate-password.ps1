param()

$securePassword = Read-Host 'New administrator password (at least 12 characters)' -AsSecureString
$credential = [System.Net.NetworkCredential]::new('', $securePassword)
$plainPassword = $credential.Password
if ($plainPassword.Length -lt 12) {
    throw 'Password must contain at least 12 characters.'
}

try {
    $env:ADMIN_PASSWORD_PLAIN = $plainPassword
    $backendPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\backend'))
    Push-Location $backendPath
    try {
        mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.5.1:java '-Dexec.mainClass=com.wreckloud.driver.tool.PasswordHashTool'
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:ADMIN_PASSWORD_PLAIN -ErrorAction SilentlyContinue
    $plainPassword = $null
    $credential = $null
}
