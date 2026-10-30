param(
    [Parameter(Mandatory)][string]$Installer,
    [Parameter(Mandatory)][string]$UpgradeInstaller,
    [Parameter(Mandatory)][string]$ExpectedVersion,
    [Parameter(Mandatory)][string]$ExpectedUpgradeVersion
)
$ErrorActionPreference = 'Stop'
foreach ($version in @($ExpectedVersion, $ExpectedUpgradeVersion)) {
    if ($version -notmatch '^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$') {
        throw "Expected a numeric three-part package version, got: $version"
    }
}
if ([version]$ExpectedUpgradeVersion -le [version]$ExpectedVersion) {
    throw 'The upgrade package version must be greater than the installed package version.'
}
# This script installs/uninstalls software. Run exclusively on disposable GitHub Windows VMs.
if ($env:GITHUB_ACTIONS -ne 'true' -or $env:RUNNER_OS -ne 'Windows' -or !$env:GITHUB_RUN_ID) {
    throw 'Installer smoke tests are restricted to disposable GitHub Windows runners.'
}
$workspace = (Resolve-Path -LiteralPath $env:GITHUB_WORKSPACE).Path
foreach ($candidate in @($Installer, $UpgradeInstaller)) {
    $resolved = (Resolve-Path -LiteralPath $candidate).Path
    if (!$resolved.StartsWith((Join-Path $workspace 'composeApp\build\'), [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Expected an installer built inside this workspace.'
    }
}
$reportDir = Join-Path $workspace 'composeApp/build/reports/installer'
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
$appDir = Join-Path $env:LOCALAPPDATA 'DreamsignWizardApp'
$dataDir = Join-Path $env:LOCALAPPDATA 'DreamsignWizard'
$progress = Join-Path $dataDir 'progress.json'
if ((Test-Path -LiteralPath $appDir) -or (Test-Path -LiteralPath $dataDir)) {
    throw 'The runner is not clean; existing application or learner data must not be overwritten.'
}
New-Item -ItemType Directory -Path $dataDir | Out-Null
[IO.File]::WriteAllText($progress, '{"xp":37,"hearts":3,"maxHearts":5,"streakDays":1,"completedLessonIds":["mit_18_01sc_2010_s001"],"activeSubjectId":"math_vectors","selectedCourseIds":{"math_vectors":"mit_18_01sc_2010"}}')
$originalHash = (Get-FileHash -LiteralPath $progress -Algorithm SHA256).Hash
function Assert-Progress {
    if (!(Test-Path -LiteralPath $progress) -or (Get-FileHash -LiteralPath $progress -Algorithm SHA256).Hash -ne $originalHash) {
        throw 'Learner progress changed during an installer operation.'
    }
}
function Invoke-CheckedProcess([string]$File, [string[]]$Arguments) {
    $process = Start-Process -FilePath $File -ArgumentList $Arguments -PassThru -WindowStyle Hidden
    if (!$process.WaitForExit(180000)) { $process.Kill(); throw 'Installer check timed out.' }
    if ($process.ExitCode -ne 0) { throw "Process failed with code $($process.ExitCode): $File" }
}
# Ask Windows Installer directly rather than assuming an ARP registry view.
# The jpackage bootstrapper embeds its MSI ProductCode as a UTF-8 resource.
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class SetupIdentity {
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    static extern IntPtr LoadLibraryEx(string path, IntPtr file, uint flags);
    [DllImport("kernel32.dll", CharSet=CharSet.Unicode, SetLastError=true)]
    static extern IntPtr FindResource(IntPtr module, string name, IntPtr type);
    [DllImport("kernel32.dll")] static extern uint SizeofResource(IntPtr module, IntPtr resource);
    [DllImport("kernel32.dll")] static extern IntPtr LoadResource(IntPtr module, IntPtr resource);
    [DllImport("kernel32.dll")] static extern IntPtr LockResource(IntPtr resource);
    [DllImport("kernel32.dll")] static extern bool FreeLibrary(IntPtr module);
    public static string ProductCode(string path) {
        var module = LoadLibraryEx(path, IntPtr.Zero, 2); // Data only: never execute setup.
        if (module == IntPtr.Zero) throw new Exception("Cannot read installer resources.");
        try {
            var resource = FindResource(module, "product_code", new IntPtr(10));
            uint size = SizeofResource(module, resource);
            if (resource == IntPtr.Zero || size == 0 || size > 128) throw new Exception("Missing installer identity.");
            var bytes = new byte[size];
            Marshal.Copy(LockResource(LoadResource(module, resource)), bytes, 0, bytes.Length);
            return Encoding.UTF8.GetString(bytes).TrimEnd('\0');
        } finally { FreeLibrary(module); }
    }
}
"@
$msiEngine = New-Object -ComObject WindowsInstaller.Installer
function Read-InstalledEntry([string]$Version, [string]$SetupPath) {
    $code = [SetupIdentity]::ProductCode((Resolve-Path -LiteralPath $SetupPath).Path)
    if ($code -notmatch '^\{[0-9A-Fa-f-]{36}\}$') { throw 'Invalid installer identity.' }
    $installedVersion = $msiEngine.ProductInfo($code, 'VersionString')
    $assignment = $msiEngine.ProductInfo($code, 'AssignmentType')
    if ($installedVersion -ne $Version -or $assignment -ne '0' -or $msiEngine.ProductState($code) -ne 5) {
        throw 'Expected an installed per-user MSI product at the requested version.'
    }
    return [pscustomobject]@{ ProductCode = $code; Version = $installedVersion }
}
function Assert-InstalledApp([string]$Version, [string]$Phase, [string]$SetupPath) {
    $entry = Read-InstalledEntry $Version $SetupPath
    $launcher = Join-Path $appDir 'DreamsignWizard.exe'
    if (!(Test-Path -LiteralPath $launcher)) { throw 'Installed application launcher is missing.' }
    foreach ($notice in @('THIRD-PARTY-NOTICES.txt', 'LISEZ-MOI.md')) {
        if (!(Get-ChildItem -LiteralPath $appDir -Recurse -File -Filter $notice)) { throw "Missing installed document: $notice" }
    }
    $startMenu = [Environment]::GetFolderPath('Programs')
    $desktop = [Environment]::GetFolderPath('DesktopDirectory')
    if (!(Get-ChildItem -LiteralPath $startMenu -Filter 'DreamsignWizard.lnk' -File -Recurse)) { throw 'Start menu shortcut is missing.' }
    if (!(Test-Path -LiteralPath (Join-Path $desktop 'DreamsignWizard.lnk'))) { throw 'Desktop shortcut is missing.' }
    $diagnostic = Join-Path $reportDir "$Phase-diagnostic.json"
    Invoke-CheckedProcess $launcher @('--self-check', '--report', ('"{0}"' -f $diagnostic))
    $checks = Get-Content -Raw -LiteralPath $diagnostic | ConvertFrom-Json
    if ($checks.mathRenderingVerified -ne $true -or $checks.pdfRenderingVerified -ne $true -or
        $checks.uiStarted -ne $false -or $checks.userDataAccessed -ne $false) { throw 'Installed app diagnostic failed.' }
    if ($checks.updaterInstalledEdition -ne $true -or $checks.updaterVersion -ne $Version) { throw 'Installed updater edition/version mismatch.' }
    Assert-Progress
    return $entry
}
Invoke-CheckedProcess (Resolve-Path -LiteralPath $Installer).Path @('/quiet', '/norestart', '/L*v', ('"{0}"' -f (Join-Path $reportDir 'install.log')))
$first = Assert-InstalledApp $ExpectedVersion 'installed' $Installer
# Exercise the production native updater launcher and its Windows PowerShell helper.
$launcher = Join-Path $appDir 'DreamsignWizard.exe'
$result = Join-Path $reportDir 'last-result.json'
$upgradeSource = (Resolve-Path -LiteralPath $UpgradeInstaller).Path
$upgradeCacheCopy = Join-Path $reportDir "DreamsignWizard-$ExpectedUpgradeVersion-Setup.exe"
Copy-Item -LiteralPath $upgradeSource -Destination $upgradeCacheCopy
$upgradeSourceHash = (Get-FileHash -LiteralPath $upgradeSource -Algorithm SHA256).Hash
Invoke-CheckedProcess $launcher @('--self-check-updater', '--installer', ('"{0}"' -f $upgradeCacheCopy), '--report-dir', ('"{0}"' -f $reportDir))
$deadline = (Get-Date).AddMinutes(3)
while (!(Test-Path -LiteralPath $result) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 500 }
if (!(Test-Path -LiteralPath $result)) { throw 'Updater did not produce a result.' }
$updateResult = Get-Content -LiteralPath $result -Raw | ConvertFrom-Json
if ($updateResult.success -ne $true) { throw 'Automatic update helper failed.' }
$cleanupDeadline = (Get-Date).AddSeconds(20)
do {
    $payloads = @(Get-ChildItem -LiteralPath $reportDir -Filter 'apply-*.json' -File -ErrorAction SilentlyContinue)
    if (!(Test-Path -LiteralPath $upgradeCacheCopy) -and $payloads.Count -eq 0) { break }
    Start-Sleep -Milliseconds 250
} while ((Get-Date) -lt $cleanupDeadline)
if (Test-Path -LiteralPath $upgradeCacheCopy) { throw 'The updater did not remove its cached installer after success.' }
if (Get-ChildItem -LiteralPath $reportDir -Filter 'apply-*.json' -File -ErrorAction SilentlyContinue) { throw 'The updater left its payload file behind.' }
if (!(Test-Path -LiteralPath $upgradeSource) -or (Get-FileHash -LiteralPath $upgradeSource -Algorithm SHA256).Hash -ne $upgradeSourceHash) {
    throw 'The updater changed or removed the external upgrade fixture.'
}
$updated = Assert-InstalledApp $ExpectedUpgradeVersion 'upgraded' $UpgradeInstaller
if ($first.ProductCode -eq $updated.ProductCode -or $msiEngine.ProductState($first.ProductCode) -ne -1) { throw 'The upgrade did not replace the old MSI product registration.' }
if ($updated.ProductCode -notmatch '^\{[0-9A-Fa-f-]{36}\}$') { throw 'Invalid uninstall product code.' }
Invoke-CheckedProcess 'msiexec.exe' @('/x', $updated.ProductCode, '/quiet', '/norestart', '/L*v', ('"{0}"' -f (Join-Path $reportDir 'uninstall.log')))
Assert-Progress
if (Test-Path -LiteralPath (Join-Path $appDir 'DreamsignWizard.exe')) { throw 'The app launcher remains after uninstall.' }
if ($msiEngine.ProductState($updated.ProductCode) -ne -1 -or $msiEngine.ProductState($first.ProductCode) -ne -1) { throw 'An MSI product remains registered after uninstall.' }
[ordered]@{
    installedVersion = $ExpectedVersion; upgradedVersion = $ExpectedUpgradeVersion; installedAppDiagnostic = $true
    perUserRegistration = $true; shortcutsVerified = $true; noticesVerified = $true
    updateHelperVerified = $true; upgradeVerified = $true; uninstallVerified = $true; progressPreserved = $true
    updaterCacheCleanupVerified = $true; externalInstallerPreserved = $true
    sourceCommit = $env:GITHUB_SHA
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $reportDir 'summary.json') -Encoding utf8
