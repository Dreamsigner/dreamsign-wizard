param([Parameter(Mandatory=$true)][string]$Payload)
$ErrorActionPreference = 'Stop'
$config = Get-Content -LiteralPath $Payload -Raw -Encoding UTF8 | ConvertFrom-Json
try {
    $parent = Get-Process -Id $config.parentPid -ErrorAction SilentlyContinue
    if ($parent) { $parent | Wait-Process -Timeout 120 -ErrorAction Stop }
    $hash = (Get-FileHash -LiteralPath $config.installer -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $config.sha256) { throw 'Installer integrity check failed' }
    $installer = Start-Process -FilePath $config.installer -ArgumentList '/quiet','/norestart' -WindowStyle Hidden -PassThru
    if (-not $installer.WaitForExit(600000)) { throw 'Installer timeout' }
    $code = $installer.ExitCode
    if ($code -notin @(0,3010)) { throw "Installer failed with exit code $code" }
    @{success=$true; exitCode=$code} | ConvertTo-Json | Set-Content -LiteralPath $config.result -Encoding UTF8
    if ($config.restart -and (Test-Path -LiteralPath $config.executable)) {
        Start-Process -FilePath $config.executable -WindowStyle Normal
    }
} catch {
    @{success=$false; error=$_.Exception.Message} | ConvertTo-Json | Set-Content -LiteralPath $config.result -Encoding UTF8
    exit 1
}
