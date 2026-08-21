param([string]$ManifestPath)

$ErrorActionPreference = "Stop"

$approvedPermissions = @(
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
    "android.permission.WAKE_LOCK"
)

$workspaceRoot = (Get-Location).Path
if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    $workspaceName = Split-Path -Leaf $workspaceRoot
    $projectManifest = Join-Path $workspaceRoot "app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml"
    $externalManifest = Join-Path $env:TEMP "LocalMusicPlayer-build/$workspaceName/app/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml"
    $resolvedManifest = if (Test-Path -LiteralPath $externalManifest -PathType Leaf) {
        $externalManifest
    } else {
        $projectManifest
    }
} else {
    $resolvedManifest = Join-Path $workspaceRoot $ManifestPath
}

if (-not (Test-Path -LiteralPath $resolvedManifest -PathType Leaf)) {
    throw "Merged manifest not found: $resolvedManifest"
}

[xml]$manifest = Get-Content -Raw -LiteralPath $resolvedManifest
$actualPermissions = @(
    $manifest.manifest.'uses-permission' |
        ForEach-Object { $_.GetAttribute("name", "http://schemas.android.com/apk/res/android") }
)

$privatePermissions = @(
    $manifest.manifest.permission |
        Where-Object {
            $_.GetAttribute("protectionLevel", "http://schemas.android.com/apk/res/android") -eq "signature"
        } |
        ForEach-Object { $_.GetAttribute("name", "http://schemas.android.com/apk/res/android") }
)

$unexpected = @(
    $actualPermissions |
        Where-Object { $_ -notin $approvedPermissions -and $_ -notin $privatePermissions }
)
if ($unexpected.Count -ne 0) {
    throw "Unexpected permissions: $($unexpected -join ', ')"
}

$missing = @($approvedPermissions | Where-Object { $_ -notin $actualPermissions })
if ($missing.Count -ne 0) {
    throw "Missing approved permissions: $($missing -join ', ')"
}

Write-Output "Manifest permission contract passed: $($actualPermissions -join ', ')"
