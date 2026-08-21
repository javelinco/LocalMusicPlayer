# Private Android Music Player — Design Specification

**Status:** Approved design

**Date:** 2026-08-20

**Initial audience:** Two users with recent Samsung Android devices

**Repository visibility:** Public

**Working project name:** LocalMusicPlayer

## 1. Product intent

LocalMusicPlayer is a deliberately small, offline Android music player for large personal MP3 libraries. It prioritizes least-privilege-by-default file access, predictable playback controls, correct shuffle and repeat behavior, fast local search, non-disruptive scanning, and portable local backups.

The first release will be distributed as a signed APK and will target Android 13 or newer. It is intended initially for two known Samsung devices purchased within the last two years. The signing key must remain stable so APK updates preserve application data.

## 2. Goals

- Play MP3 files from explicitly selected folders, explicitly selected individual files, or an optional user-authorized scan of Android's shared media library.
- Continue playback while the app is backgrounded or the screen is locked.
- Integrate with Android lock-screen controls, media notifications, audio focus, and standard Samsung earbud media commands.
- Make internet access technically unavailable by omitting the Android `INTERNET` permission.
- Avoid all-files, Bluetooth, location, contacts, microphone, advertising, analytics, and telemetry access. Shared media-library access is optional and requested only for the user-selected whole-device discovery mode.
- Remain responsive with at least 50,000 indexed tracks.
- Keep scanning from interfering with playback, search, queue editing, playlist editing, or normal interface use.
- Provide a user-entered dedicated scanning mode for maximum first-scan throughput.
- Preserve queues, playlists, favorites, shuffle state, and missing-file references across restarts and device migration.
- Make backup files easy to locate and copy over USB.
- Keep essential playback controls obvious and directly accessible.

## 3. V1 scope

### 3.1 Supported content

- MP3 audio only.
- Three explicit source modes: Choose Folders, Choose Individual Files, and Find All Music on This Device.
- Metadata-driven library views: Artists, Albums, Tracks, Genres, and Playlists.
- Search over title, track artist, album artist, album, genre, and filename fallback.
- Embedded artwork only. Artwork is decoded and cached locally.
- Existing ReplayGain tags are honored when supported by the file's metadata.

Files of other audio types are ignored and included in a visible skipped-file count. Missing metadata is represented by explicit `Unknown` categories.

### 3.2 Playback features

- Play, pause, seek, previous, and next.
- Persistent queue with manual reordering and removal.
- Play Now, Play Next, and Add to Queue actions for tracks and metadata collections.
- Uniform shuffle as specified in section 9.
- Repeat Off, Repeat All, and Repeat One as specified in section 10.
- Favorites and manually created playlists.
- Gapless transitions where the MP3 encoding and Android decoder permit them.
- Session restoration after process death or phone restart without automatic audio start.
- Light, dark, and follow-system themes.

### 3.3 Explicitly deferred to V2

- Calculating loudness normalization for files without ReplayGain metadata. V2 should provide a resumable low-priority analysis pass and may also provide an exclusive dedicated analysis mode.
- A true audio-reactive level visualization derived from decoded audio.

### 3.4 Non-goals for V1

- Streaming, accounts, cloud synchronization, online metadata, lyrics retrieval, recommendations, social features, advertisements, analytics, or in-app update checks.
- Folder-based library browsing.
- Tag editing, deleting music, moving music, or reorganizing source folders.
- Arbitrary all-files access or reading another application's private storage.
- Equalization, crossfade, playback-speed control, or calculated loudness analysis.
- Public app-store release or broad device compatibility certification.
- iOS, desktop, web, or cross-platform framework support.

## 4. Technology and component boundaries

The application will be native Android software written in Kotlin. It will use Jetpack Compose for the interface, Media3 for playback and media-session integration, Room/SQLite for local structured data and full-text search, and Android's Storage Access Framework (SAF) for user-scoped file access.

Components must communicate through explicit interfaces so scanning, playback, queue logic, and user-owned data can be developed and tested independently.

### 4.1 Compose UI

Displays immutable UI state and sends user commands. It does not directly mutate the database, queue, or player internals.

### 4.2 Library repository

Serves indexed artists, albums, tracks, genres, and search results from Room. It publishes updated snapshots as scan transactions complete.

### 4.3 Source registry

Represents the three discovery mechanisms behind one read-only interface: persisted SAF folder trees, persisted SAF document selections, and the optional Android MediaStore audio collection. It exposes stable logical source descriptors without leaking provider-specific access details into playlists, queues, or backups.

### 4.4 Scan coordinator

Traverses selected SAF trees, extracts MP3 metadata, builds or updates the index, records scan problems, and checkpoints progress. It supports background and dedicated modes with different resource priorities.

### 4.5 Playback service

Owns Media3 playback, the MediaSession, audio focus, media notification, lock-screen integration, audio routing, and headset commands. Playback remains independent of UI process visibility.

### 4.6 Queue engine

Owns source ordering, current-cycle ordering, current item, actual listening history, unplayed items, shuffle state, repeat state, and manual queue edits. It exposes commands rather than mutable collections.

### 4.7 User-data repository

Owns playlists, playlist ordering, favorites, settings, selected-source descriptors, and persisted playback-session state. Scanner output cannot delete or rewrite these records.

### 4.8 Backup manager

Exports and restores portable, versioned user data. It writes only to the separately selected backup folder and does not include disposable indexes, cached artwork, or audio files.

## 5. Privacy and permission contract

### 5.1 Music folders

The app uses `ACTION_OPEN_DOCUMENT_TREE` and retains read permission only for folder trees explicitly selected by the user. It must not request write permission for music roots.

### 5.2 Individual music files

`Choose Individual Files` uses `ACTION_OPEN_DOCUMENT` with multiple selection enabled and an audio MIME filter. The app retains read permission only for the documents selected by the user. Selecting more files later adds to the registered source set without broadening access to their containing folders.

### 5.3 Optional shared media-library discovery

`Find All Music on This Device` is a separate, clearly labeled action. On Android 13 and newer, choosing it requests the runtime `READ_MEDIA_AUDIO` permission and then queries Android MediaStore for shared audio content. The permission is not requested during first launch, folder selection, individual-file selection, ordinary playback, or backup setup.

The interface must explain before the system prompt that this option can read all audio exposed through Android's shared media library. Denial or later revocation leaves folder and individual-file sources working normally. This mode does not grant arbitrary filesystem access and cannot inspect another application's private storage.

The app must never declare legacy broad external-storage permissions or `MANAGE_EXTERNAL_STORAGE`.

### 5.4 Backup folder

The user selects a separate, USB-visible shared folder such as `Documents/LocalMusicPlayer Backups`. The app retains read/write SAF permission only for this selected backup tree. No music-root write permission is implied.

### 5.5 Network and sensitive capabilities

The manifest must omit `INTERNET`. The build must not include network clients, telemetry SDKs, advertising SDKs, online crash reporting, or dependencies that require network behavior.

The app must not request location, contacts, microphone, nearby-device, or direct Bluetooth permissions. Earbud commands arrive through the Android MediaSession.

The manifest may declare only the foreground-service capabilities required for media playback. Dedicated scanning keeps the display on with an activity window flag while that screen is active; it does not require a wake-lock permission. Long-running low-priority scans stop and checkpoint when the app is dismissed, so they do not require an independent background scanning service.

### 5.6 Build verification

An automated check must fail the build if the merged release manifest contains `INTERNET`, all-files access, legacy storage access, or any permission outside the approved allowlist. `READ_MEDIA_AUDIO` is allowlisted solely for the optional whole-device discovery feature and must have an accompanying runtime-gating test.

## 6. Library identity and metadata rules

- Album identity is normalized album artist plus album title.
- If album artist is absent, track artist is the fallback.
- Compilation tags are honored; a `Various Artists` album artist keeps compilation tracks together.
- Album playback order is disc number, then track number, then a deterministic title/filename fallback.
- Every registered folder, individual document, or MediaStore collection has a stable application-generated source ID independent of Android's device-specific content URI or media ID.
- A track record retains its source ID, relative path or display-path hint when available, file size, modification information when available, duration, and normalized metadata.
- Device-specific SAF URIs are runtime access details, not the sole durable identity used in backups.
- Missing or inaccessible tracks remain indexed as unavailable until access returns or the user explicitly removes the source or reference.

Search uses a Room full-text index. It covers normalized metadata plus filename fallback, updates incrementally, and never leaves the device.

## 7. Scanning

### 7.1 General guarantees

- Opening the app loads the last valid library index immediately.
- Scans update the library in small transactions and publish incremental snapshots.
- Scanner writes may add, update, or mark catalog records unavailable.
- Scanner writes must never alter the queue, queue history, shuffle permutation, playback state, playlists, favorites, or user settings.
- Scan work is resumable and cancellation-safe.
- Corrupt, unreadable, or unsupported files are recorded in a scan report and do not stop the scan.
- The scan interface reports phase, discovered files, processed files, skipped files, errors, elapsed time, and completion state. When total work cannot yet be known, progress is explicitly indeterminate rather than misleading.

### 7.2 Background scan

- Starts automatically when the app opens by default; the setting can be disabled.
- Can also be started manually with `Scan for changes`.
- Runs at low priority and yields to playback, search, UI work, queue edits, and playlist edits.
- Is cancelable.
- Checkpoints and stops when the app is dismissed. It does not continue as a hidden or separately notified long-running job.
- Must not produce audible playback interruptions or persistent UI jank on the target devices.

### 7.3 Dedicated scanning mode

- Is optional and is offered after initial folder selection when no indexed music exists.
- Can be entered later by an explicit user action.
- Stops playback before high-priority work begins.
- Temporarily prevents playback and playlist or queue editing while active.
- Replaces the ordinary application UI with a full-screen progress interface.
- Keeps the screen on only while the dedicated scanning screen remains active.
- Continues until the scan completes or the user explicitly exits.
- Exiting checkpoints progress so later work resumes instead of restarting.

### 7.4 Scan phases

The scanner prioritizes a usable textual library:

1. Enumerate candidate MP3 files from registered folder trees, individual documents, and the authorized MediaStore audio collection.
2. Read essential metadata and duration.
3. Commit searchable records in batches.
4. Decode and cache embedded artwork by album identity.
5. Reconcile records not observed during the completed root scan and mark them unavailable.

Artwork processing may trail metadata so artwork cannot delay the searchable library unnecessarily.

## 8. Queue model and commands

Starting a track from a metadata view makes the entire currently displayed and sorted collection the queue, with the selected track as current. Items before it remain available behind the cursor and items after it remain ahead.

- **Play Now:** Replaces the queue with the chosen track or collection and begins playback.
- **Play Next:** Inserts the chosen item or collection directly after the current track, including while shuffle is enabled.
- **Add to Queue:** Appends in normal mode. In shuffle mode, inserts each new item uniformly among the remaining unplayed positions.
- Queue reorder and removal affect the current queue only and never rewrite its source playlist.
- Reordering a shuffled queue edits the current cycle without turning shuffle off. A later Repeat All cycle still receives a new uniform permutation.
- The queue engine maintains actual listening history separately from source order and upcoming order.

The complete session is persisted: source order, current-cycle order, current item, position, history, unplayed items, shuffle mode and permutation, repeat mode, and user edits. After app or phone restart, the session is restored but remains paused until the user starts playback.

## 9. Shuffle semantics

Shuffle is a uniformly random permutation, not repeated random selection and not a constrained or weighted `smart shuffle`.

- Use unbiased Fisher–Yates with an injectable random source backed by Android `SecureRandom` in production.
- Every eligible track appears exactly once in a cycle.
- Artist, album, genre, and prior-play weighting are forbidden in V1.
- Natural clusters are accepted as a property of true randomness.
- Previous follows the actual playback history, not a newly randomized choice.
- Next advances through the already-created permutation.
- App restart restores the exact current permutation.

When shuffle is enabled during playback, the current track and history remain fixed. All other unplayed items are uniformly shuffled. When shuffle is disabled, the current track and history remain fixed, and remaining unplayed items return to their relative source order.

`Play Next` is an explicit override that inserts directly after the current track. `Add to Queue` during shuffle inserts uniformly into the remaining unplayed positions. Neither command may place an item into history or interrupt the current track.

With Repeat All, completion of the current cycle creates one fresh uniform permutation for the next cycle. No other automatic event regenerates the permutation.

## 10. Repeat and navigation semantics

Repeat has exactly three clearly labeled states:

- **Off:** Playback stops after the last playable item in the current queue.
- **All:** Completion begins a new full-queue cycle. If shuffle is enabled, the new cycle uses a fresh uniform permutation.
- **One:** Natural completion replays the current track. Manual Next and Previous remain functional.

The UI, notification, lock screen, and Samsung earbuds use the same navigation rules:

- A Previous command received after more than three seconds of the current track restarts that track.
- A Previous command within the first three seconds moves through actual listening history.
- If actual history is empty, Previous uses the prior item behind the current queue cursor when one exists.
- Pressing Previous twice in succession can therefore restart and then move backward.
- Next always advances when an eligible next track exists, including during Repeat One.
- Controls are disabled only at a true unavailable boundary, never for commercial or account-related gating.

If a queued item is unavailable or fails to decode, playback reports the problem and advances to the next playable item without deleting or reordering the saved queue.

## 11. Audio behavior

- Media3 owns playback and MediaSession state.
- Audio continues while the app is backgrounded or the screen is locked.
- Standard media notification, lock-screen, hardware, and Samsung earbud commands are supported.
- The app responds correctly to Android audio focus. Calls and permanent focus loss pause playback; transient interruption behavior follows Android media conventions.
- Removing connected earbuds pauses playback instead of switching unexpectedly to the phone speaker.
- Gapless transitions are enabled where Media3 and the MP3 encoder metadata permit them. Crossfade is not provided.
- Existing ReplayGain metadata is applied when present and recognized. Files without usable tags play at their original level in V1.

## 12. Interface design

### 12.1 Navigation

The app opens directly to the local library. Primary destinations are Artists, Albums, Tracks, Genres, and Playlists. Search is always readily accessible. There is no recommendation or promotional home feed.

### 12.2 Mini-player

Whenever a playback session exists, the persistent mini-player shows:

- Locally available album art.
- Track title and artist.
- Previous, play/pause, and next controls.
- A lightweight equalizer-style animation while playback is active.
- An accessible textual/state description such as Playing, Paused, or Unavailable.

The V1 animation reflects playback state, not decoded signal amplitude. It becomes still when paused. Reduced-motion settings replace motion with a visually distinct static indicator while preserving the accessible state text.

### 12.3 Now Playing

The full screen exposes these without an overflow menu:

- Previous, play/pause, and next.
- Seek bar with elapsed and remaining time.
- Shuffle with icon and explicit state label.
- Repeat with icon and explicit Off, All, or One label.
- Favorite toggle.
- Open Queue.

Less frequent actions, including Play Next, Add to Playlist, and track details, may use a secondary menu.

### 12.4 Queue and playlist separation

The queue screen supports reorder and removal. Queue edits never mutate playlists. Playlist editing is a separate, explicit context.

### 12.5 Themes

Light, dark, and follow-system modes are supported in V1. Follow-system is the default.

## 13. Backup and restore

### 13.1 Backup contents

Backups are unencrypted, portable, versioned ZIP files containing:

- A human-readable manifest with backup and schema versions.
- Playlists and playlist order.
- Favorites.
- Settings.
- Selected-source logical descriptors.
- User-owned references to unavailable tracks.
- The saved playback session, including queue and shuffle state.
- Sufficient portable track identity data to relink user-owned references.

Backups exclude MP3 audio, cached artwork, the full disposable library index, temporary scan state, and logs that are not required for recovery.

### 13.2 Backup schedule

- `Back Up Now` creates a manually named backup on demand.
- Automatic backup is enabled after the user selects a backup folder.
- Meaningful user-data changes schedule a debounced backup no more than once per day.
- Playback-position-only changes do not schedule a full backup.
- The seven newest automatic backups are retained.
- Manually named backups are never deleted by automatic retention.

### 13.3 Safe writes

The manager writes a temporary file, validates its contents, and then promotes it to the final backup name. An interrupted or invalid write must not replace the prior valid backup.

### 13.4 Restore and cross-device relinking

Before changing user data, restore validates the archive type, manifest, supported schema version, and required records. It then creates a safety backup of current user data.

On another phone, the user selects the relevant music roots. The app rescans and relinks references using root descriptors and relative paths first, then corroborating file facts and normalized metadata. Ambiguous or unmatched references remain unavailable for later resolution; restore must not guess between multiple equally plausible tracks.

Newer unsupported backup schemas are rejected with an actionable explanation. Older supported schemas are migrated through explicit, tested transformations.

## 14. Failure handling

- Revoked folder or document permission marks that source unavailable and offers SAF reauthorization.
- Revoked `READ_MEDIA_AUDIO` permission marks MediaStore-derived tracks unavailable without affecting SAF-selected sources and offers the discovery action again.
- A temporarily disconnected or renamed source cannot erase playlists or favorites.
- Database and user-data mutations are transactional.
- Interrupted scans resume from checkpoints.
- Scanner errors are aggregated into a reviewable report and do not flood the interface.
- A current-track playback failure is shown clearly, then playback advances when possible.
- If no playable item remains, playback stops while preserving the queue for inspection.
- Failed backup creation leaves the previous valid backups intact.
- Failed restore leaves current user data intact and reports the failing validation or migration stage.

## 15. Performance targets

Targets apply to the two recent Samsung devices and a library of at least 50,000 synthetic records:

- The existing indexed library becomes usable within one second of app opening.
- Ordinary search returns its first visible results in approximately 100 milliseconds after query submission/debounce.
- Common playback controls visibly respond within approximately 100 milliseconds.
- Background scanning causes no audible dropout and no persistent UI jank.
- Scanning and artwork work are batched and memory-bounded; the design must not require holding the full decoded artwork set or entire file tree in memory.

These are acceptance targets to measure on the actual devices, not reasons to hide progress or return incomplete results without explanation.

## 16. Verification strategy

### 16.1 Unit and property tests

- Queue state transitions and persistence.
- Every shuffle cycle contains each eligible item exactly once.
- Shuffle toggle preserves current track and history.
- Add to Queue inserts uniformly among unplayed shuffle positions.
- Play Next overrides the random placement rule.
- Repeat Off, All, and One interactions.
- Manual Next and Previous, including the three-second restart threshold.
- Queue reorder, unavailable items, and restored sessions.
- Backup retention and version migrations.

The random source is injectable. Deterministic sources verify exact state transitions, while repeated statistical diagnostics check for positional and transition bias without making ordinary tests flaky.

### 16.2 Database and scan tests

- Incremental add, update, missing, reappearance, cancellation, and resume.
- Corrupt MP3 and incomplete metadata handling.
- Compilation, multi-disc, duplicate-title, and unknown-field grouping.
- FTS search over 50,000 or more records.
- Scanner transactions cannot mutate user-owned tables or playback-session records.

### 16.3 Android integration tests

- SAF permission persistence and reauthorization.
- Background playback and session restoration.
- Lock-screen, notification, and Samsung-style headset commands.
- Audio focus and audio-route removal.
- Dedicated scan entry, playback stop, keep-screen-on behavior, explicit exit, and checkpoint resume.
- Light, dark, follow-system, and reduced-motion behavior.

### 16.4 Backup tests

- Interrupted and invalid writes.
- Same-device restore.
- Cross-device restore with changed content URIs and equivalent relative paths.
- Ambiguous and missing-track relinking.
- Safety backup and transactional rollback.

### 16.5 Release checks

- Inspect the merged release manifest against the permission allowlist.
- Verify `READ_MEDIA_AUDIO` is requested only after the explicit `Find All Music on This Device` action and that denial leaves scoped sources functional.
- Confirm no network, telemetry, advertising, or online crash-reporting dependency is packaged.
- Install and update the signed APK on both target Samsung devices without losing data.
- Perform real-device playback, search, scan, backup, restore, and earbud acceptance tests.

## 17. V1 success criteria

V1 is successful when both users can choose folders or individual MP3 files without broad access, optionally authorize discovery across Android's shared media library, select a separate backup folder, complete or resume a large initial scan, search and browse tens of thousands of MP3s responsively, play music reliably in the background with Samsung earbuds, predict shuffle and repeat behavior from the rules above, recover their user-owned data from a USB-visible backup on the same or another phone, and verify that the installed app has no internet or all-files permission.
