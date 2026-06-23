# Exports the signed release AAB and Play release notes to Desktop\Build.
# Version/versionCode are parsed from build.gradle.kts so they never drift.
# Includes the mandatory 500-char-per-locale enforcement (play-store-release-notes):
# over-limit notes are silently truncated by Play Console, so we fail fast instead.
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleText = Get-Content (Join-Path $repoRoot 'build.gradle.kts') -Raw
$version = [regex]::Match($gradleText, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$versionCode = [regex]::Match($gradleText, 'versionCode\s*=\s*(\d+)').Groups[1].Value
if (-not $version -or -not $versionCode) { throw 'Could not parse versionName/versionCode from build.gradle.kts' }
$project = 'RxScan'

$aab = Join-Path $repoRoot 'apps/android/app/build/outputs/bundle/release/app-release.aab'
if (-not (Test-Path $aab)) { throw "Release AAB not found: $aab (run: ./gradlew :app:bundleRelease)" }

$notes = Join-Path $repoRoot "play_store/release_notes/v$version-vc$versionCode.txt"
if (-not (Test-Path $notes)) { throw "Release notes not found: $notes" }
$notesContent = Get-Content $notes -Raw -Encoding UTF8

# Play Console hard limit: 500 Unicode chars per locale block (excluding tags).
$localePattern = '<(ko-KR|en-US|ja-JP|zh-CN|zh-TW)>([\s\S]*?)</\1>'
$violations = @()
foreach ($match in [regex]::Matches($notesContent, $localePattern)) {
    $locale = $match.Groups[1].Value
    $body = $match.Groups[2].Value.Trim()
    $len = $body.Length
    $status = if ($len -gt 500) { 'OVER' } else { 'OK' }
    Write-Host ("  {0,-7}  {1,4} / 500  {2}" -f $locale, $len, $status)
    if ($len -gt 500) { $violations += "$locale ($len chars, $($len - 500) over)" }
}
if ($violations.Count -gt 0) {
    throw "Play Console release notes exceed the 500-character limit per locale: " +
        ($violations -join ', ') + ". Trim before exporting."
}

$desktop = [Environment]::GetFolderPath('Desktop')
$buildDir = Join-Path $desktop 'Build'
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null

$aabOut = Join-Path $buildDir "$project-v$version-vc$versionCode.aab"
$notesOut = Join-Path $buildDir "$project-v$version-vc$versionCode-release-notes.txt"
Copy-Item $aab $aabOut -Force
Copy-Item $notes $notesOut -Force

Write-Host ''
Write-Host "Exported $project v$version (vc $versionCode):"
Write-Host "  $aabOut"
Write-Host "  $notesOut"
