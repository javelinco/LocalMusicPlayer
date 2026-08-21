# Unified Track Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one consistent seven-action menu to every available track, wire correct Play Next/Add to Queue behavior to the real Media3 queue, and make reversible index removal survive scans and backups.

**Architecture:** A reusable Compose track-action component delegates playback, playlist, artist-navigation, information, and ignore commands to root coordinators. Media3 remains the playback authority while `PlaybackViewModel` publishes its real ordered media IDs; Room owns durable ignored-track rules that scanner batches consult transactionally and the portable backup layer serializes. The work is split into independently testable catalog, backup, playback, and UI slices.

**Tech Stack:** Kotlin 2.3.21, Android SDK 37/minimum SDK 33, Jetpack Compose Material 3, Room 2.8.4 with KSP, Media3 1.11.0, kotlinx serialization, coroutines, JUnit 4, AndroidX/Compose instrumentation tests.

## Global Constraints

- The menu contains exactly: Play now, Play next, Add to queue, Add to playlist, Go to artist, Track information, and Remove from library.
- `Go to album`, `Go to genre`, favorites, tag editing, sharing, and audio-file deletion are excluded.
- Removing from Library never deletes or modifies the MP3 and never rewrites playlists or the current queue.
- Removed tracks stay ignored across scans until explicitly restored from Library tools.
- Play Next overrides shuffle placement; Add to Queue appends normally and uses a uniform unplayed-position draw while shuffle is active.
- No new runtime permission, internet capability, telemetry, online dependency, or broad storage access may be introduced.
- Every task follows red-green-refactor and preserves the current true-uniform Fisher–Yates shuffle contract.

---

## File Structure

```text
app/src/main/java/com/javelinco/localmusicplayer/
  data/db/Entities.kt                         ignored-track Room entity
  data/db/IgnoredTrackIdentity.kt             exact and portable ignore matching
  data/db/LibraryDao.kt                       transactional ignore/restore and scanner filtering
  data/db/LocalMusicDatabase.kt               schema version 3
  data/db/DatabaseMigrations.kt               migration 2 -> 3
  data/scan/RoomScanCatalog.kt                scanner uses ignore-aware batch application
  data/backup/BackupModels.kt                 portable ignored-track DTO
  data/backup/RoomBackupDataSource.kt          snapshot/relink/restore ignored rules
  library/LibraryViewModel.kt                 ignore/restore commands and ignored flow
  playback/service/PlaybackViewModel.kt       Play Next/Add to Queue and actual queue state
  ui/library/TrackActions.kt                  reusable overflow, info, and confirmation UI
  ui/library/IgnoredTracksScreen.kt            restore management UI
  ui/library/LibraryScreen.kt                 action menu and artist-entry routing
  ui/library/MetadataDetailScreen.kt          shared track actions
  ui/library/PlaylistScreen.kt                shared track actions for available entries
  ui/home/HomeScreen.kt                       shared track actions for recent tracks
  ui/player/QueueScreen.kt                     actual queue rows and shared actions
  ui/navigation/AppNavigation.kt              cross-surface dialogs, picker, and artist routing
  MainActivity.kt                              production callback wiring
```

### Task 1: Durable Ignore Rules and Room Migration

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/Entities.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LibraryDao.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LocalMusicDatabase.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/DatabaseMigrations.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/AppContainer.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/db/LibraryDaoTest.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/db/DatabaseMigrationTest.kt`
- Generated: `app/schemas/com.javelinco.localmusicplayer.data.db.LocalMusicDatabase/3.json`

**Interfaces:**
- Consumes: existing `TrackEntity` and Room transaction support.
- Produces: `IgnoredTrackEntity`, portable identity conversion, `LibraryDao.observeIgnoredTracks()`, `ignoredTracks()`, `ignoreTrack(trackId, ignoredAtEpochMs)`, `restoreIgnoredTrack(ignoreId)`, and `replaceIgnoredTracks(rows)`.

- [ ] **Step 1: Write failing DAO tests for ignore and restore**

Add tests that insert a track plus a playlist entry, call `ignoreTrack`, and assert that available queries/search omit it while `allTracks()` and the playlist entry retain it. Add a restore assertion:

```kotlin
dao.ignoreTrack("first", ignoredAtEpochMs = 20)
assertEquals(emptyList<TrackEntity>(), dao.observeAvailableTracks().first())
assertEquals("first", dao.observeIgnoredTracks().first().single().ignoreId)
assertEquals("first", userDao.observeAllPlaylistEntries().first().single().trackId)

dao.restoreIgnoredTrack("first")
assertEquals("first", dao.observeAvailableTracks().first().single().trackId)
assertEquals(emptyList<IgnoredTrackEntity>(), dao.observeIgnoredTracks().first())
```

- [ ] **Step 2: Run the DAO test and verify the red state**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.db.LibraryDaoTest`

Expected: compilation fails because ignored-track APIs do not exist.

- [ ] **Step 3: Add the entity and transactional DAO operations**

Define the retained identity snapshot:

```kotlin
@Entity(tableName = "ignored_tracks", indices = [Index("sourceId")])
data class IgnoredTrackEntity(
    @PrimaryKey val ignoreId: String,
    val trackId: String?,
    val sourceId: String?,
    val contentUri: String?,
    val relativePath: String?,
    val fileName: String,
    val title: String?,
    val artist: String?,
    val normalizedTitle: String,
    val normalizedArtist: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val ignoredAtEpochMs: Long,
)
```

Add private insert/delete/update queries plus public `@Transaction` methods. A newly ignored local track uses its current track ID as `ignoreId` and retains both exact and portable identity. `ignoreTrack` reads the current row, inserts its snapshot, then sets `available = 0`. `restoreIgnoredTrack` reads and deletes the rule, then sets its currently linked catalog row to `available = 1`. Both operations are idempotent; nullable source/track fields allow an unmatched cross-phone backup rule to persist until a later scan relinks it.

- [ ] **Step 4: Add and test migration 2 to 3**

Create `MIGRATION_2_3` with the exact `ignored_tracks` table and source index, raise `LocalMusicDatabase.version` to 3, register both migrations in `AppContainer`, and extend `DatabaseMigrationTest`:

```kotlin
helper.runMigrationsAndValidate(
    TEST_DB,
    3,
    true,
    DatabaseMigrations.MIGRATION_1_2,
    DatabaseMigrations.MIGRATION_2_3,
)
```

- [ ] **Step 5: Run database tests and schema validation**

Run: `./gradlew.bat :app:kspDebugKotlin :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.db.DatabaseMigrationTest,com.javelinco.localmusicplayer.data.db.LibraryDaoTest`

Expected: PASS and schema `3.json` is generated.

- [ ] **Step 6: Commit the database slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/db app/src/main/java/com/javelinco/localmusicplayer/AppContainer.kt app/src/androidTest/java/com/javelinco/localmusicplayer/data/db app/schemas
git commit -m "feat: add durable ignored track records"
```

### Task 2: Ignore-Aware Scanning and Library Management

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LibraryDao.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/IgnoredTrackIdentity.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/RoomScanCatalog.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/IgnoredTracksScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/db/IgnoredTrackIdentityTest.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/db/LibraryDaoTest.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`

**Interfaces:**
- Consumes: Task 1's ignored-track DAO API.
- Produces: ignore-filtered scan batches, `LibraryViewModel.ignoredTracks`, `ignoreTrack(trackId)`, `restoreIgnoredTrack(trackId)`, and an Ignored tracks Library-tools view.

- [ ] **Step 1: Write failing matcher and Room scanner-batch regression tests**

In the pure matcher test, cover exact ID, changed-ID portable match, and an ambiguous metadata match that must be rejected. In `LibraryDaoTest`, seed an ignored ID, apply a later scan batch with the same stable ID, and assert it is still unavailable while other files are indexed. Add a second batch whose source/track ID changed but whose portable path, size, duration, normalized title, and normalized artist match:

```kotlin
dao.ignoreTrack("source:ignored", 20)
coordinator.run(ScanExecutionMode.DEDICATED)
assertFalse(dao.track("source:ignored")!!.available)
assertTrue(dao.track("source:kept")!!.available)
```

- [ ] **Step 2: Run the focused scan test and verify failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.data.db.IgnoredTrackIdentityTest" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.db.LibraryDaoTest`

Expected: FAIL because current batch upsert resets `available` to true.

- [ ] **Step 3: Filter ignored IDs inside the scan transaction**

In `LibraryDao.applyScanBatch`, load the ignored identities, match incoming IDs and portable identity, upsert matches as unavailable, and exclude their rows from FTS replacement. Keep the coordinator's `seenTrackIds` unchanged so reconciliation recognizes that the physical file still exists:

```kotlin
val matches = matchIgnoredTracks(batch.tracks, ignoredTracks())
val indexed = batch.tracks.map { track ->
    if (track.trackId in matches) track.copy(available = false) else track
}
```

Do not delete or skip the unavailable catalog row: retaining it lets Ignored tracks restore immediately. Guard the empty batch, reject ambiguous portable matches, and run the focused scan tests until green.

- [ ] **Step 4: Expose ignore state and commands from LibraryViewModel**

Add:

```kotlin
val ignoredTracks = libraryDao.observeIgnoredTracks()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

fun ignoreTrack(trackId: String) = viewModelScope.launch {
    libraryDao.ignoreTrack(trackId, System.currentTimeMillis())
}

fun restoreIgnoredTrack(trackId: String) = viewModelScope.launch {
    libraryDao.restoreIgnoredTrack(trackId)
}
```

- [ ] **Step 5: Write failing Compose tests for ignored-track management**

Verify Library tools shows `Ignored tracks (1)`, opens the management view, and invokes restore from a named ignored row. Verify an empty state reads `No ignored tracks.`

- [ ] **Step 6: Implement the management view**

Add a compact `IgnoredTracksScreen` with Back and `Restore to library` per row. Extend `LibraryScreenState` with `ignoredTracks` and `LibraryActions` with `onRestoreIgnoredTrack`. Library tools opens the view without making it a permanent primary navigation item.

- [ ] **Step 7: Run scanner and UI tests, then commit**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.data.db.IgnoredTrackIdentityTest" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.db.LibraryDaoTest,com.javelinco.localmusicplayer.ui.LibraryUiTest`

Expected: PASS.

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/data app/src/main/java/com/javelinco/localmusicplayer/library app/src/main/java/com/javelinco/localmusicplayer/ui/library app/src/test/java/com/javelinco/localmusicplayer/data/db app/src/androidTest/java/com/javelinco/localmusicplayer/data/db app/src/androidTest/java/com/javelinco/localmusicplayer/ui
git commit -m "feat: keep ignored tracks out of rescans"
```

### Task 3: Portable Backup of Ignore Rules

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/backup/BackupModels.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/backup/RoomBackupDataSource.kt`
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/data/backup/BackupCodecTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/backup/RoomBackupDataSourceTest.kt`

**Interfaces:**
- Consumes: `IgnoredTrackEntity`, `PortableTrackReference`, and `TrackRelinker`.
- Produces: `BackupIgnoredTrack`, optional `BackupUserData.ignoredTracks`, and ignore-rule snapshot/relink/restore.

- [ ] **Step 1: Write failing codec compatibility tests**

Add a round-trip containing one ignore rule and decode a schema-1 JSON document with the `ignoredTracks` key absent. The older document must produce an empty list:

```kotlin
assertEquals(emptyList<BackupIgnoredTrack>(), decoded.userData.ignoredTracks)
```

- [ ] **Step 2: Add the portable DTO**

```kotlin
@Serializable
data class BackupIgnoredTrack(
    val oldTrackId: String,
    val reference: PortableTrackReference,
    val title: String? = null,
    val artist: String? = null,
    val fileName: String = "",
    val ignoredAtEpochMs: Long = 0,
)
```

Add `ignoredTracks: List<BackupIgnoredTrack> = emptyList()` to `BackupUserData` and raise `CURRENT_SCHEMA_VERSION` to 2 while retaining decoder support for versions 1 and 2.

- [ ] **Step 3: Write failing Room snapshot/restore tests**

Snapshot an ignored row and assert its portable path, size, duration, normalized title, and normalized artist. Restore that bundle into a database whose matching track has a different track ID/content URI, then assert the new ID is ignored and unavailable.

- [ ] **Step 4: Implement snapshot and portable relinking**

Use the same candidate list and `TrackRelinker` used for playlists. Snapshot from `libraryDao.ignoredTracks()`. During restore, map `oldTrackId` to the current candidate, build current-device `IgnoredTrackEntity` snapshots, then call `replaceIgnoredTracks` transactionally after backup validation. Persist unmatched rules with nullable current track/source fields plus their portable reference so a later scan can relink them; never select an ambiguous candidate or make an unrelated track unavailable.

- [ ] **Step 5: Run backup tests and commit**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.backup.*" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.backup.RoomBackupDataSourceTest`

Expected: PASS for schema-1 compatibility, schema-2 round trip, same-phone restore, and relinked restore.

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/backup app/src/test/java/com/javelinco/localmusicplayer/data/backup app/src/androidTest/java/com/javelinco/localmusicplayer/data/backup
git commit -m "feat: back up ignored track rules"
```

### Task 4: Real Playback Queue Commands

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/PlaybackViewModel.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playback/service/QueuePlacementTest.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/playback/service/PlaybackServiceTest.kt`

**Interfaces:**
- Consumes: `TrackEntity`, existing Media3 controller, `SecureRandom`, and current materialized shuffle order.
- Produces: `PlaybackUiState.queueMediaIds`, `PlaybackUiState.actionMessage`, `playNext(track)`, `addToQueue(track)`, and `dismissActionMessage()`.

- [ ] **Step 1: Write failing pure placement tests**

Extract a small internal helper whose random source can be injected:

```kotlin
internal fun queueInsertionIndex(
    currentIndex: Int,
    itemCount: Int,
    shuffleEnabled: Boolean,
    random: (Int) -> Int,
): Int
```

Assert Play Next always uses `currentIndex + 1`; normal Add uses `itemCount`; shuffled Add draws one of every index in the inclusive range after current through the end. Bounds of 1 must not call an invalid random bound.

- [ ] **Step 2: Run the focused JVM test and verify failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.playback.service.QueuePlacementTest"`

Expected: compilation fails because the helper and commands do not exist.

- [ ] **Step 3: Implement Media3 queue mutation**

Add `queueMediaIds` by reading `player.getMediaItemAt(index).mediaId` in `update`. Implement:

```kotlin
fun playNext(track: TrackEntity)
fun addToQueue(track: TrackEntity)
fun dismissActionMessage()
```

When no media item exists, both commands call the one-track play path. Otherwise Play Next inserts at `currentMediaItemIndex + 1`; Add inserts at `mediaItemCount` normally or a `SecureRandom.nextInt(remainingSlots + 1)` offset after current while shuffle is active. Update the retained source-order list consistently so disabling shuffle does not discard explicit additions. Neither command calls `seekTo` or interrupts the current item.

- [ ] **Step 4: Add Media3 integration assertions**

Extend `PlaybackServiceTest` to create a session, invoke the controller additions, and assert current media ID is unchanged while the ordered controller media IDs contain the inserted track at the expected position.

- [ ] **Step 5: Run playback suites repeatedly**

Run three times: `./gradlew.bat :app:testDebugUnitTest --tests "*.playback.*"`

Then run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.playback.service.PlaybackServiceTest`

Expected: all passes; shuffle distribution/property tests remain unchanged and green.

- [ ] **Step 6: Commit the playback slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/playback app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/test/java/com/javelinco/localmusicplayer/playback app/src/androidTest/java/com/javelinco/localmusicplayer/playback
git commit -m "feat: wire play next and add to queue"
```

### Task 5: Reusable Track Action UI

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/TrackActions.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataDetailScreen.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`

**Interfaces:**
- Consumes: a `TrackEntity`, its source label/location, and callbacks from Tasks 1 and 4.
- Produces: `TrackActionCallbacks`, `TrackActionMenu`, `TrackInformationDialog`, and `RemoveTrackDialog`.

- [ ] **Step 1: Write failing Compose tests for the exact menu**

Open `More actions for First track` and assert exactly the seven labels. Invoke Play Next, Add to Queue, and Go to Artist independently and assert only the selected callback fires. Verify no Go to album/genre, Favorite, Share, Edit, or Delete file text exists.

- [ ] **Step 2: Write failing information and removal tests**

Verify Track information shows title, artist, album, genre, formatted `3:00`, filename, and source. Verify Remove first opens a dialog containing `The MP3 file will not be deleted or modified` and only its confirm button invokes ignore.

- [ ] **Step 3: Implement the typed reusable actions**

Define:

```kotlin
data class TrackActionCallbacks(
    val onPlayNow: (TrackEntity) -> Unit,
    val onPlayNext: (TrackEntity) -> Unit,
    val onAddToQueue: (TrackEntity) -> Unit,
    val onAddToPlaylist: (TrackEntity) -> Unit,
    val onGoToArtist: (TrackEntity) -> Unit,
    val onShowInformation: (TrackEntity) -> Unit,
    val onRemoveFromLibrary: (TrackEntity) -> Unit,
)
```

Render a Material 3 `DropdownMenu` from a visible MoreVert icon. Close the menu before invoking every callback. Retain whole-card Play now and replace the dedicated playlist icon with the unified overflow to keep rows uncluttered.

- [ ] **Step 4: Implement read-only and destructive-confirmation dialogs**

The information dialog formats absent text as `Unknown` and duration as minutes/seconds. The removal dialog uses `Remove from library` rather than `Delete` and repeats that the file remains untouched and the action is reversible from Ignored tracks.

- [ ] **Step 5: Run Compose tests and commit**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest`

Expected: PASS.

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui/library app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt
git commit -m "feat: add unified track action menu"
```

### Task 6: Cross-Surface Actions, Artist Routing, and Actual Queue Screen

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/QueueScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/PlayerUiTest.kt`
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/ui/navigation/NavigationPolicyTest.kt`

**Interfaces:**
- Consumes: Task 5's action component, `PlaybackUiState.queueMediaIds`, source list, playlists, and Library callbacks.
- Produces: identical actions on all available track surfaces, cross-surface playlist picker, artist detail routing, and queue rows ordered from the real Media3 list.

- [ ] **Step 1: Write failing routing and queue-screen tests**

Verify Go to Artist from a recent track selects Library/Artists and opens the matching normalized artist. Verify an empty artist opens Unknown Artist. Supply queue IDs `two, one` while Library order is `one, two, three` and assert Queue shows only Second then First, with Playing on the current ID.

- [ ] **Step 2: Centralize cross-surface transient state**

In `AppNavigation`, own the pending playlist track, information track, removal track, and requested normalized artist. Render the existing playlist picker plus Task 5 dialogs once at the root. Build one callback object and pass it to Home, Library, Playlist, and Queue surfaces.

- [ ] **Step 3: Implement artist routing**

Go to Artist must:

```kotlin
libraryActions.onSelectView(LibraryView.ARTISTS)
requestedArtist = track.normalizedArtist
destination = Destination.LIBRARY
```

`LibraryScreen` consumes the request after the observed artist group is present, opens `OpenedMetadataGroup(LibraryView.ARTISTS, group)`, and clears the request. Back returns to Artists.

- [ ] **Step 4: Replace each available-track row with the shared action contract**

Use the component in recent tracks, Library/search/detail track cards, available playlist entries, and queue rows. Each surface passes its displayed ordered collection to Play now. Unavailable playlist snapshots retain only playlist-entry management.

- [ ] **Step 5: Render the actual playback queue**

Map `playback.queueMediaIds` to `TrackEntity` rows in the same order; do not pass the full Library as queue content. Use indexed keys so the UI remains stable if an ID repeats. Keep current-track labeling and expose the same track menu.

- [ ] **Step 6: Wire production callbacks and non-blocking status**

Connect MainActivity to `playbackViewModel.playNext`, `addToQueue`, and `libraryViewModel.ignoreTrack`. Show `PlaybackUiState.actionMessage` through a Material 3 SnackbarHost and dismiss it after display.

- [ ] **Step 7: Run navigation and UI suites, then commit**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.ui.navigation.*" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest,com.javelinco.localmusicplayer.ui.PlayerUiTest`

Expected: PASS.

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/main/java/com/javelinco/localmusicplayer/ui app/src/test/java/com/javelinco/localmusicplayer/ui app/src/androidTest/java/com/javelinco/localmusicplayer/ui
git commit -m "feat: expose track actions on every surface"
```

### Task 7: Full Verification, Device Update, and Documentation

**Files:**
- Modify: `README.md`
- Create: `docs/verification/2026-08-21-unified-track-actions.md`
- Update if generated: `dist/Music-Please-v0.1.0-development-signed.apk`

**Interfaces:**
- Consumes: completed feature and attached Samsung device.
- Produces: verified release artifact, preserved user data, device evidence, and updated user documentation.

- [ ] **Step 1: Run all local quality gates**

Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`

Run the repository's existing privacy/dependency/documentation checks listed in `README.md` or Gradle tasks. Expected: all exit 0; merged manifests contain no `android.permission.INTERNET` or `MANAGE_EXTERNAL_STORAGE`.

- [ ] **Step 2: Back up before updating the attached phone**

Use the app's configured USB-visible backup folder to create a manual backup and confirm the ZIP can be listed/read before installation. Record the backup filename. Do not uninstall the app or clear its data.

- [ ] **Step 3: Sign and update-install the APK**

Sign with the existing local development signing identity, verify the certificate SHA-256 remains `724d17783107e9393423bf1032620665ef074706cfc4e77ae5088aa24ed6c942`, then use `adb install -r` so Android preserves app data.

- [ ] **Step 4: Run Samsung acceptance checks**

On the attached device verify: card tap plays; all seven labels appear; Play Next is next; Add to Queue uses the visible actual queue; Add to Playlist works; Go to Artist opens the correct group; information matches metadata; removal requires confirmation and does not delete the MP3; a rescan does not restore it; Ignored tracks restores it. Confirm Previous/Next, repeat, shuffle, lock-screen controls, and earbuds still work.

- [ ] **Step 5: Run the full connected suite and privacy inspection**

Run: `./gradlew.bat :app:connectedDebugAndroidTest`

Expected: every instrumentation test passes. Then verify package permissions show no granted broad media permission unless the user previously chose Find All, no internet permission exists, crash log is empty, and stay-awake is restored after testing.

- [ ] **Step 6: Document evidence and commit**

Document commands, test counts, device model/Android version, backup filename, APK path/hash, signing certificate, permission evidence, and acceptance results. Update README action/ignore descriptions.

```powershell
git add -- README.md docs/verification dist
git commit -m "docs: verify unified track actions"
```

- [ ] **Step 7: Push the verified main branch**

Run: `git status --short`, `git log -7 --oneline`, then `git push origin main`.

Expected: clean worktree and GitHub `main` contains every feature and verification commit.
