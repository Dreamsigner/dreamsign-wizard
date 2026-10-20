param([string]$Voice = 'Microsoft Hortense Desktop')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$repoRoot = Split-Path -Parent $PSScriptRoot
$course = Get-Content -LiteralPath (Join-Path $repoRoot 'curriculum/courses/tcf_canada_listening_practice_2026.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$resourceRoot = Join-Path $repoRoot 'composeApp/src/commonMain/composeResources'
$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $speaker.SelectVoice($Voice)
    $speaker.Rate = 0
    $speaker.Volume = 100
    $format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(16000, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
    $documents = @{}
    foreach ($lesson in $course.lessons) {
        foreach ($exercise in $lesson.exercises) {
            if ($exercise.audioResourcePath -notmatch '^files/audio/tcf_[a-z]+\.wav$') { throw 'Unexpected resource path' }
            if ($documents.ContainsKey($exercise.audioResourcePath) -and $documents[$exercise.audioResourcePath] -cne $exercise.transcript) { throw 'Conflicting transcripts' }
            $documents[$exercise.audioResourcePath] = $exercise.transcript
        }
    }
    foreach ($entry in $documents.GetEnumerator() | Sort-Object Name) {
        $outputPath = Join-Path $resourceRoot $entry.Key
        [IO.Directory]::CreateDirectory((Split-Path -Parent $outputPath)) | Out-Null
        $speaker.SetOutputToWaveFile($outputPath, $format)
        $speaker.Speak($entry.Value)
        $speaker.SetOutputToNull()
        Get-FileHash -LiteralPath $outputPath -Algorithm SHA256 | Select-Object Path,Hash
    }
} finally { $speaker.Dispose() }
