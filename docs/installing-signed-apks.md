# Installing signed APKs

## Install over USB

1. Enable Developer options and USB debugging on the Samsung phone.
2. Connect the phone, accept its RSA prompt, and verify it appears under `adb devices`.
3. Install:

```powershell
adb install LocalMusicPlayer-v0.1.0.apk
```

Use `adb install -r LocalMusicPlayer-v0.1.0.apk` for an update signed by the same key. Android will reject an update signed by a different key; do not uninstall merely to bypass that protection unless the app's local database has already been backed up.

## Install from the phone

Copy the APK to the phone over USB, open it in My Files, and allow “Install unknown apps” only for the file manager Android identifies. Disable that allowance afterward if desired.

## Preserve the signing key

The signing keystore and its password are the permanent identity of this application. Preserve them in at least two secure locations. They are intentionally excluded by [`.gitignore`](../.gitignore) and must never be pushed to GitHub, attached to a release, or copied into the phone's USB-visible backup folder.

The source repository ships an unsigned release build. A maintainer signs it with Android SDK `apksigner`, then verifies it with `apksigner verify --verbose --print-certs` before installation or publication.

