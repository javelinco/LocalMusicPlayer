# Library Drill-In and Playlist Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users drill into artists, genres, and playlists; add one track or a whole artist/genre to a playlist; and dismiss scan-result banners.

**Architecture:** Keep all collection detail state inside `LibraryScreen` rather than adding primary navigation destinations. Reuse the existing observed track snapshot and playlist repository, add one shared playlist picker, and expose one bulk-add callback plus one scan-dismiss callback through the existing `LibraryActions` boundary.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, StateFlow, Media3-era Android architecture, JUnit, coroutine tests, Compose instrumentation tests, Gradle, ADB.

## Global Constraints

- Keep Home/Library/More primary navigation unchanged.
- Filter artist and genre details from the already-observed available-track snapshot using exact normalized values.
- Preserve current Library order and existing duplicate playlist-entry behavior.
- Keep track-card body taps as play; playlist-add controls must not start playback.
- Apply scan dismissal to successful and failed result messages and allow later scans to publish new messages.
- Do not add permissions, dependencies, network access, database migrations, entities, persistent settings, artwork, or playback behavior.
- Preserve package `com.javelinco.localmusicplayer`, label `Music, Please!`, and backup prefix `LocalMusicPlayer-`.

---

### Task 1: Make scan-result messages dismissible

**Files:**
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/library/ScanSessionManagerTest.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/ScanSessionManager.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`

**Interfaces:**
- Produces: `ScanSessionManager.dismissMessage()`, `LibraryViewModel.dismissScanMessage()`, and `LibraryActions.onDismissScanMessage: () -> Unit`.

- [ ] **Step 1: Add failing state and UI tests**

Add this coroutine test:

```kotlin
@Test
fun completionMessageCanBeDismissed() = runTest {
    val manager = ScanSessionManager(RecordingScanCoordinator(), this)
    manager.startBackground()
    advanceUntilIdle()

    assertEquals("Scan complete · 7 indexed · 1 skipped · 0 errors", manager.message.value)
    manager.dismissMessage()

    assertNull(manager.message.value)
}
```

Add `assertNull` import. Add a Compose test that renders `LibraryScreenState(scanMessage = "Scan complete", sources = listOf(source()))`, clicks `Dismiss scan result`, and asserts the supplied `onDismissScanMessage` callback ran. Add a test helper:

```kotlin
private fun source() = SafTreeSource(SourceId("source"), "content://music", "Music")
```

- [ ] **Step 2: Run the tests and verify RED**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.javelinco.localmusicplayer.library.ScanSessionManagerTest
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest#scanResultCanBeDismissed'
```

Expected: compilation fails because the dismiss state method/action does not exist.

- [ ] **Step 3: Implement state clearing and the banner**

Add to `ScanSessionManager`:

```kotlin
fun dismissMessage() {
    mutableMessage.value = null
}
```

Delegate from `LibraryViewModel`:

```kotlin
fun dismissScanMessage() {
    scanSession.dismissMessage()
}
```

Add `onDismissScanMessage` to `LibraryActions` and wire it in `MainActivity`. Replace the plain scan-message `Text` with a full-width `Card` containing a weighted message and an `IconButton` using `Icons.Rounded.Close` with content description `Dismiss scan result`.

- [ ] **Step 4: Rerun the focused tests and verify GREEN**

Run both Step 2 commands. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/javelinco/localmusicplayer/library/ScanSessionManagerTest.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt app/src/main/java/com/javelinco/localmusicplayer/library/ScanSessionManager.kt app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt
git commit -m "feat: dismiss scan result banners"
```

### Task 2: Add tracks directly through a shared playlist picker

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistPickerDialog.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/LibraryViewModel.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`

**Interfaces:**
- Produces: `LibraryActions.onAddTracksToPlaylist: (String, List<String>) -> Unit`, `PendingPlaylistAddition(label: String, trackIds: List<String>)`, and `PlaylistPickerDialog`.
- Changes: `TrackList` gains optional `onAddToPlaylist: ((TrackEntity) -> Unit)? = null`.

- [ ] **Step 1: Add failing track-add and empty-picker tests**

Add a Compose test that supplies one track, one playlist, and one source to `LibraryScreen`, clicks `Add First track to playlist`, chooses `Road Mix`, then asserts the callback received playlist `mix` and IDs `listOf("one")` while the play callback remained untouched.

Add a second test with no playlists that clicks the same add icon, verifies `Create a playlist first`, clicks `Go to playlists`, and asserts `onSelectView(LibraryView.PLAYLISTS)` ran.

- [ ] **Step 2: Run the two focused tests and verify RED**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest'
```

Expected: FAIL because the track add control and playlist picker do not exist.

- [ ] **Step 3: Implement bulk action plumbing**

Replace the single-track action in `LibraryActions` with:

```kotlin
val onAddTracksToPlaylist: (String, List<String>) -> Unit = { _, _ -> },
```

Replace `LibraryViewModel.addTrackToPlaylist` with:

```kotlin
fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) {
    viewModelScope.launch {
        container.playlistRepository.addTracks(
            PlaylistId(playlistId),
            trackIds.map(::TrackId),
        )
    }
}
```

Wire `MainActivity` to `libraryViewModel::addTracksToPlaylist`. Adapt `PlaylistScreen`'s existing single-track add button to call the bulk callback with `listOf(track.trackId)`.

- [ ] **Step 4: Implement the shared picker**

Create:

```kotlin
internal data class PendingPlaylistAddition(
    val label: String,
    val trackIds: List<String>,
)
```

`PlaylistPickerDialog` uses `AlertDialog`. Its title is `Add <label> to playlist`. With playlists it displays a height-limited `LazyColumn` of clickable playlist `ListItem`s showing name and track count. With none it shows `Create a playlist first.` and a `Go to playlists` button. It always supports Cancel/dismiss.

In `LibraryScreen`, remember `PendingPlaylistAddition?`, show the dialog when non-null, call `actions.onAddTracksToPlaylist(selectedPlaylistId, pending.trackIds)` on selection, and clear pending state.

- [ ] **Step 5: Add the direct track control**

Extend `TrackList` with the optional callback. Inside each card use a `Row`; keep metadata in a weighted `Column` and, when the callback is present, render an `IconButton` with `Icons.Rounded.PlaylistAdd`, content description `Add <visible title> to playlist`, and `onClick = { onAddToPlaylist(track) }`. The surrounding card continues to call only `onPlay(track)`.

Pass the callback from Library track browsing and track-search results so it creates `PendingPlaylistAddition(track title, listOf(track.trackId))`.

- [ ] **Step 6: Rerun tests, host checks, and commit**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest'
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
git diff --check
git add app/src/main app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt
git commit -m "feat: add tracks from the library to playlists"
```

### Task 3: Drill into artists and genres and bulk-add their tracks

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataDetailScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataListScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`

**Interfaces:**
- Produces: `OpenedMetadataGroup(view: LibraryView, group: NamedGroupSummary)` and `tracksForMetadataGroup(view, normalizedName, tracks)`.
- Consumes: the Task 2 pending-addition state and `TrackList` callback.

- [ ] **Step 1: Add failing artist and genre tests**

Add one Compose test for Artists and one for Genres. Each test provides two groups and tracks from both groups, taps the requested group, verifies only matching tracks appear, clicks `Add all to playlist`, chooses `Road Mix`, and asserts the bulk callback receives the matching IDs in Library order.

- [ ] **Step 2: Run the Library UI class and verify RED**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest'
```

Expected: FAIL because metadata rows do not open or bulk-add.

- [ ] **Step 3: Implement exact group selection**

Add:

```kotlin
internal fun tracksForMetadataGroup(
    view: LibraryView,
    normalizedName: String,
    tracks: List<TrackEntity>,
): List<TrackEntity> = when (view) {
    LibraryView.ARTISTS -> tracks.filter { it.normalizedArtist == normalizedName }
    LibraryView.GENRES -> tracks.filter { it.normalizedGenre == normalizedName }
    else -> emptyList()
}
```

Remember `OpenedMetadataGroup?` in `LibraryScreen`, keyed by `state.selectedView`. Memoize the matching tracks with `remember(state.tracks, openedGroup)`.

- [ ] **Step 4: Implement interactive group rows and detail**

Change `MetadataListScreen` to accept `onOpen: (NamedGroupSummary) -> Unit` and `onAddToPlaylist: (NamedGroupSummary) -> Unit`. Each `ListItem` is full-row clickable, shows a trailing playlist-add `IconButton`, and a decorative `ChevronRight` icon.

Create `MetadataDetailScreen` with an ArrowBack control, group name, `<count> tracks`, `Add all to playlist` button enabled only when tracks are non-empty, and `TrackList` with both play and per-track add callbacks.

Use the same functions for normal Artist/Genre lists and named-group search results. Group-row and detail-header bulk actions create the same pending addition using matching ordered track IDs.

- [ ] **Step 5: Rerun tests and commit**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest'
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
git diff --check
git add app/src/main/java/com/javelinco/localmusicplayer/ui/library app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt
git commit -m "feat: browse and add artist and genre tracks"
```

### Task 4: Make playlist drill-in unmistakable

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`

**Interfaces:**
- Keeps: existing local playlist selection and ordered-entry editing behavior.
- Adds: `Open <playlist name>` semantics on the forward affordance and a visible selected-playlist heading.

- [ ] **Step 1: Add a failing playlist affordance test**

Render `PlaylistScreen` with playlist `Road Mix`, one `PlaylistEntryEntity`, and its track. Assert `Open Road Mix` is displayed, tap `Road Mix`, and assert the selected detail displays heading `Road Mix`, `Playlist order`, and the track title.

- [ ] **Step 2: Run the focused test and verify RED**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest#playlistRowsClearlyOpenTheirTracks'
```

Expected: FAIL because `Open Road Mix` semantics do not exist.

- [ ] **Step 3: Add the affordance and heading**

Add `trailingContent = { Icon(Icons.Rounded.ChevronRight, "Open ${playlist.name}") }` to playlist rows. In selected detail, render `Text(selected.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)` directly below Back.

- [ ] **Step 4: Verify and commit**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest#playlistRowsClearlyOpenTheirTracks'
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
git diff --check
git add app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt
git commit -m "feat: clarify playlist drill-in"
```

### Task 5: Verify, document, sign, install, and publish

**Files:**
- Modify: `docs/testing/samsung-acceptance-checklist.md`
- Generate: `dist/Music-Please-v0.1.0-development-signed.apk` (ignored artifact)

**Interfaces:**
- Produces: recorded Samsung evidence, signed update APK, and published `main`.

- [ ] **Step 1: Run complete verification**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
.\scripts\check_release_manifest.ps1
.\scripts\check_packaged_dependencies.ps1
.\scripts\check_documentation_links.ps1
.\gradlew.bat :app:connectedDebugAndroidTest
git diff --check
```

Expected: all host tasks, all instrumentation tests, and all privacy/documentation gates pass.

- [ ] **Step 2: Record and commit device evidence**

Update only the full instrumentation row in `docs/testing/samsung-acceptance-checklist.md` with the new total and explicit artist/genre/playlist drill-in, direct/bulk playlist addition, and dismissible scan-result coverage. Preserve pending hands-on rows.

```powershell
git add docs/testing/samsung-acceptance-checklist.md
git commit -m "docs: record library action verification"
```

- [ ] **Step 3: Sign and verify the APK**

Sign the release APK with build-tools 37.0.0 and the existing debug keystore. Verify certificate SHA-256 `724d17783107e9393423bf1032620665ef074706cfc4e77ae5088aa24ed6c942`, package `com.javelinco.localmusicplayer`, label `Music, Please!`, and record APK SHA-256.

- [ ] **Step 4: Verify the Samsung update**

Install the prior signed APK, record `firstInstallTime`, install the new APK with `adb install -r`, revoke `READ_MEDIA_AUDIO`, cold-launch `.MainActivity`, and confirm unchanged install time and an empty crash buffer.

- [ ] **Step 5: Integrate the already-selected path**

Fast-forward the feature branch into local `main`, copy the signed APK into the main checkout, rerun unit/lint/release checks and privacy gates on merged `main`, remove only the owned feature worktree and merged branch, push `main`, and verify remote `main` equals local `HEAD`.

- [ ] **Step 6: Final audit**

Confirm clean `main`, removed feature branch, matching local/remote SHA, unchanged APK identity/certificate, denied audio permission, and empty Samsung crash log.
