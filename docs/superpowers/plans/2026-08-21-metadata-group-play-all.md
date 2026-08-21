# Metadata Group Play All Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a listener immediately play every track in an album, artist, or genre as an ordered queue, from either the library row or the group's track view.

**Architecture:** `LibraryScreen` will derive ordered group tracks from its observed library state and expose one ordered-list playback callback through `LibraryActions`. Artist, genre, and album rows will open a common track-detail experience and provide a direct Play all action; `MainActivity` will hand the chosen list to the existing playback queue entry point.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Media3 playback through the existing `PlaybackViewModel`, JUnit 4, AndroidX Compose UI tests, Gradle.

## Global Constraints

- Preserve album ordering by disc number, track number, and filename.
- Identify an album by both normalized album artist and normalized album title.
- Playing a group must replace the queue with exactly the displayed group tracks and begin at the first track.
- Keep one visible playback action on each group row; keep bulk playlist addition inside the detail page.
- Add no permission, internet access, dependency, database migration, or media-file write.
- Run computer-only verification; do not invoke connected-device tasks.

---

## File map

- `ui/library/LibraryScreen.kt`: group selection state, group-track derivation, album identity, and dispatch of ordered group playback.
- `ui/library/MetadataListScreen.kt`: artist and genre rows with open and Play all actions.
- `ui/library/AlbumListScreen.kt`: focused album row UI with composite identity, open, and Play all actions.
- `ui/library/MetadataDetailScreen.kt`: common album/artist/genre detail header and bulk actions.
- `MainActivity.kt`: connect ordered group playback to the existing playback queue.
- `MetadataGroupTracksTest.kt`: pure matching, ordering, and album-identity coverage.
- `LibraryUiTest.kt`: Compose interaction coverage for direct and detail playback.

### Task 1: Ordered group selection

**Files:**
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/ui/library/MetadataGroupTracksTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`

**Interfaces:**
- Consumes: `TrackEntity.normalizedArtist`, `normalizedGenre`, `normalizedAlbumArtist`, `normalizedAlbumTitle`, `discNumber`, `trackNumber`, and `fileName`.
- Produces: `tracksForMetadataGroup(view: LibraryView, normalizedName: String, tracks: List<TrackEntity>): List<TrackEntity>` and `tracksForAlbum(album: AlbumSummary, tracks: List<TrackEntity>): List<TrackEntity>`.

- [ ] **Step 1: Write failing album-selection tests**

Extend `MetadataGroupTracksTest` with tracks whose album title is shared by two album artists and whose input order does not match disc/track order:

```kotlin
@Test
fun albumMatchesCompositeIdentityAndUsesDiscTrackFilenameOrder() {
    val album = AlbumSummary("chosen artist", "shared", "Chosen Artist", "Shared", 3)
    val tracks = listOf(
        track("disc-two", albumArtist = "chosen artist", album = "shared", disc = 2, number = 1),
        track("other-artist", albumArtist = "other artist", album = "shared", disc = 1, number = 1),
        track("track-two", albumArtist = "chosen artist", album = "shared", disc = 1, number = 2),
        track("track-one", albumArtist = "chosen artist", album = "shared", disc = 1, number = 1),
    )

    assertEquals(
        listOf("track-one", "track-two", "disc-two"),
        tracksForAlbum(album, tracks).map(TrackEntity::trackId),
    )
}
```

Update the local `track` factory with explicit album, album-artist, disc, and track-number parameters.

- [ ] **Step 2: Run the focused unit test and verify RED**

Run:

```powershell
./gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.ui.library.MetadataGroupTracksTest"
```

Expected: compilation fails because `tracksForAlbum` does not exist.

- [ ] **Step 3: Implement exact album matching and deterministic ordering**

Add a pure helper in `LibraryScreen.kt`:

```kotlin
internal fun tracksForAlbum(
    album: AlbumSummary,
    tracks: List<TrackEntity>,
): List<TrackEntity> = tracks
    .filter {
        it.normalizedAlbumArtist == album.normalizedAlbumArtist &&
            it.normalizedAlbumTitle == album.normalizedAlbumTitle
    }
    .sortedWith(
        compareBy<TrackEntity> { it.discNumber ?: 1 }
            .thenBy { it.trackNumber ?: 0 }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.fileName },
    )
```

Retain existing artist and genre matching and ordering.

- [ ] **Step 4: Run the focused unit test and verify GREEN**

Run the Task 1 Gradle command again. Expected: all `MetadataGroupTracksTest` tests pass.

- [ ] **Step 5: Commit the ordered selection unit**

```powershell
git add app/src/test/java/com/javelinco/localmusicplayer/ui/library/MetadataGroupTracksTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt
git commit -m "feat: select ordered album tracks"
```

### Task 2: Group-row and detail Play all controls

**Files:**
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataListScreen.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/AlbumListScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataDetailScreen.kt`

**Interfaces:**
- Consumes: Task 1 group-selection helpers.
- Produces: `LibraryActions.onPlayTracks: (List<TrackEntity>) -> Unit`, `OpenedLibraryGroup` variants for named metadata and albums, `MetadataListScreen(..., onPlayAll)`, and `AlbumListScreen(..., onOpen, onPlayAll)`.

- [ ] **Step 1: Write failing Compose interaction tests**

Add tests to `LibraryUiTest` that construct two-track groups, capture `List<String>` from `LibraryActions(onPlayTracks = { played = it.map(TrackEntity::trackId) })`, and assert:

```kotlin
compose.onNodeWithContentDescription("Play all by Artist One").performClick()
compose.runOnIdle { assertEquals(listOf("one", "two"), played) }
```

Add equivalent direct-row assertions for `"Play all from Album One"` and `"Play all in Rock"`. Add one drill-in assertion that taps the group name, confirms its tracks, taps the written `"Play all"` button, and receives the same ordered IDs. Confirm the written `"Add all to playlist"` action remains visible in the detail view.

- [ ] **Step 2: Compile Android tests and verify RED**

Run:

```powershell
./gradlew.bat compileDebugAndroidTestKotlin
```

Expected: compilation fails because `LibraryActions.onPlayTracks` and the new UI actions do not exist.

- [ ] **Step 3: Add one ordered-list callback and a common opened-group model**

Add to `LibraryActions`:

```kotlin
val onPlayTracks: (List<TrackEntity>) -> Unit = {},
```

Represent open details without forcing album identity into `NamedGroupSummary`:

```kotlin
internal sealed interface OpenedLibraryGroup {
    val title: String
    val parentLabel: String

    data class Named(val view: LibraryView, val group: NamedGroupSummary) : OpenedLibraryGroup
    data class Album(val album: AlbumSummary) : OpenedLibraryGroup
}
```

Add a single `tracksForOpenedGroup(opened, tracks)` dispatcher and use it for detail display, direct playback, and playlist addition so those operations cannot disagree about membership or ordering.

- [ ] **Step 4: Implement clean rows with playback as the sole visible action**

Update `MetadataListScreen` to accept `onPlayAll: (NamedGroupSummary) -> Unit`. Replace the row-level playlist icon with a play icon whose content description depends on the selected view, passed in as a small label builder such as `"Play all by ${group.displayName}"` for artists and `"Play all in ${group.displayName}"` for genres. Keep the row body clickable to open tracks and retain the chevron.

Create `AlbumListScreen.kt` with `AlbumListScreen(albums, onOpen, onPlayAll)`. Each album row is keyed by both normalized album artist and title, opens on body tap, and has one play icon described as `"Play all from ${album.displayTitle}"`.

- [ ] **Step 5: Add Play all to the common detail view**

Change `MetadataDetailScreen` to accept `onPlayAll: () -> Unit`. Place a filled `Play all` button before an outlined or text-styled `Add all to playlist` button. Disable both when `tracks.isEmpty()`. Do not change track-level playback or actions.

- [ ] **Step 6: Route browse and search results through the same actions**

In `LibraryScreen`, use the group helpers for both normal library lists and search-result lists. Direct row playback must call `actions.onPlayTracks(matchingTracks)` only when the list is non-empty. Opening an album must show its details, and album detail playlist addition must use the same ordered track IDs.

- [ ] **Step 7: Compile Android tests and run unit tests for GREEN**

Run:

```powershell
./gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin
```

Expected: both tasks succeed.

- [ ] **Step 8: Commit the UI unit**

```powershell
git add app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataListScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/AlbumListScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/MetadataDetailScreen.kt
git commit -m "feat: play library groups directly"
```

### Task 3: Existing playback queue integration

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`

**Interfaces:**
- Consumes: `LibraryActions.onPlayTracks` from Task 2 and `PlaybackViewModel.play(track: TrackEntity, view: List<TrackEntity>)`.
- Produces: complete group-to-playback queue wiring.

- [ ] **Step 1: Wire non-empty group playback to the existing queue entry point**

Set the new action beside `onPlayTrack`:

```kotlin
onPlayTracks = { groupTracks ->
    groupTracks.firstOrNull()?.let { first ->
        playbackViewModel.play(first, groupTracks)
    }
},
```

This uses the existing queue replacement and history behavior rather than adding a second playback path.

- [ ] **Step 2: Run focused computer-only verification**

Run:

```powershell
./gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
```

Expected: all tasks succeed.

- [ ] **Step 3: Commit playback wiring**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt
git commit -m "feat: queue all tracks from library groups"
```

### Task 4: Full verification and integration

**Files:**
- Verify all modified files from Tasks 1-3.

**Interfaces:**
- Consumes: complete feature branch.
- Produces: a reviewed, computer-verified main branch ready to push.

- [ ] **Step 1: Review the complete diff against the design**

Confirm album composite identity, exact displayed queue membership, disabled empty actions, search parity, clear content descriptions, and absence of new permissions/dependencies. Run `git diff --check`.

- [ ] **Step 2: Run the full computer-only verification suite**

```powershell
./gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

Expected: every task succeeds. Do not run `connectedDebugAndroidTest` or any `adb` command.

- [ ] **Step 3: Merge using the repository's established fast-forward workflow**

Verify main has only the pre-existing unstaged `NavigationUiTest.kt` change, fast-forward the feature branch into main, and rerun the full verification command on merged main.

- [ ] **Step 4: Push public main and verify synchronization**

Push `main` to `origin`, then confirm local `HEAD` equals `origin/main` and the pre-existing unstaged `NavigationUiTest.kt` change remains untouched.
