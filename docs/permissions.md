# Permissions and privacy

The merged release manifest is allowed to contain only these Android system permissions:

| Permission | Why it exists | When used |
|---|---|---|
| `READ_MEDIA_AUDIO` | Optional device-wide discovery of shared audio | Requested only after the user chooses “Find all music on this device” and accepts its explanation |
| `FOREGROUND_SERVICE` | Reliable user-visible background playback | While the media playback service is active |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Identifies the foreground service as local media playback | While playing or maintaining the MediaSession |
| `WAKE_LOCK` | Prevents local playback from stalling when the display sleeps | Managed by Media3 during playback |

AndroidX may generate an application-private `signature` permission for non-exported dynamic receivers. It cannot be granted to ordinary third-party apps and is accepted by the manifest gate.

Music, Please! deliberately does **not** request:

- `INTERNET` or network-state access
- `MANAGE_EXTERNAL_STORAGE` or legacy external-storage access
- Bluetooth/nearby, location, contacts, microphone, camera, or notification permissions

Galaxy Buds controls arrive through Android's MediaSession; the application does not need Bluetooth permission. User-selected music folders and files are retained as read-only Storage Access Framework grants. The separately selected backup folder is retained read/write because backup creation and rotation require it.

Run [`check_release_manifest.ps1`](../scripts/check_release_manifest.ps1) against the merged release manifest to enforce the permission allowlist. Run [`check_packaged_dependencies.ps1`](../scripts/check_packaged_dependencies.ps1) to scan the APK for common network, ads, and telemetry namespaces.
