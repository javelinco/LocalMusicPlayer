# Library-Centered UI Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the destination-heavy V1 interface with a bold, Library-centered UI that guides first-run setup into dedicated scanning, exits dedicated mode on completion, remembers the Library view, and provides contextual Home and recent playback.

**Architecture:** AppNavigation owns only Home, Library, and More. LibraryViewModel owns the remembered Library view, view-aware search, and scan session state; a focused ScanSessionManager makes first/later source policy and dedicated completion testable. A Room-backed RecentPlayRepository records bounded local track/playlist history without changing the portable backup format.

**Tech Stack:** Kotlin 2.3, Jetpack Compose Material 3, Room 2.8, DataStore Preferences, Media3 1.11, coroutines/Flow, JUnit/Robolectric, Compose instrumentation tests.

## Global Constraints

- Android only; `minSdk = 33`, `targetSdk = 37`.
- MP3 playback remains fully offline; do not add `INTERNET`, telemetry, advertising, or all-files access.
- Folder/file selection remains read-only through SAF; whole-device discovery remains optional and audio-only.
- Favorites persistence remains schema-compatible but Favorites must not appear in the UI.
- Do not add listening history to backup schema version 1.
- First source starts dedicated scanning immediately; later sources start background scanning immediately.
- Dedicated completion and failure both leave dedicated mode; intentional exit checkpoints first.
- Large-library lists remain lazy and search remains debounced.
- Both light and dark themes must retain accessible contrast and 48 dp icon targets.

---

### Task 1: Persist the selected Library view

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryView.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/settings/AppSettings.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/data/settings/AppSettingsTest.kt`

**Interfaces:**
- Produces: `enum class LibraryView { TRACKS, ARTISTS, ALBUMS, GENRES, PLAYLISTS }`
- Produces: `SettingsState.libraryView: LibraryView`
- Produces: `suspend fun AppSettings.setLibraryView(view: LibraryView)`

- [ ] **Step 1: Write the failing persistence test**

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppSettingsTest {
    @Test fun libraryViewDefaultsToTracksAndPersistsSelection() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = AppSettings(context)
        assertEquals(LibraryView.TRACKS, settings.state.first().libraryView)
        settings.setLibraryView(LibraryView.ALBUMS)
        assertEquals(LibraryView.ALBUMS, settings.state.first().libraryView)
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.AppSettingsTest"`

Expected: compilation fails because `LibraryView` and `libraryView` do not exist.

- [ ] **Step 3: Add the stable enum and DataStore key**

```kotlin
enum class LibraryView(val label: String) {
    TRACKS("Tracks"), ARTISTS("Artists"), ALBUMS("Albums"), GENRES("Genres"), PLAYLISTS("Playlists")
}

data class SettingsState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val reducedMotion: Boolean = false,
    val backupTreeUri: String? = null,
    val libraryView: LibraryView = LibraryView.TRACKS,
)
```

Decode saved values with `runCatching { LibraryView.valueOf(value) }.getOrNull() ?: LibraryView.TRACKS`, add `LAST_LIBRARY_VIEW = stringPreferencesKey("last_library_view")`, and write it from `setLibraryView`.

- [ ] **Step 4: Run the focused test and the settings consumers**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.AppSettingsTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryView.kt app/src/main/java/com/javelinco/localmusicplayer/data/settings/AppSettings.kt app/src/test/java/com/javelinco/localmusicplayer/data/settings/AppSettingsTest.kt
git commit -m "feat: remember selected library view"
```

### Task 2: Add bounded recent playback history

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/Entities.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/RecentPlayDao.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LocalMusicDatabase.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/data/db/DatabaseMigrations.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/home/RecentPlayRepository.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/AppContainer.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/home/RecentPlayRepositoryTest.kt`
- Create after Room processing: `app/schemas/com.javelinco.localmusicplayer.data.db.LocalMusicDatabase/2.json`

**Interfaces:**
- Produces: `RecentPlayKind.TRACK` and `RecentPlayKind.PLAYLIST`
- Produces: `RecentPlayRepository.recordTrack`, `recordPlaylist`, `observeRecentTracks`, and `observeRecentPlaylists`
- Produces: `DatabaseMigrations.MIGRATION_1_2`

- [ ] **Step 1: Write failing repository tests**

Test real in-memory Room behavior: recording the same track twice moves it to the front without duplication; only five valid items are exposed; unavailable tracks and deleted playlists disappear; track and playlist histories remain independent.

```kotlin
repository.recordTrack("one", 10)
repository.recordTrack("two", 20)
repository.recordTrack("one", 30)
assertEquals(listOf("one", "two"), repository.observeRecentTracks().first().map { it.trackId })
```

- [ ] **Step 2: Run and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.RecentPlayRepositoryTest"`

Expected: compilation fails because recent-play types do not exist.

- [ ] **Step 3: Add the entity and DAO**

```kotlin
@Entity(tableName = "recent_plays", primaryKeys = ["kind", "itemId"], indices = [Index("playedAtEpochMs")])
data class RecentPlayEntity(val kind: String, val itemId: String, val playedAtEpochMs: Long)
```

DAO writes use `@Upsert`; valid-track reads join `tracks` with `available = 1`; valid-playlist reads join `playlists` and count entries. After each write, delete rows beyond the newest 20 for that kind. Repository flows expose only the newest five.

- [ ] **Step 4: Add database version 2 and a non-destructive migration**

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `recent_plays` (`kind` TEXT NOT NULL, `itemId` TEXT NOT NULL, `playedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`kind`, `itemId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_plays_playedAtEpochMs` ON `recent_plays` (`playedAtEpochMs`)")
    }
}
```

Register the entity, expose `recentPlayDao()`, set database version 2, and add `.addMigrations(DatabaseMigrations.MIGRATION_1_2)` in AppContainer. Do not add recent history to `UserDataSnapshot` or backup models.

- [ ] **Step 5: Run focused tests and generate/check schema 2**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.RecentPlayRepositoryTest" :app:kspDebugKotlin`

Expected: PASS and schema `2.json` exists.

- [ ] **Step 6: Commit**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/db app/src/main/java/com/javelinco/localmusicplayer/home app/src/main/java/com/javelinco/localmusicplayer/AppContainer.kt app/src/test/java/com/javelinco/localmusicplayer/home app/schemas
git commit -m "feat: store bounded recent playback history"
```

### Task 3: Make scan policy and dedicated completion explicit

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/library/ScanSessionManager.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/library/ScanSessionManagerTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`

**Interfaces:**
- Produces: `ScanSessionManager.dedicated: StateFlow<Boolean>`
- Produces: `ScanSessionManager.message: StateFlow<String?>`
- Produces: `sourceAdded(wasFirstSource: Boolean, stopPlayback: () -> Unit)`
- Produces: `startBackground`, `startDedicated`, `prioritize`, and `leaveDedicated`

- [ ] **Step 1: Write failing scan-session tests with a real fake coordinator**

```kotlin
@Test fun firstSourceUsesDedicatedAndCompletionLeavesMode() = runTest {
    val coordinator = RecordingScanCoordinator(completeOnRun = true)
    val manager = ScanSessionManager(coordinator, backgroundScope)
    manager.sourceAdded(wasFirstSource = true) {}
    advanceUntilIdle()
    assertEquals(listOf(ScanExecutionMode.DEDICATED), coordinator.modes)
    assertFalse(manager.dedicated.value)
    assertTrue(manager.message.value!!.startsWith("Scan complete"))
}
```

Add separate tests for later-source background mode, Prioritize scan switching modes, failure exiting dedicated mode, and intentional exit calling `cancelAndCheckpoint`.

- [ ] **Step 2: Run and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.ScanSessionManagerTest"`

Expected: compilation fails because `ScanSessionManager` does not exist.

- [ ] **Step 3: Implement the minimal manager**

The manager owns one job. Dedicated execution sets `dedicated = true`, invokes `stopPlayback`, runs the coordinator, builds a summary only when progress is `COMPLETE`, and clears dedicated in `finally`. Failure stores `"Scan failed: <message>"`. Prioritization requests checkpoint cancellation, waits for the background job to finish, then starts dedicated mode.

```kotlin
private suspend fun runDedicated(stopPlayback: () -> Unit) {
    stopPlayback()
    mutableDedicated.value = true
    try {
        coordinator.run(ScanExecutionMode.DEDICATED)
        coordinator.progress.value?.takeIf { it.phase == ScanPhase.COMPLETE }?.let(::publishSummary)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        mutableMessage.value = "Scan failed: ${error.message ?: "unknown error"}"
    } finally {
        mutableDedicated.value = false
    }
}
```

- [ ] **Step 4: Delegate existing LibraryViewModel scan methods to the manager**

Keep public callbacks stable where possible, expose manager flows as `dedicated` and scan status, and add `onSourceAdded(wasFirstSource, stopPlayback)` plus `prioritizeScan(stopPlayback)`.

- [ ] **Step 5: Run focused and coordinator tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.ScanSessionManagerTest" --tests "*.ScanCoordinatorTest"`

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/library app/src/test/java/com/javelinco/localmusicplayer/library
git commit -m "feat: finish dedicated scans contextually"
```

### Task 4: Build remembered, view-aware Library state

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LibraryDao.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryRepository.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/library/LibrarySearchTest.kt`
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/data/db/LibraryDaoTest.kt`

**Interfaces:**
- Produces: `LibrarySearchResult` sealed values for tracks, named groups, albums, and playlists
- Produces: `selectLibraryView`, `openSearch`, `closeSearch`, and `searchInSelectedView`
- Consumes: `SettingsState.libraryView`

- [ ] **Step 1: Write failing DAO/repository tests for each view**

Seed multiple tracks and playlists, then assert that `"winter"` finds a track title only in Tracks, an artist only in Artists, an album only in Albums, a genre only in Genres, and a playlist name only in Playlists. Assert a blank query restores the full selected list.

- [ ] **Step 2: Run and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.LibrarySearchTest" --tests "*.LibraryDaoTest"`

Expected: failures because view-aware queries and state do not exist.

- [ ] **Step 3: Add indexed/grouped DAO queries**

Add observable artist/album/genre group queries and bounded `LIKE` searches on normalized columns. Track search continues using FTS. Playlist name search filters `playlists` from its Room flow.

- [ ] **Step 4: Implement debounced selected-view search**

```kotlin
fun selectLibraryView(view: LibraryView) {
    mutableLibraryView.value = view
    viewModelScope.launch { container.settings.setLibraryView(view) }
    search(query.value, view)
}
```

Use a 180 ms debounce job. Perform grouping/search work off the main thread and cap search results at 200. Do not change selection when search opens or closes.

- [ ] **Step 5: Run focused tests**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.LibrarySearchTest" --tests "*.LibraryDaoTest"`

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/db/LibraryDao.kt app/src/main/java/com/javelinco/localmusicplayer/library app/src/test/java/com/javelinco/localmusicplayer/library app/src/test/java/com/javelinco/localmusicplayer/data/db/LibraryDaoTest.kt
git commit -m "feat: search within remembered library views"
```

### Task 5: Consolidate navigation, Home, Sources, and Playlists

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt`
- Rewrite: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Rewrite: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Rewrite: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt` as Library tools content
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt`
- Remove: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SearchScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/playback/service/PlaybackViewModel.kt`
- Rewrite: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/NavigationUiTest.kt`

**Interfaces:**
- Top-level destinations are exactly `HOME`, `LIBRARY`, and `MORE`
- `HomeScreen` consumes active playback plus recent tracks/playlists
- `LibraryScreen` owns dropdown, search icon, Library tools icon, contextual scan status, and internal Playlists view
- Playback adds `playPlaylist(playlistId, orderedTracks)` and records recent history

- [ ] **Step 1: Write failing Compose navigation tests**

Assert Home/Library/More are visible and Search/Playlists/Sources are absent from bottom navigation. Assert no-history startup selects Tracks, the Library dropdown exposes all five views, the search icon has content description `Search Tracks`, and Library tools has content description `Library tools`.

- [ ] **Step 2: Write failing playback-history integration tests**

At the view-model/repository boundary, assert starting a track records the track and starting a playlist records both its playlist ID and the first playing track while preserving playlist entry order.

- [ ] **Step 3: Run and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*.RecentPlay*" :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest,com.javelinco.localmusicplayer.ui.NavigationUiTest'`

Expected: tests fail because the new navigation and Home do not exist.

- [ ] **Step 4: Implement top-level routing and contextual Home**

Initial route logic is: active playback → Home/Now Playing; else recent history → Home/Recently Played; else Library/Tracks. Home renders Now Playing only while `playback.isPlaying`; otherwise it renders the bounded recent lists.

- [ ] **Step 5: Implement consolidated Library**

Use a Material exposed dropdown for LibraryView, an icon button for search, and an icon button for Library tools. When sources are empty, replace list content with first-run source choices. When a background scan is active, show a compact status card with Prioritize scan. Render Playlists inside the same content area when selected.

- [ ] **Step 6: Wire first and later source behavior in MainActivity**

Before each source registration, read `app.sourceRegistry.observeSources().first().isEmpty()`. After successful registration call `libraryViewModel.onSourceAdded(wasFirstSource, playbackViewModel::stopForDedicatedScan)`. Apply the same rule to SAF folder, SAF documents, and accepted MediaStore permission.

- [ ] **Step 7: Add playlist playback and recent-history writes**

Resolve playlist entries to available tracks in their stored order. PlaybackViewModel records the selected track on play and the playlist when playback was initiated from a playlist. Deleted/unavailable items are filtered by the repository on Home.

- [ ] **Step 8: Run focused host and device tests**

Run the Step 3 command again.

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/main/java/com/javelinco/localmusicplayer/playback app/src/main/java/com/javelinco/localmusicplayer/ui app/src/androidTest/java/com/javelinco/localmusicplayer/ui app/src/test
git commit -m "feat: center navigation on home and library"
```

### Task 6: Apply the bold visual system and simplified playback controls

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Rewrite: `app/src/main/java/com/javelinco/localmusicplayer/ui/theme/AppTheme.kt`
- Rewrite: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/NowPlayingScreen.kt`
- Rewrite: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/MiniPlayer.kt`
- Remove: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/PlayingIndicator.kt`
- Rewrite: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/PlaybackUiTest.kt`

**Interfaces:**
- Material icons replace letter/text stand-ins
- Now Playing primary row is Previous, Play/Pause, Next
- Secondary row is Shuffle, Repeat, Queue
- No Favorite control and no level/equalizer graphic

- [ ] **Step 1: Rewrite the Compose test first**

Use content descriptions rather than visible text for transport icons. Assert Previous, Play/Pause, Next, Shuffle off/on, Repeat one/all/off, and Queue exist. Assert nodes labeled Favorite and Music is playing do not exist. Assert primary and secondary rows have stable test tags.

- [ ] **Step 2: Run and verify RED**

Run: `./gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.PlaybackUiTest'`

Expected: failure because old text buttons and Favorite/PlayingIndicator remain.

- [ ] **Step 3: Add Material icons and custom color schemes**

Add the Compose BOM-managed `androidx.compose.material:material-icons-extended` dependency. Define high-contrast light/dark schemes with saturated violet/blue primary accents, rounded Material 3 shapes, and no forced theme.

- [ ] **Step 4: Implement the two-row Now Playing controls**

```kotlin
Row(Modifier.fillMaxWidth().testTag("transport-controls"), horizontalArrangement = Arrangement.SpaceEvenly) {
    IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous") }
    FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) { /* Play or Pause */ }
    IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next") }
}
Row(Modifier.fillMaxWidth().testTag("playback-modes"), horizontalArrangement = Arrangement.SpaceEvenly) {
    ModeAction(/* Shuffle */)
    ModeAction(/* Repeat Off, Repeat All, or Repeat One */)
    ModeAction(/* Queue */)
}
```

Remove Favorite parameters/callbacks/markers from TrackList, MiniPlayer, NowPlaying, AppNavigation, MainActivity, and LibraryViewModel UI wiring. Keep repository and backup persistence methods intact.

- [ ] **Step 5: Run playback UI tests and release lint**

Run: `./gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.PlaybackUiTest' :app:lintRelease`

Expected: PASS with no accessibility lint failures.

- [ ] **Step 6: Commit**

```powershell
git add -- gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/javelinco/localmusicplayer app/src/androidTest/java/com/javelinco/localmusicplayer/ui/PlaybackUiTest.kt
git commit -m "feat: polish playback and library visuals"
```

### Task 7: Full verification, Samsung installation, and publication

**Files:**
- Modify: `docs/testing/samsung-acceptance-checklist.md`
- Modify if behavior copy changed: `README.md`
- Produce locally: `dist/LocalMusicPlayer-v0.1.0-development-signed.apk`

**Interfaces:**
- No new runtime interfaces
- Delivers a verified signed APK and published Git commit

- [ ] **Step 1: Run the complete host verification**

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
./scripts/check_release_manifest.ps1
./scripts/check_packaged_dependencies.ps1
./scripts/check_documentation_links.ps1
git diff --check
```

Expected: all commands PASS; manifest contains no Internet or all-files permission.

- [ ] **Step 2: Run all instrumentation tests on the attached Samsung**

Run: `./gradlew.bat :app:connectedDebugAndroidTest`

Expected: all tests pass on SM-S928U / Android 16. The harmless missing `androidx.test.services` appops setup message may appear, but the Gradle task must be successful.

- [ ] **Step 3: Update the Samsung acceptance record**

Record automatic dedicated completion, simplified navigation, first-run source state, remembered Library view, and two-row playback controls. Leave personal-library, earbuds, USB restore, and second-phone checks pending when they still require user interaction.

- [ ] **Step 4: Commit documentation and verify a clean worktree**

```powershell
git add -- README.md docs/testing/samsung-acceptance-checklist.md
git commit -m "docs: record refined Samsung interface checks"
git status --short
```

- [ ] **Step 5: Rebuild/sign or reuse the release signing workflow, install, and smoke-test**

Verify the APK with Android SDK `apksigner`, calculate SHA-256, install with `adb install -r`, revoke the test-only `READ_MEDIA_AUDIO` grant, cold-launch MainActivity, inspect the UI hierarchy, and confirm the crash log is empty.

- [ ] **Step 6: Publish only after verification**

Push the verified head to the already-approved public repository `javelinco/LocalMusicPlayer` main branch without force-pushing. Report commit, APK path/hash, device result, and remaining hands-on checks.
