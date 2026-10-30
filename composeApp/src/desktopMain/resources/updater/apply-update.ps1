param([Parameter(Mandatory=$true)][string]$Payload)
$ErrorActionPreference = 'Stop'
$installSucceeded = $false
$helperExitCode = 0
$config = $null
try {
    $config = Get-Content -LiteralPath $Payload -Raw -Encoding UTF8 | ConvertFrom-Json
    $parent = Get-Process -Id $config.parentPid -ErrorAction SilentlyContinue
    if ($parent) { $parent | Wait-Process -Timeout 120 -ErrorAction Stop }
    # Use the framework API: Windows PowerShell may inherit a PowerShell 7 module path.
    $stream = [System.IO.File]::OpenRead($config.installer)
    try {
        $hasher = [System.Security.Cryptography.SHA256]::Create()
        try { $hash = ([BitConverter]::ToString($hasher.ComputeHash($stream))).Replace('-','').ToLowerInvariant() }
        finally { $hasher.Dispose() }
    } finally { $stream.Dispose() }
    if ($hash -ne $config.sha256) { throw 'Installer integrity check failed' }
    $installer = Start-Process -FilePath $config.installer -ArgumentList '/quiet','/norestart' -WindowStyle Hidden -PassThru
    if (-not $installer.WaitForExit(600000)) { throw 'Installer timeout' }
    $code = $installer.ExitCode
    if ($code -notin @(0,3010)) { throw "Installer failed with exit code $code" }
    $installSucceeded = $true
    @{success=$true; exitCode=$code} | ConvertTo-Json | Set-Content -LiteralPath $config.result -Encoding UTF8
    if ($config.restart -and (Test-Path -LiteralPath $config.executable)) {
        Start-Process -FilePath $config.executable -WindowStyle Normal
    }
} catch {
    if ($config -and $config.result) {
        try { @{success=$false; error=$_.Exception.Message} | ConvertTo-Json | Set-Content -LiteralPath $config.result -Encoding UTF8 } catch { }
    }
    $helperExitCode = 1
} finally {
    $payloadPath = $null
    $payloadDirectory = $null
    $payloadIsOwned = $false
    try {
        $payloadPath = [IO.Path]::GetFullPath($Payload)
        $payloadDirectory = [IO.Path]::GetDirectoryName($payloadPath)
        $payloadName = [IO.Path]::GetFileName($payloadPath)
        $payloadIsOwned = $payloadName -match '^apply-[A-Za-z0-9._-]+\.json$'
        if ($payloadIsOwned) {
            $payloadParent = Get-Item -LiteralPath $payloadDirectory -Force -ErrorAction Stop
            $payloadIsOwned = $payloadParent -is [IO.DirectoryInfo] -and -not ($payloadParent.Attributes -band [IO.FileAttributes]::ReparsePoint)
        }
    } catch { $payloadIsOwned = $false }

    if ($installSucceeded -and $config -and $config.installer -and $payloadIsOwned) {
        try {
            $installerPath = [IO.Path]::GetFullPath([string]$config.installer)
            $installerParent = [IO.Path]::GetDirectoryName($installerPath)
            $strictName = '^DreamsignWizard-(0|[1-9]\d{0,3})\.(0|[1-9]\d{0,3})\.(0|[1-9]\d{0,3})-Setup\.exe$'
            if ([string]::Equals($installerParent, $payloadDirectory, [StringComparison]::OrdinalIgnoreCase) -and
                [IO.Path]::GetFileName($installerPath) -match $strictName) {
                $candidate = Get-Item -LiteralPath $installerPath -Force -ErrorAction Stop
                if ($candidate -is [IO.FileInfo] -and -not ($candidate.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
                    Remove-Item -LiteralPath $installerPath -Force -ErrorAction Stop
                }
            }
        } catch { }
    }
    if ($payloadIsOwned) {
        try {
            $payloadItem = Get-Item -LiteralPath $payloadPath -Force -ErrorAction Stop
            if ($payloadItem -is [IO.FileInfo] -and -not ($payloadItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
                Remove-Item -LiteralPath $payloadPath -Force -ErrorAction Stop
            }
        }
        catch { }
    }
}
if ($helperExitCode -ne 0) { exit $helperExitCode }
