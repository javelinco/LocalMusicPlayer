# LocalMusicPlayer V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete privacy-first Android V1 described in the approved design: scoped and optional whole-device MP3 discovery, scalable indexing and search, dedicated/background scanning, deterministic queues, correct shuffle/repeat, background Media3 playback, playlists/favorites, accessible Compose UI, and portable USB-visible backup/restore.

**Architecture:** A single Android application module uses manual dependency injection to keep dependencies auditable. Room owns indexed and user-created data; a source registry abstracts SAF trees, SAF documents, and optional MediaStore discovery; a scan coordinator isolates catalog writes; a pure Kotlin queue engine drives a Media3 playback service; Compose consumes immutable state; and a versioned ZIP backup layer exports only portable user data.

**Tech Stack:** Kotlin 2.3.21, Android Gradle Plugin 9.2.1, Gradle 9.4.1, compile/target SDK 37, minimum SDK 33, Jetpack Compose BOM 2026.08.00, Material 3, Navigation 3 1.1.6, Lifecycle 2.11.0, Room 2.8.4 with KSP, DataStore 1.2.1, Media3 1.11.0, coroutines, JUnit 4, kotlinx-coroutines-test, AndroidX Test 1.7.0, Espresso 3.7.0, and Compose UI tests.

## Global Constraints

- Application ID and Kotlin package: `com.javelinco.localmusicplayer`.
- Android 13/API 33 is the minimum; compile and target API 37.
- MP3 is the only indexed audio format in V1.
- The merged manifest must not contain `android.permission.INTERNET`, `MANAGE_EXTERNAL_STORAGE`, legacy external-storage permissions, Bluetooth, nearby-device, location, contacts, or microphone permissions.
- `READ_MEDIA_AUDIO` is declared solely for the explicit `Find All Music on This Device` action and is never requested during startup or scoped source selection.
- SAF music folders and individual documents are retained read-only; the separately selected backup tree is retained read/write.
- No network, ads, telemetry, analytics, remote crash reporting, or online metadata dependencies.
- Scanner transactions cannot mutate queue, session, playlist, favorite, or settings records.
- Background scanning stops and checkpoints when the UI process is dismissed; dedicated scanning stops playback and keeps the screen on until completion or explicit exit.
- Production shuffle is unbiased Fisher–Yates backed by `SecureRandom`, with no weighting or anti-clustering.
- Follow-system theme is the default; light, dark, reduced-motion, and accessible text state are required.
- V2-only work is excluded: decoded-signal reactive visualization and calculated loudness analysis.
- Every implementation task follows red-green-refactor, runs focused tests before the full suite, and commits only the listed paths.

## File Structure

```text
app/src/main/java/com/javelinco/localmusicplayer/
  LocalMusicPlayerApp.kt              manual dependency container
  MainActivity.kt                     activity-result launchers and root UI
  core/model/                          shared immutable domain models
  data/db/                             Room entities, DAOs, migrations, database
  data/source/                         SAF/MediaStore source registry and readers
  data/scan/                           MP3 extraction, scan plans, progress, coordinator
  data/settings/                       DataStore settings and theme preferences
  data/backup/                         portable DTOs, ZIP writer, validator, relinker
  library/                             library/search repository and view models
  playback/queue/                      pure queue state machine and persistence DTOs
  playback/service/                    Media3 service, controller, media mapping
  playlists/                           favorites and playlist repository/view models
  ui/navigation/                       Navigation 3 keys and root navigation
  ui/library/                          metadata lists, search, source and scan screens
  ui/player/                           mini-player, now-playing, and queue screens
  ui/theme/                            light/dark/system theme and reduced motion
app/src/test/                          JVM unit, property, persistence, and large-index tests
app/src/androidTest/                   SAF, Room, Compose, manifest, and MediaSession tests
benchmark/                             startup/search Macrobenchmark module
```

---

### Task 1: Reproducible Android project and permission gate

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/LocalMusicPlayerApp.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`
- Create: `scripts/check_release_manifest.ps1`

**Interfaces:**
- Consumes: Android SDK 37 and Android Studio bundled JDK at `C:\Program Files\Android\Android Studio\jbr`.
- Produces: installable `app`, `LocalMusicPlayerApp`, and a merged-manifest permission gate used by every later task.

- [ ] **Step 1: Write the failing merged-manifest contract check**

```powershell
param([string]$ManifestPath = "app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml")
$approved = @(
  "android.permission.READ_MEDIA_AUDIO",
  "android.permission.FOREGROUND_SERVICE",
  "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"
)
[xml]$manifest = Get-Content -Raw -LiteralPath $ManifestPath
$actual = @($manifest.manifest.'uses-permission' | ForEach-Object { $_.'android:name' })
$unexpected = @($actual | Where-Object { $_ -notin $approved })
if ($unexpected.Count -ne 0) { throw "Unexpected permissions: $($unexpected -join ', ')" }
$missing = @($approved | Where-Object { $_ -notin $actual })
if ($missing.Count -ne 0) { throw "Missing approved permissions: $($missing -join ', ')" }
```

- [ ] **Step 2: Create the Gradle wrapper and run the red test**

Run: `./scripts/check_release_manifest.ps1`

Expected: FAIL because the merged manifest does not exist.

- [ ] **Step 3: Add the minimal project and manifest**

Pin the versions from the header in `libs.versions.toml`. Configure `namespace`, `applicationId`, `minSdk = 33`, `compileSdk = 37`, `targetSdk = 37`, Compose, KSP, Room schema export, unit resources, and the AndroidX test runner. The main manifest declares only:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

Set `android:allowBackup="false"`; V1 uses its own portable backup rather than OS cloud backup.

- [ ] **Step 4: Run build and permission checks**

Run: `./gradlew.bat :app:testDebugUnitTest :app:processDebugMainManifest :app:lintDebug` then `./scripts/check_release_manifest.ps1`.

Expected: PASS; merged-manifest inspection shows no forbidden permission.

- [ ] **Step 5: Commit**

```bash
git add -- settings.gradle.kts build.gradle.kts gradle.properties gradle app scripts/check_release_manifest.ps1
git commit -m "build: scaffold offline Android application"
```

### Task 2: Domain model and source registry

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/core/model/LibraryModels.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/MusicSource.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SourceRegistry.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SafPermissionStore.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/source/SourceRegistryTest.kt`

**Interfaces:**
- Consumes: Android `Uri` strings at the application boundary.
- Produces: `MusicSource`, `SourceId`, `TrackId`, `SourceRegistry.observeSources(): Flow<List<MusicSource>>`, and add/remove/availability commands.

- [ ] **Step 1: Write source identity and deduplication tests**

```kotlin
@Test fun samePersistedUriDoesNotCreateTwoFolderSources() = runTest {
    val registry = InMemorySourceRegistry()
    registry.add(SafTreeSource(SourceId("a"), "content://tree/music", "Music"))
    registry.add(SafTreeSource(SourceId("b"), "content://tree/music", "Music again"))
    assertEquals(1, registry.observeSources().first().size)
}

@Test fun mediaStorePermissionLossDoesNotDisableSafSources() = runTest {
    val registry = InMemorySourceRegistry(seedSources())
    registry.setAvailability(SourceKind.MEDIA_STORE, false)
    assertTrue(registry.observeSources().first().filterIsInstance<SafTreeSource>().all { it.available })
}
```

- [ ] **Step 2: Run focused tests and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.SourceRegistryTest"`

Expected: FAIL with unresolved source types.

- [ ] **Step 3: Implement immutable source types and registry**

```kotlin
@JvmInline value class SourceId(val value: String)
@JvmInline value class TrackId(val value: String)

sealed interface MusicSource { val id: SourceId; val label: String; val available: Boolean }
data class SafTreeSource(/* id, treeUri, label, available */) : MusicSource
data class SafDocumentSource(/* id, documentUri, displayName, available */) : MusicSource
data class MediaStoreSource(/* id, label, available */) : MusicSource

interface SourceRegistry {
    fun observeSources(): Flow<List<MusicSource>>
    suspend fun add(source: MusicSource)
    suspend fun remove(id: SourceId)
    suspend fun setAvailability(id: SourceId, available: Boolean)
}
```

Persist logical source descriptors in Room in Task 3; keep `SafPermissionStore` responsible only for taking/releasing provider grants.

- [ ] **Step 4: Run focused and full JVM tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.SourceRegistryTest"` then `./gradlew.bat :app:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/core app/src/main/java/com/javelinco/localmusicplayer/data/source app/src/test/java/com/javelinco/localmusicplayer/data/source
git commit -m "feat: add scoped music source registry"
```

### Task 3: Room catalog, full-text search, and protected user tables

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/Entities.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LibraryDao.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/UserDataDao.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LocalMusicDatabase.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryRepository.kt`
- Create: `app/schemas/com.javelinco.localmusicplayer.data.db.LocalMusicDatabase/1.json`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/db/LibraryDaoTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/db/ScannerIsolationTest.kt`

**Interfaces:**
- Consumes: `MusicSource` and normalized MP3 metadata.
- Produces: `TrackEntity`, `TrackSearchFts`, album/artist/genre projections, transactional `LibraryDao.applyScanBatch(batch: ScanBatch)`, and `LibraryRepository.search(query, filter)`.

- [ ] **Step 1: Write in-memory Room tests**

Test album grouping by `(normalizedAlbumArtist, normalizedAlbumTitle)`, disc/track ordering, filename fallback search, unknown metadata buckets, missing-track preservation, and scanner isolation. The isolation test inserts a playlist, favorite, and session row, applies a scan batch, then asserts their row hashes are unchanged.

- [ ] **Step 2: Run instrumentation tests and confirm failure**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.db.LibraryDaoTest`

Expected: FAIL because the database is undefined.

- [ ] **Step 3: Implement schema and DAOs**

Use separate tables for sources, tracks, FTS, scan checkpoints, scan errors, playlists, playlist entries, favorites, queue session, and settings metadata. Use foreign keys that preserve user entries when catalog tracks become unavailable. `applyScanBatch` may touch only sources/tracks/FTS/checkpoint/error tables.

```kotlin
@Transaction
suspend fun applyScanBatch(batch: ScanBatch) {
    upsertTracks(batch.tracks)
    upsertSearchRows(batch.tracks.map(TrackSearchFts::from))
    saveCheckpoint(batch.checkpoint)
}
```

- [ ] **Step 4: Run Room tests and export schema**

Run: `./gradlew.bat :app:connectedDebugAndroidTest :app:kspDebugKotlin`

Expected: PASS and schema version 1 is committed.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/db app/src/main/java/com/javelinco/localmusicplayer/library app/src/androidTest/java/com/javelinco/localmusicplayer/data/db app/schemas
git commit -m "feat: add scalable local music catalog"
```

### Task 4: Android source acquisition and least-privilege gating

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SourcePickerContracts.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SafTreeReader.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SafDocumentReader.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/MediaStoreReader.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SourceEntry.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/source/PermissionFlowTest.kt`

**Interfaces:**
- Consumes: activity-result contracts and `ContentResolver`.
- Produces: `SourceReader.enumerate(source, checkpoint): Flow<SourceEntry>` and three explicit UI commands: `chooseFolder`, `chooseFiles`, `findAllDeviceMusic`.

- [ ] **Step 1: Write permission-flow tests**

Assert startup does not request `READ_MEDIA_AUDIO`; folder selection retains only read permission; multi-document selection registers only returned URIs; and whole-device discovery requests the runtime permission only after its explanation action.

- [ ] **Step 2: Run and observe failure**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.source.PermissionFlowTest`

Expected: FAIL because launchers and readers are absent.

- [ ] **Step 3: Implement source launchers and readers**

Use `OpenDocumentTree`, `OpenMultipleDocuments`, and `RequestPermission`. Query document-tree children with `DocumentsContract` projections rather than recursively constructing `DocumentFile` objects. MediaStore queries must filter MIME type `audio/mpeg` and `IS_MUSIC != 0` where supported. Readers expose content URIs and metadata hints but never raw unrestricted paths.

- [ ] **Step 4: Verify denial and revocation paths**

Run the focused instrumentation class, then `./gradlew.bat :app:connectedDebugAndroidTest`.

Expected: PASS; denial leaves scoped sources usable.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/source app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/androidTest/java/com/javelinco/localmusicplayer/data/source
git commit -m "feat: add least-privilege music discovery"
```

### Task 5: MP3 extraction and resumable scanning

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/Mp3MetadataExtractor.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/MetadataNormalizer.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/ScanModels.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/ScanCoordinator.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/ArtworkCache.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/scan/MetadataNormalizerTest.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/scan/ScanCoordinatorTest.kt`
- Create: `app/src/androidTest/assets/mp3/valid-tagged.mp3`
- Create: `app/src/androidTest/assets/mp3/corrupt.mp3`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/scan/Mp3MetadataExtractorTest.kt`

**Interfaces:**
- Consumes: `Flow<SourceEntry>`, `ContentResolver`, `LibraryDao`, and a `ScanExecutionMode`.
- Produces: `StateFlow<ScanProgress>`, checkpointed catalog batches, error report rows, and album-keyed artwork cache entries.

- [ ] **Step 1: Write failing normalization and coordinator tests**

Cover ID3v1/v2 fallback, album-artist fallback, compilation grouping, multi-disc order, skipped non-MP3s, corrupt MP3 continuation, cancellation checkpoint, resume without duplicate work, and user-table hashes unchanged after batches.

- [ ] **Step 2: Run focused tests and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.scan.*"`

Expected: FAIL with missing extractor/coordinator types.

- [ ] **Step 3: Implement extraction and phased scan state machine**

```kotlin
enum class ScanPhase { ENUMERATING, METADATA, INDEXING, ARTWORK, RECONCILING, COMPLETE }
enum class ScanExecutionMode { BACKGROUND, DEDICATED }
data class ScanProgress(val phase: ScanPhase, val found: Long, val processed: Long,
    val skipped: Long, val errors: Long, val determinate: Boolean)

interface ScanCoordinator {
    val progress: StateFlow<ScanProgress?>
    suspend fun run(mode: ScanExecutionMode)
    suspend fun cancelAndCheckpoint()
}
```

Use `MediaMetadataRetriever` only behind `Mp3MetadataExtractor`; close all descriptors deterministically. Batch database writes, bound concurrency, extract text before artwork, cache one artwork payload per album key, and mark missing only after a source completes reconciliation.

- [ ] **Step 4: Run scanner tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.scan.*" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.scan.Mp3MetadataExtractorTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/scan app/src/test/java/com/javelinco/localmusicplayer/data/scan app/src/androidTest
git commit -m "feat: add resumable MP3 scanning"
```

### Task 6: Queue engine, uniform shuffle, repeat, and persistence

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/queue/QueueModels.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/queue/RandomSource.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/queue/QueueEngine.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/queue/QueueSessionStore.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playback/queue/QueueEngineTest.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playback/queue/ShufflePropertyTest.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playback/queue/ShuffleDistributionDiagnosticTest.kt`

**Interfaces:**
- Consumes: ordered `List<TrackId>` and queue commands.
- Produces: `StateFlow<QueueState>`, `QueueEffect.Play(track)`, exact saved/restored session DTOs, and injectable random source.

- [ ] **Step 1: Write state-machine and shuffle tests**

Tests cover full-list queue with selected cursor, Previous history fallback, three-second restart decision, Next during Repeat One, all repeat modes, shuffle toggle, stable app restart, explicit Play Next, random Add to Queue, reorder during shuffle, unavailable skip, and Repeat All reshuffle. Property tests assert every permutation contains each input exactly once. A deterministic random source validates Fisher–Yates swaps; a non-gating diagnostic samples positions and transitions.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.playback.queue.*"`

Expected: FAIL with missing queue types.

- [ ] **Step 3: Implement the pure queue reducer**

```kotlin
sealed interface QueueCommand {
    data class PlayView(val tracks: List<TrackId>, val selected: TrackId) : QueueCommand
    data object Next : QueueCommand
    data class Previous(val positionMs: Long) : QueueCommand
    data class SetShuffle(val enabled: Boolean) : QueueCommand
    data class SetRepeat(val mode: RepeatMode) : QueueCommand
    data class PlayNext(val tracks: List<TrackId>) : QueueCommand
    data class Add(val tracks: List<TrackId>) : QueueCommand
    data class Move(val from: Int, val to: Int) : QueueCommand
    data class Remove(val track: TrackId) : QueueCommand
}
```

Keep source order, cycle order, history, current item, and unplayed set separate. Production `RandomSource` wraps `SecureRandom.nextInt(bound)`; Fisher–Yates iterates downward and swaps with an inclusive `[0, i]` draw.

- [ ] **Step 4: Run queue suite repeatedly**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.playback.queue.*"` three times.

Expected: PASS each run; no flaky statistical gate.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/playback/queue app/src/test/java/com/javelinco/localmusicplayer/playback/queue
git commit -m "feat: add deterministic queue and uniform shuffle"
```

### Task 7: Media3 background playback and system controls

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/PlaybackService.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/PlaybackController.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/MediaItemMapper.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/ReplayGainAudioProcessor.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playback/service/MediaItemMapperTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/playback/service/PlaybackServiceTest.kt`

**Interfaces:**
- Consumes: `QueueState`, `QueueCommand`, catalog track URIs, and existing ReplayGain tags.
- Produces: Media3 `MediaSessionService`, `PlaybackController.state`, queue-to-player synchronization, notification/lock-screen/earbud command handling.

- [ ] **Step 1: Write mapping and session-command tests**

Assert content URIs remain local, unavailable items are skipped without queue deletion, Previous applies the three-second rule, manual Next bypasses Repeat One, audio-becoming-noisy pauses, session restoration stays paused, and no player factory receives an HTTP URI.

- [ ] **Step 2: Run focused tests and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.playback.service.*"`

Expected: FAIL with missing service and mapper.

- [ ] **Step 3: Implement Media3 service**

Declare `PlaybackService` with `foregroundServiceType="mediaPlayback"` and the Media3 service intent. Build ExoPlayer for local progressive MP3 playback, enable gapless behavior supported by encoder metadata, publish MediaSession buttons, pause on `AUDIO_BECOMING_NOISY`, and persist queue state after meaningful transitions. Apply existing ReplayGain only when a parsed gain value is valid; otherwise apply unity gain.

- [ ] **Step 4: Run service tests and manifest check**

Run: `./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.playback.service.PlaybackServiceTest :app:processDebugMainManifest`

Expected: PASS and no new permission outside the allowlist.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/playback app/src/main/AndroidManifest.xml app/src/test/java/com/javelinco/localmusicplayer/playback app/src/androidTest/java/com/javelinco/localmusicplayer/playback
git commit -m "feat: add background Media3 playback"
```

### Task 8: Playlists, favorites, and user-data integrity

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playlists/PlaylistRepository.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playlists/PlaylistViewModel.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playlists/PlaylistRepositoryTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/UserDataDao.kt`

**Interfaces:**
- Consumes: `TrackId`, playlist commands, Room user tables.
- Produces: ordered playlists, favorites flow, explicit unavailable entries, and transactionally safe editing.

- [ ] **Step 1: Write repository tests**

Cover create/rename/delete playlist, ordered add/remove/reorder, favorite toggle, duplicate track entries allowed within a playlist, missing tracks retained, and scanner batch isolation.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.playlists.*"`

Expected: FAIL because repository methods are missing.

- [ ] **Step 3: Implement explicit user-data transactions**

```kotlin
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<PlaylistSummary>>
    fun observeEntries(id: PlaylistId): Flow<List<PlaylistEntry>>
    suspend fun addTracks(id: PlaylistId, tracks: List<TrackId>)
    suspend fun moveEntry(id: PlaylistId, from: Int, to: Int)
    suspend fun setFavorite(track: TrackId, favorite: Boolean)
}
```

Use stable entry IDs so duplicate tracks can be reordered independently. Never cascade-delete entries when a catalog row becomes unavailable.

- [ ] **Step 4: Run playlist and scanner-isolation suites**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.playlists.*" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.db.ScannerIsolationTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/playlists app/src/main/java/com/javelinco/localmusicplayer/data/db/UserDataDao.kt app/src/test/java/com/javelinco/localmusicplayer/playlists
git commit -m "feat: add playlists and favorites"
```

### Task 9: Portable backup, validation, and cross-device relinking

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/backup/BackupModels.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/backup/BackupCodec.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/backup/BackupManager.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/backup/TrackRelinker.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/backup/BackupCodecTest.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/backup/TrackRelinkerTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/data/backup/BackupManagerTest.kt`

**Interfaces:**
- Consumes: user-data snapshot, logical source descriptors, selected backup tree URI, and current catalog.
- Produces: versioned ZIP backups, seven-file automatic retention, manual backups, validated restore plan, safety backup, and unambiguous relink results.

- [ ] **Step 1: Write backup contract tests**

Assert ZIP contains `manifest.json` and `user-data.json`; excludes audio/index/artwork; rejects path traversal and newer schema versions; interrupted write leaves prior file; daily automatic backup is debounced; seven automatic files retained; manual files untouched; changed content URIs relink by root/relative path; ambiguous matches stay unavailable.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.data.backup.*"`

Expected: FAIL with missing backup types.

- [ ] **Step 3: Implement version 1 portable format**

```kotlin
@Serializable data class BackupManifest(
    val format: String = "LocalMusicPlayerBackup",
    val schemaVersion: Int = 1,
    val createdAtEpochMs: Long,
    val appVersion: String,
)

sealed interface RelinkResult {
    data class Matched(val trackId: TrackId) : RelinkResult
    data class Ambiguous(val candidates: List<TrackId>) : RelinkResult
    data object Unavailable : RelinkResult
}
```

Write to a sibling temporary document, reopen and validate the complete ZIP, then promote to the final name. Restore creates a safety backup before one Room transaction. Use exact relative path first, then corroborated size/duration/normalized metadata; never choose between equally ranked candidates.

- [ ] **Step 4: Run backup unit and SAF integration tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.data.backup.*" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.data.backup.BackupManagerTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/backup app/src/test/java/com/javelinco/localmusicplayer/data/backup app/src/androidTest/java/com/javelinco/localmusicplayer/data/backup
git commit -m "feat: add portable backup and restore"
```

### Task 10: Compose navigation, themes, library, search, and source UI

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/theme/AppTheme.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataListScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SearchScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/ScanStatus.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/DedicatedScanScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/settings/AppSettings.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/DedicatedScanUiTest.kt`

**Interfaces:**
- Consumes: library/search flows, source commands, scan progress, theme settings, and `PlaybackController` summary state.
- Produces: Artists/Albums/Tracks/Genres/Playlists navigation, search filters, three source actions, background progress, and exclusive dedicated scan UI.

- [ ] **Step 1: Write Compose UI tests**

Assert primary destinations exist; search results display within metadata categories; source screen offers exactly the three approved access actions and explains whole-device permission; background progress does not block navigation; dedicated scan warns then stops playback, sets keep-screen-on, shows phase/counts/time, disables ordinary navigation, and exits through explicit checkpoint action; themes and reduced-motion semantics are exposed.

- [ ] **Step 2: Run tests and confirm failure**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.javelinco.localmusicplayer.ui`

Expected: FAIL because screens are absent.

- [ ] **Step 3: Implement root UI and state holders**

Use Navigation 3 stable APIs and Material 3. Keep search input local, debounce database submission, and collect flows lifecycle-aware. Dedicated scan applies `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` only for its lifecycle and clears it on disposal. Motion state uses system animator settings; content descriptions always include `Playing`, `Paused`, or `Unavailable` independent of animation.

- [ ] **Step 4: Run Compose and accessibility tests**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.javelinco.localmusicplayer.ui :app:lintDebug`

Expected: PASS with no accessibility lint errors in touched UI.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt app/src/main/java/com/javelinco/localmusicplayer/data/settings app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui
git commit -m "feat: add offline library and scan interface"
```

### Task 11: Mini-player, Now Playing, queue, playlist, and backup UI

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/PlayingIndicator.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/MiniPlayer.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/NowPlayingScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/QueueScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/PlaybackViewModel.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/PlaybackUiTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/BackupUiTest.kt`

**Interfaces:**
- Consumes: `PlaybackController`, `QueueEngine`, playlist repository, backup manager, and reduced-motion setting.
- Produces: direct playback controls, labeled shuffle/repeat, lightweight state animation, queue editing, playlist editing, backup selection/manual backup/restore, and unavailable-state UX.

- [ ] **Step 1: Write UI behavior tests**

Assert mini-player is persistent when a session exists; equalizer animation moves only while playing and becomes static under reduced motion; Now Playing exposes all required controls without overflow; shuffle/repeat have text labels; queue reorder does not alter playlist; backup screen reports selected folder, seven-backup policy, manual naming, validation failure, and restore safety backup.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.PlaybackUiTest,com.javelinco.localmusicplayer.ui.BackupUiTest`

Expected: FAIL because player/backup UI is absent.

- [ ] **Step 3: Implement screens and accessible controls**

Use direct buttons for Previous, Play/Pause, Next, seek, Favorite, Queue, Shuffle, and Repeat. Render repeat text exactly `Repeat Off`, `Repeat All`, or `Repeat One`; shuffle text exactly `Shuffle Off` or `Shuffle On`. Use semantics state descriptions and 48dp minimum touch targets. `PlayingIndicator` is time-based state animation only and never inspects decoded samples.

- [ ] **Step 4: Run UI suite**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.javelinco.localmusicplayer.ui`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui app/src/main/java/com/javelinco/localmusicplayer/playback/service/PlaybackViewModel.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui
git commit -m "feat: complete playback and backup interface"
```

### Task 12: Dependency container and end-to-end lifecycle integration

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/LocalMusicPlayerApp.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/AppContainer.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/AppLifecycleTest.kt`

**Interfaces:**
- Consumes: every repository/service from Tasks 2–11.
- Produces: one auditable manual object graph and lifecycle coordination for automatic scans, dedicated scan, playback, backups, and revocation.

- [ ] **Step 1: Write lifecycle integration tests**

Test first launch with no sources, folder-only flow without media permission, optional MediaStore denial, immediate library from existing DB, auto-scan start, UI dismissal checkpoint, playback surviving activity destruction, dedicated scan stopping playback, daily backup scheduling, and session restore paused after application recreation.

- [ ] **Step 2: Run and confirm failure**

Run: `./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.AppLifecycleTest`

Expected: FAIL because dependencies are not wired together.

- [ ] **Step 3: Implement manual application container**

```kotlin
class AppContainer(context: Context) {
    val database = LocalMusicDatabase.create(context)
    val sourceRegistry: SourceRegistry = RoomSourceRegistry(database.sourceDao())
    val libraryRepository = RoomLibraryRepository(database.libraryDao())
    val queueEngine = QueueEngine(SecureRandomSource(), RoomQueueSessionStore(database.userDataDao()))
    val scanCoordinator = DefaultScanCoordinator(/* readers, extractor, dao, dispatchers */)
    val backupManager = DefaultBackupManager(/* resolver, dao, codec, relinker */)
}
```

Inject dispatchers, clock, and random source through constructors for tests. Do not add a DI framework.

- [ ] **Step 4: Run integration and full suites**

Run: `./gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -- app/src/main/java/com/javelinco/localmusicplayer app/src/androidTest/java/com/javelinco/localmusicplayer/AppLifecycleTest.kt
git commit -m "feat: integrate complete offline player lifecycle"
```

### Task 13: Large-library performance and release privacy verification

**Files:**
- Create: `benchmark/build.gradle.kts`
- Create: `benchmark/src/main/AndroidManifest.xml`
- Create: `benchmark/src/main/java/com/javelinco/localmusicplayer/benchmark/StartupBenchmark.kt`
- Create: `benchmark/src/main/java/com/javelinco/localmusicplayer/benchmark/SearchBenchmark.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/library/LargeLibraryTest.kt`
- Modify: `scripts/check_release_manifest.ps1`
- Create: `scripts/check_packaged_dependencies.ps1`
- Create: `docs/testing/samsung-acceptance-checklist.md`
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: release APK, 50,000-track fixture, and two target Samsung devices.
- Produces: repeatable startup/search measurements, release permission/dependency gates, and real-device acceptance record.

- [ ] **Step 1: Write the failing large-library and release checks**

Generate 50,000 deterministic catalog rows. Assert first search page completes under 100ms on the test host after warm-up and catalog query results are correct. The PowerShell manifest script extracts the merged manifest and fails on forbidden permissions; the dependency script fails on known network/ads/telemetry namespaces and prints the approved dependency set.

- [ ] **Step 2: Run checks and confirm missing benchmark/gates**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.LargeLibraryTest" :app:assembleRelease` then `./scripts/check_release_manifest.ps1`.

Expected: FAIL until fixtures, benchmark module, and scripts exist.

- [ ] **Step 3: Implement performance fixtures and release gates**

Measure warm indexed startup and representative title/artist/album/filename queries. Macrobenchmarks use API 33+ and report medians without hiding outliers. The Samsung checklist records scan throughput, background playback dropout, control latency, earbuds Previous/Next, screen lock, permission denial/revocation, USB backup visibility, same-device restore, and second-device relinking.

- [ ] **Step 4: Run complete automated verification**

Run:

```powershell
./gradlew.bat clean :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintRelease :app:assembleRelease
./scripts/check_release_manifest.ps1
./scripts/check_packaged_dependencies.ps1
git diff --check
git status --short
```

Expected: all commands exit 0; status contains only the intended Task 13 files before commit.

- [ ] **Step 5: Perform and record Samsung acceptance**

Install the signed debug/release candidate on both phones, execute every row in `docs/testing/samsung-acceptance-checklist.md`, and record device model, Android version, result, and notes. Any failure becomes a focused red-green fix before proceeding.

- [ ] **Step 6: Commit**

```bash
git add -- benchmark scripts docs/testing settings.gradle.kts app/src/test/java/com/javelinco/localmusicplayer/library/LargeLibraryTest.kt
git commit -m "test: add privacy and large-library release gates"
```

### Task 14: Documentation, signed APK, and GitHub publication

**Files:**
- Modify: `README.md`
- Create: `docs/permissions.md`
- Create: `docs/backup-format-v1.md`
- Create: `docs/installing-signed-apks.md`
- Create: `CHANGELOG.md`
- Create: `.gitignore`

**Interfaces:**
- Consumes: verified release candidate and acceptance results.
- Produces: auditable permission explanation, backup format documentation, installation/update instructions, release notes, and a GitHub-published V1 source state.

- [ ] **Step 1: Write documentation verification assertions**

Create `scripts/check_documentation_links.ps1` to resolve every repository-relative Markdown link in README and the V1 documents, then fail if any target is absent. Keep the canonical approved permission set in `docs/permissions.md` and have `check_release_manifest.ps1` emit that set in its successful report.

- [ ] **Step 2: Run and confirm failure**

Run: `./scripts/check_documentation_links.ps1`

Expected: FAIL until documentation files and matching allowlist exist.

- [ ] **Step 3: Write exact user and maintainer documentation**

Document all three source modes, why `READ_MEDIA_AUDIO` is optional, the absence of internet/all-files access, background and dedicated scans, shuffle/repeat rules, backup contents/retention/restore, signing-key preservation, USB installation, and known V1 limits. Do not claim V2 normalization or reactive visualization exists.

- [ ] **Step 4: Run final fresh verification**

Run the complete Task 13 command block again from a clean checkout. Verify the release APK can be installed as an update over the prior signed build without data loss.

Expected: all automated checks and both-device acceptance rows pass.

- [ ] **Step 5: Commit and publish**

```bash
git add -- README.md docs CHANGELOG.md .gitignore scripts/check_documentation_links.ps1
git commit -m "docs: prepare LocalMusicPlayer v1"
```

Publish the verified commits to `javelinco/LocalMusicPlayer` only after confirming the remote branch and repository. Do not publish the signing keystore, passwords, local SDK paths, device backups, MP3 fixtures that are not redistributable, or private user data.

## Plan self-review result

- Spec coverage: all approved V1 sections map to Tasks 1–14; V2 reactive visualization and loudness analysis remain explicitly excluded.
- Privacy coverage: manifest allowlist, runtime gating, dependency inspection, and user documentation are each independently tested.
- Type consistency: `SourceId`, `TrackId`, `MusicSource`, `SourceRegistry`, `ScanCoordinator`, `QueueCommand`, `QueueState`, `PlaylistRepository`, `BackupManager`, and `PlaybackController` are introduced before their consumers.
- Scope: one end-to-end plan as explicitly requested, while preserving reviewer-sized tasks and independent commits.

