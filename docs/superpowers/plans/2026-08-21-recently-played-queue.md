# Recently Played Queue Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Play the displayed Recently Played songs as a navigable queue and let users remove track or playlist entries from recent history without changing music, playlists, or active playback.

**Architecture:** A small immutable `RecentPlaybackQueue` converts a selected recent track and the displayed snapshot into the existing `PlaybackViewModel.play(track, view)` contract. Room owns persisted history deletion through explicit repository methods, while Home adds contextual written removal actions and forwards callbacks through navigation to `LibraryViewModel`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room, Media3, Kotlin coroutines/Flow, JUnit 4, Robolectric, Android Compose UI tests.

## Global Constraints

- Android only; retain the current Android SDK requirements.
- MP3 files, playlist contents, and the active playback queue must never be changed by recent-history removal.
- No new permissions, network access, telemetry, or dependencies.
- The displayed recent-song order is captured when playback starts; later history updates do not mutate that active queue.
- Do not run connected tests on the user's phone.

---

### Task 1: Persisted Recent-History Removal

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/RecentPlayDao.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/home/RecentPlayRepository.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`
- Test: `app/src/test/java/com/javelinco/localmusicplayer/home/RecentPlayRepositoryTest.kt`

**Interfaces:**
- Produces: `RecentPlayDao.remove(kind: String, itemId: String)`.
- Produces: `RecentPlayRepository.removeTrack(trackId: String)` and `removePlaylist(playlistId: String)`.
- Produces: `LibraryViewModel.removeRecentTrack(trackId: String)` and `removeRecentPlaylist(playlistId: String)`.

- [ ] **Step 1: Write failing repository tests**

Add tests that create underlying track and playlist records, record both kinds of history, call the requested removal method, and assert only the matching recent entry disappears while the underlying track or playlist still exists through its normal DAO observation.

```kotlin
@Test
fun removingTrackHistoryKeepsTrackAndPlaylistHistory() = runTest {
    database.libraryDao().applyScanBatch(ScanBatch(listOf(track("song")), checkpoint()))
    database.userDataDao().upsertPlaylist(PlaylistEntity("mix", "Mix", 1, 1))
    repository.recordTrack("song", 10)
    repository.recordPlaylist("mix", 20)

    repository.removeTrack("song")

    assertEquals(emptyList<TrackEntity>(), repository.observeRecentTracks().first())
    assertEquals(listOf("mix"), repository.observeRecentPlaylists().first().map { it.playlistId })
    assertEquals(listOf("song"), database.libraryDao().observeAvailableTracks().first().map { it.trackId })
}

@Test
fun removingPlaylistHistoryKeepsPlaylistAndTrackHistory() = runTest {
    database.libraryDao().applyScanBatch(ScanBatch(listOf(track("song")), checkpoint()))
    database.userDataDao().upsertPlaylist(PlaylistEntity("mix", "Mix", 1, 1))
    repository.recordTrack("song", 10)
    repository.recordPlaylist("mix", 20)

    repository.removePlaylist("mix")

    assertEquals(listOf("song"), repository.observeRecentTracks().first().map { it.trackId })
    assertEquals(emptyList<RecentPlaylistRow>(), repository.observeRecentPlaylists().first())
    assertEquals(listOf("mix"), database.userDataDao().observePlaylists().first().map { it.playlistId })
}
```

- [ ] **Step 2: Run the focused repository tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.home.RecentPlayRepositoryTest"
```

Expected: compilation fails because `removeTrack` and `removePlaylist` do not exist.

- [ ] **Step 3: Add the minimal delete path**

Add the keyed Room query and repository wrappers:

```kotlin
@Query("DELETE FROM recent_plays WHERE kind = :kind AND itemId = :itemId")
suspend fun remove(kind: String, itemId: String)

suspend fun removeTrack(trackId: String) = dao.remove(RecentPlayKind.TRACK.name, trackId)
suspend fun removePlaylist(playlistId: String) = dao.remove(RecentPlayKind.PLAYLIST.name, playlistId)
```

Expose view-model launchers:

```kotlin
fun removeRecentTrack(trackId: String) {
    viewModelScope.launch { container.recentPlayRepository.removeTrack(trackId) }
}

fun removeRecentPlaylist(playlistId: String) {
    viewModelScope.launch { container.recentPlayRepository.removePlaylist(playlistId) }
}
```

- [ ] **Step 4: Run the focused repository tests and verify GREEN**

Run the Step 2 command. Expected: all `RecentPlayRepositoryTest` tests pass.

- [ ] **Step 5: Commit the persistence slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/data/db/RecentPlayDao.kt app/src/main/java/com/javelinco/localmusicplayer/home/RecentPlayRepository.kt app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt app/src/test/java/com/javelinco/localmusicplayer/home/RecentPlayRepositoryTest.kt
git commit -m "feat: remove recently played entries"
```

### Task 2: Recent-Song Queue Snapshot

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/home/RecentPlaybackQueue.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/home/RecentPlaybackQueueTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`

**Interfaces:**
- Produces: `RecentPlaybackQueue(selected: TrackEntity, tracks: List<TrackEntity>)`.
- Produces: `recentPlaybackQueue(selectedTrackId: String, displayedTracks: List<TrackEntity>): RecentPlaybackQueue?`.
- Changes: `HomeScreen` receives `onPlayRecentQueue: (RecentPlaybackQueue) -> Unit`.
- Changes: `AppNavigation` receives and forwards `onPlayRecentQueue: (RecentPlaybackQueue) -> Unit`.

- [ ] **Step 1: Write the failing queue snapshot test**

```kotlin
@Test
fun selectedTrackKeepsTheCompleteDisplayedOrder() {
    val first = track("first")
    val selected = track("selected")
    val last = track("last")

    val queue = recentPlaybackQueue(selected.trackId, listOf(first, selected, last))

    assertEquals("selected", queue?.selected?.trackId)
    assertEquals(listOf("first", "selected", "last"), queue?.tracks?.map { it.trackId })
}

@Test
fun missingSelectionDoesNotStartAQueue() {
    assertNull(recentPlaybackQueue("missing", listOf(track("present"))))
}
```

- [ ] **Step 2: Run the focused queue test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.home.RecentPlaybackQueueTest"
```

Expected: compilation fails because `recentPlaybackQueue` does not exist.

- [ ] **Step 3: Implement the immutable snapshot**

```kotlin
data class RecentPlaybackQueue(
    val selected: TrackEntity,
    val tracks: List<TrackEntity>,
)

fun recentPlaybackQueue(
    selectedTrackId: String,
    displayedTracks: List<TrackEntity>,
): RecentPlaybackQueue? {
    val selected = displayedTracks.find { it.trackId == selectedTrackId } ?: return null
    return RecentPlaybackQueue(selected, displayedTracks.toList())
}
```

Use this snapshot for both row taps and Home's `Play now` menu action. In `MainActivity`, consume it with:

```kotlin
onPlayRecentQueue = { queue -> playbackViewModel.play(queue.selected, queue.tracks) }
```

- [ ] **Step 4: Run the focused queue test and verify GREEN**

Run the Step 2 command. Expected: both queue snapshot tests pass.

- [ ] **Step 5: Compile the Android UI tests**

Run:

```powershell
.\gradlew.bat compileDebugAndroidTestKotlin
```

Expected: compilation succeeds with the new callback threaded through existing navigation tests using a default no-op parameter or explicit no-op callbacks.

- [ ] **Step 6: Commit the queue slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/home/RecentPlaybackQueue.kt app/src/test/java/com/javelinco/localmusicplayer/home/RecentPlaybackQueueTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt
git commit -m "feat: play recently played as a queue"
```

### Task 3: Contextual Removal Menus

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/TrackActions.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/RecentlyPlayedUiTest.kt`

**Interfaces:**
- Changes: `TrackActionMenu` accepts optional `onRemoveFromRecentlyPlayed: ((TrackEntity) -> Unit)? = null`.
- Changes: `HomeScreen` receives `onRemoveRecentTrack: (String) -> Unit` and `onRemoveRecentPlaylist: (String) -> Unit`.
- Changes: `AppNavigation` receives and forwards both history-removal callbacks.

- [ ] **Step 1: Write the failing Compose UI tests**

Render `HomeScreen` with one song and one playlist. Open each three-dot menu, click the written action, and assert the correct identifier is captured:

```kotlin
compose.onNodeWithContentDescription("More actions for Recent song").performClick()
compose.onNodeWithText("Remove from recently played").performClick()
compose.runOnIdle { assertEquals("song", removedTrackId) }

compose.onNodeWithContentDescription("More actions for Recent mix").performClick()
compose.onNodeWithText("Remove from recently played").performClick()
compose.runOnIdle { assertEquals("mix", removedPlaylistId) }
```

Also tap the song card and assert the callback receives the selected song plus every displayed recent song in order.

- [ ] **Step 2: Compile the focused UI test and verify RED**

Run:

```powershell
.\gradlew.bat compileDebugAndroidTestKotlin
```

Expected: compilation fails because the new Home removal callbacks and contextual track-menu parameter do not exist.

- [ ] **Step 3: Add written contextual removal actions**

Append this item only when the optional callback is supplied:

```kotlin
onRemoveFromRecentlyPlayed?.let { remove ->
    DropdownMenuItem(
        text = { Text("Remove from recently played") },
        onClick = { expanded = false; remove(track) },
    )
}
```

Add a Home-only playlist overflow menu containing the same written action. Wire song and playlist IDs through `AppNavigation` to `LibraryViewModel.removeRecentTrack` and `removeRecentPlaylist` in `MainActivity`.

- [ ] **Step 4: Compile Android tests and run JVM tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin
```

Expected: build succeeds and all JVM tests pass. Do not execute connected tests on the user's phone.

- [ ] **Step 5: Commit the UI slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui/library/TrackActions.kt app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui/RecentlyPlayedUiTest.kt
git commit -m "feat: remove items from recently played"
```

### Task 4: Full Computer-Only Verification

**Files:**
- Verify all modified files.

**Interfaces:**
- Consumes: all production and test interfaces from Tasks 1-3.
- Produces: a verified debug APK and a clean feature branch ready for integration.

- [ ] **Step 1: Run the full verification command**

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

Expected: `BUILD SUCCESSFUL`; no phone or connected-device command is used.

- [ ] **Step 2: Inspect the final diff and repository status**

```powershell
git diff --check
git status --short
git log --oneline --decorate -8
```

Expected: no uncommitted feature changes and no whitespace errors.
