# Recently Played Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace visually undivided Recently Played rows with readable song and playlist cards while preserving queue and menu behavior.

**Architecture:** Keep `HomeScreen` responsible for section ordering and queue construction. Move item presentation into focused `RecentTrackCard`, `RecentPlaylistCard`, and `RecentSectionHeader` composables that consume the existing database models and callbacks without adding state or dependencies.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Compose UI tests.

## Global Constraints

- Preserve recent-song queue order and the selected starting track.
- Preserve play, track actions, playlist play, and remove-from-recents behavior.
- Support existing dark and light Material themes.
- Do not add artwork indexing, permissions, dependencies, telemetry, or internet access.
- Do not modify the user-owned unstaged `NavigationUiTest.kt` in the main checkout.
- Do not execute connected instrumentation tests on the user's phone.

---

### Task 1: Card presentation and interaction coverage

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/RecentItemCards.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/RecentlyPlayedUiTest.kt`

**Interfaces:**
- Consumes: `TrackEntity`, `RecentPlaylistRow`, `TrackActionCallbacks`, and existing play/remove callbacks.
- Produces: `RecentSectionHeader(title: String, count: Int)`, `RecentTrackCard(track, actions, onPlay, onRemoveFromRecentlyPlayed)`, and `RecentPlaylistCard(playlist, onPlay, onRemoveFromRecentlyPlayed)`.

- [ ] **Step 1: Add a failing card-structure test**

Extend `RecentlyPlayedUiTest` with two tracks and one playlist. Import the not-yet-created `RECENT_TRACK_CARD_TAG` and `RECENT_PLAYLIST_CARD_TAG` production constants. Assert two track-card nodes, one playlist-card node, the section count labels `2` and `1`, and visible title, artist, album, playlist name, and track count text.

- [ ] **Step 2: Run the UI-test compilation and verify the intended failure**

Run:

```powershell
./gradlew compileDebugAndroidTestKotlin
```

Expected: compilation fails with unresolved references to `RECENT_TRACK_CARD_TAG` and `RECENT_PLAYLIST_CARD_TAG`.

- [ ] **Step 3: Implement the reusable card composables**

Create `internal const val RECENT_TRACK_CARD_TAG = "recent-track-card"` and `internal const val RECENT_PLAYLIST_CARD_TAG = "recent-playlist-card"`. Create full-width clickable Material cards using `surfaceContainerLow`, 16 dp rounded corners, subtle elevation, 12 dp internal spacing, and a 48 dp primary-container icon tile. Use `MusicNote` for songs and `QueueMusic` for playlists. Render title with `titleMedium` and semibold weight, then artist and album or track-count support text with `onSurfaceVariant`. Apply the corresponding tag constant to each outer card.

- [ ] **Step 4: Replace Home list rows with spaced cards and section headers**

Use `LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp))`. Add a section header with title and count before each non-empty section. Keep stable item keys, pass `::playRecent` to every track card, and pass the unchanged playlist callback to playlist cards.

- [ ] **Step 5: Verify focused UI behavior compiles and existing JVM behavior passes**

Run:

```powershell
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin
```

Expected: all unit tests pass and all Android UI tests compile. Do not execute the connected suite.

- [ ] **Step 6: Commit the card implementation**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/ui/home app/src/androidTest/java/com/javelinco/localmusicplayer/ui/RecentlyPlayedUiTest.kt
git commit -m "feat: redesign recently played with cards"
```

### Task 2: Verify and integrate

**Files:**
- Verify the implementation, UI test, design, and plan files from Task 1.

**Interfaces:**
- Consumes: the completed card implementation.
- Produces: a verified and pushed `main` branch; phone installation remains a separate explicit follow-up because the phone is disconnected.

- [ ] **Step 1: Run complete computer-only verification**

Run:

```powershell
./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin
```

Expected: build succeeds with no test or lint failures.

- [ ] **Step 2: Commit this plan and inspect the complete branch diff**

Confirm only planned Home UI, UI-test, design, and plan files changed and that `git diff --check` reports no whitespace errors.

- [ ] **Step 3: Fast-forward main and repeat verification**

Preserve the unstaged main-checkout `NavigationUiTest.kt`, fast-forward `main`, and repeat the complete computer-only command on the merged tree.

- [ ] **Step 4: Push the verified main branch**

Push `main` to `origin` and report the commit. Do not attempt a phone install while the phone is disconnected.
