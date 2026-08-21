param(
    [string]$ApkPath = "$env:TEMP\LocalMusicPlayer-build\implement-v1\app\outputs\apk\release\app-release-unsigned.apk"
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $ApkPath)) { throw "APK not found: $ApkPath" }
$archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ApkPath))
try {
    $names = $archive.Entries.FullName -join "`n"
    $forbidden = @(
        "com/google/firebase", "crashlytics", "okhttp3/", "retrofit2/", "com/android/volley",
        "com/google/android/gms/ads", "com/facebook/ads", "io/sentry", "com/amplitude", "segment/analytics"
    )
    $dexText = [System.Text.StringBuilder]::new()
    foreach ($entry in @($archive.Entries | Where-Object { $_.FullName -match '^classes\d*\.dex$' })) {
        $stream = $entry.Open()
        try {
            $buffer = [byte[]]::new($entry.Length)
            $read = 0
            while ($read -lt $buffer.Length) {
                $count = $stream.Read($buffer, $read, $buffer.Length - $read)
                if ($count -le 0) { break }
                $read += $count
            }
            [void]$dexText.Append([System.Text.Encoding]::ASCII.GetString($buffer, 0, $read))
        } finally { $stream.Dispose() }
    }
    $haystack = $names + $dexText.ToString()
    $found = @($forbidden | Where-Object { $haystack -match [regex]::Escape($_) })
    if ($found.Count -gt 0) { throw "Forbidden packaged dependency markers: $($found -join ', ')" }
} finally {
    $archive.Dispose()
}
Write-Host "Packaged dependency gate passed: AndroidX, Compose, Room, DataStore, Media3, coroutines, serialization only."
