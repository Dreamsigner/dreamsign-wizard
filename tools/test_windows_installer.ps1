param(
    [Parameter(Mandatory)][string]$Installer,
    [Parameter(Mandatory)][string]$UpgradeInstaller
)
$ErrorActionPreference = 'Stop'
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
$appDir = Join-Path $env:LOCALAPPDATA 'DuolingoKmpApp'
$dataDir = Join-Path $env:LOCALAPPDATA 'AtelierApprentissage'
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
function Read-InstalledEntry([string]$Version) {
    $entries = @(Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -eq 'DuolingoKmpApp' })
    if ($entries.Count -ne 1 -or $entries[0].DisplayVersion -ne $Version -or $entries[0].WindowsInstaller -ne 1) {
        throw 'Expected exactly one per-user Windows Installer registration at the requested version.'
    }
    return $entries[0]
}
function Assert-InstalledApp([string]$Version, [string]$Phase) {
    $entry = Read-InstalledEntry $Version
    $launcher = Join-Path $appDir 'DuolingoKmpApp.exe'
    if (!(Test-Path -LiteralPath $launcher)) { throw 'Installed application launcher is missing.' }
    foreach ($notice in @('THIRD-PARTY-NOTICES.txt', 'LISEZ-MOI.md')) {
        if (!(Get-ChildItem -LiteralPath $appDir -Recurse -File -Filter $notice)) { throw "Missing installed document: $notice" }
    }
    $startMenu = [Environment]::GetFolderPath('Programs')
    $desktop = [Environment]::GetFolderPath('DesktopDirectory')
    if (!(Get-ChildItem -LiteralPath $startMenu -Filter 'DuolingoKmpApp.lnk' -File -Recurse)) { throw 'Start menu shortcut is missing.' }
    if (!(Test-Path -LiteralPath (Join-Path $desktop 'DuolingoKmpApp.lnk'))) { throw 'Desktop shortcut is missing.' }
    $diagnostic = Join-Path $reportDir "$Phase-diagnostic.json"
    Invoke-CheckedProcess $launcher @('--self-check', '--report', ('"{0}"' -f $diagnostic))
    $checks = Get-Content -Raw -LiteralPath $diagnostic | ConvertFrom-Json
    if ($checks.mathRenderingVerified -ne $true -or $checks.pdfRenderingVerified -ne $true -or
        $checks.uiStarted -ne $false -or $checks.userDataAccessed -ne $false) { throw 'Installed app diagnostic failed.' }
    Assert-Progress
    return $entry
}
Invoke-CheckedProcess (Resolve-Path -LiteralPath $Installer).Path @('/quiet', '/norestart', '/L*v', ('"{0}"' -f (Join-Path $reportDir 'install.log')))
$first = Assert-InstalledApp '1.1.0' 'installed'
Invoke-CheckedProcess (Resolve-Path -LiteralPath $UpgradeInstaller).Path @('/quiet', '/norestart', '/L*v', ('"{0}"' -f (Join-Path $reportDir 'upgrade.log')))
$updated = Assert-InstalledApp '1.1.1' 'upgraded'
if ($first.PSChildName -eq $updated.PSChildName) { throw 'The upgrade did not replace the MSI product registration.' }
if ($updated.PSChildName -notmatch '^\{[0-9A-Fa-f-]{36}\}$') { throw 'Invalid uninstall product code.' }
Invoke-CheckedProcess 'msiexec.exe' @('/x', $updated.PSChildName, '/quiet', '/norestart', '/L*v', ('"{0}"' -f (Join-Path $reportDir 'uninstall.log')))
Assert-Progress
if (Test-Path -LiteralPath (Join-Path $appDir 'DuolingoKmpApp.exe')) { throw 'The app launcher remains after uninstall.' }
if (Test-Path -LiteralPath $updated.PSPath) { throw 'Uninstall registration remains after uninstall.' }
[ordered]@{
    installedVersion = '1.1.0'; upgradedVersion = '1.1.1'; installedAppDiagnostic = $true
    perUserRegistration = $true; shortcutsVerified = $true; noticesVerified = $true
    upgradeVerified = $true; uninstallVerified = $true; progressPreserved = $true
    sourceCommit = $env:GITHUB_SHA
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $reportDir 'summary.json') -Encoding utf8
