# Consistent Screen Headers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every full-screen destination the same compact `Music, Please!` identity and accurate screen title.

**Architecture:** Add one shared Compose header and one pure destination-title mapping. Render the shared header in the normal navigation shell and directly in dedicated scan mode, then remove duplicate destination-level headings from individual screens.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit, Android Compose UI tests.

## Global Constraints

- Preserve all navigation and playback behavior.
- Do not add permissions, dependencies, telemetry, or internet access.
- Do not modify the user-owned unstaged `NavigationUiTest.kt` in the main checkout.
- Do not execute connected instrumentation tests on the phone.

---

### Task 1: Shared header and destination titles

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/components/AppScreenHeader.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/NavigationHistory.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/ui/navigation/AppScreenHeaderTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/AppScreenHeaderUiTest.kt`

**Interfaces:**
- Produces: `AppScreenHeader(title: String, modifier: Modifier = Modifier)` and `screenHeaderTitle(destination: Destination, homeIsPlaying: Boolean): String`.
- Consumes: the current navigation destination and `PlaybackUiState.isPlaying`.

- [ ] **Step 1: Write failing title-mapping and Compose tests**

The JVM test asserts all eight destination titles and both Home states. The Compose test renders `AppScreenHeader("Queue")` and asserts that `Music, Please!` and `Queue` are displayed.

- [ ] **Step 2: Verify the tests fail for missing production APIs**

Run:

```powershell
./gradlew testDebugUnitTest --tests '*AppScreenHeaderTest' compileDebugAndroidTestKotlin
```

Expected: compilation fails because `AppScreenHeader` and `screenHeaderTitle` do not exist.

- [ ] **Step 3: Add the shared header and render it centrally**

Implement the two-line Material-themed header. Add the pure title mapping beside `Destination`. In `AppNavigation`, render it before the current destination; use `homeIsPlaying` only for `Destination.HOME`. Render the same component in `DedicatedScanScreen` with `Dedicated scanning`.

- [ ] **Step 4: Verify focused tests pass**

Run the command from Step 2. Expected: the unit test passes and Android tests compile.

### Task 2: Remove duplicate destination headings

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/DedicatedScanScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/player/NowPlayingScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`

**Interfaces:**
- Consumes: the navigation-level `AppScreenHeader`.
- Produces: screen content without repeated app or destination titles.

- [ ] **Step 1: Remove only headings replaced by the shared header**

Remove `Recently played`, `Now playing`, `Library`, `More`, `Music folders and scanning`, `Appearance`, and the backup/dedicated one-off identity blocks. Keep content-level artist, album, genre, playlist, source, and backup-step headings.

- [ ] **Step 2: Run all unit tests and compile all Android tests**

Run:

```powershell
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin
```

Expected: all unit tests pass and all Android tests compile.

- [ ] **Step 3: Commit the implementation**

```powershell
git add app/src/main app/src/test app/src/androidTest/java/com/javelinco/localmusicplayer/ui/AppScreenHeaderUiTest.kt
git commit -m "feat: add consistent screen headers"
```

### Task 3: Verify, integrate, install, and scan

**Files:**
- Verify all implementation, test, design, and plan files.

**Interfaces:**
- Consumes: Tasks 1 and 2 plus the already-merged scan reconciliation fix.
- Produces: a pushed main branch and one data-preserving phone update with verified header and scan behavior.

- [ ] **Step 1: Run complete computer-only verification**

Run:

```powershell
./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin
```

Expected: build succeeds with no test or lint failures.

- [ ] **Step 2: Commit this plan and inspect the branch diff**

Confirm only planned files changed and the worktree is clean after committing documentation.

- [ ] **Step 3: Fast-forward main, verify again, and push**

Preserve the unstaged main-checkout `NavigationUiTest.kt`, repeat complete verification on the merged tree, and push `main`.

- [ ] **Step 4: Back up and update the connected phone**

Create a new timestamped private app-data archive, record its SHA-256, install with `adb install -r`, and verify database, settings, and playback-session files remain present. Restore only if preservation fails.

- [ ] **Step 5: Verify the header and repaired scan on the phone**

Open representative Home, More, and Music folders screens and confirm the shared identity/header. Run Scan quietly, confirm visible progress or completion feedback, and compare available indexed-track count with the pre-scan baseline and Android MediaStore count.

- [ ] **Step 6: Clean task-owned artifacts**

Remove only the exact temporary phone UI XML files and this task-owned worktree. Preserve `.worktrees/implement-v1`, the private backup archives, and the unstaged main-checkout UI test.
