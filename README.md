# Music, Please!

**A private, offline Android music player for your own MP3 collection.**

Music, Please! is a private, offline Android MP3 player for large personal libraries. It has no internet permission, ads, telemetry, accounts, cloud service, microphone access, location access, or all-files access.

## What works

- Add one or more user-selected folders, or optionally all device audio.
- Scan metadata into a Room/FTS catalog designed and tested with 50,000 tracks.
- Search title, artist, album, genre, filename, or playlist within a remembered Library view for Tracks, Artists, Albums, Genres, and Playlists.
- Play in the background through Android MediaSession/Media3 with lock-screen, Samsung Galaxy Buds, audio-focus, noisy-route, and wake-lock support.
- Direct Previous, Play/Pause, Next, Queue, Shuffle, and Repeat controls arranged in two clear rows.
- Unbiased Fisher–Yates shuffle backed by `SecureRandom`; explicit Repeat Off, Repeat All, and Repeat One behavior.
- Ordered playlists with duplicates, light/dark/system appearance, and reduced motion. Favorites data remains backup-compatible but is intentionally absent from the current interface.
- Library-centered source and scan tools: the first source starts a dedicated scan immediately, later sources scan quietly, and dedicated mode exits automatically when scanning finishes.
- A contextual Home screen shows Now Playing during playback and bounded, fully local recent-track and recent-playlist history otherwise.
- Every available track has one consistent menu for Play now, Play next, Add to queue, Add to playlist, Go to artist, Track information, and reversible removal from the Library index.
- Removed tracks stay ignored across scans until restored from Library tools; their MP3 files are never deleted or modified.
- User-selected, USB-visible ZIP backups with daily automatic rotation, manual backups, validation, safety backups before restore, and conservative cross-phone relinking.

## Android and installation

The minimum is Android 13 (API 33); the project targets API 37 and is tested on a Samsung Galaxy S24 Ultra running Android 16. See [installing signed APKs](docs/installing-signed-apks.md).

## Privacy and access

Selected folders use Android's Storage Access Framework and do not require broad audio permission. “Find all music on this device” is a separate, explained action that requests `READ_MEDIA_AUDIO`; denying it does not affect selected folders. See the exact [permission audit](docs/permissions.md).

The application does not request `INTERNET` or `MANAGE_EXTERNAL_STORAGE`. A build-time script rejects unexpected permissions, and another scans packaged DEX files for common networking, advertising, and telemetry libraries.

## Shuffle, repeat, and Previous

- Shuffle uses a fresh, true uniform permutation. It does not weight, cluster, or secretly suppress recently played artists.
- Turning shuffle on keeps the current track and uniformly shuffles the remaining tracks.
- Previous restarts the current track after three seconds; before three seconds it returns through actual history when available.
- Manual Next advances even under Repeat One. Natural track completion repeats under Repeat One.
- Repeat All starts a fresh shuffle permutation at the cycle boundary when shuffle is enabled.

The pure queue state machine and property tests are in [`playback/queue`](app/src/main/java/com/javelinco/localmusicplayer/playback/queue).

## Building

Install Android Studio with SDK 37, then run on Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
.\scripts\check_release_manifest.ps1
.\scripts\check_packaged_dependencies.ps1
```

The release APK produced by Gradle is unsigned until it is signed with a private key. Never commit the key or its password.

## Documentation

- [Permissions and privacy](docs/permissions.md)
- [Backup format V1](docs/backup-format-v1.md)
- [Installing and updating signed APKs](docs/installing-signed-apks.md)
- [Samsung acceptance checklist](docs/testing/samsung-acceptance-checklist.md)
- [Approved design specification](docs/superpowers/specs/2026-08-20-private-android-music-player-design.md)
- [Changelog](CHANGELOG.md)

## V1 limits

V1 indexes MP3 only. It reads metadata and existing tags but does not calculate loudness for files without normalization data. A low-priority loudness-analysis pass remains a possible V2 feature. Pending physical acceptance rows requiring a personal library, Galaxy Buds, or a second phone are explicitly marked in the Samsung checklist.
