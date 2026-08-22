# Scan Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a manual scan remove phone-deleted MP3s from the Library, avoid re-reading unchanged metadata, and show persistent scan feedback on the scanning screen.

**Architecture:** Split scanning into a complete lightweight source inventory followed by incremental metadata processing. Reconcile against the complete inventory, retain checkpoints only for an explicitly interrupted in-process scan, and share scan feedback UI between Library and Music folders.

**Tech Stack:** Kotlin, coroutines and Flow, Room, Jetpack Compose, JUnit, Android Gradle Plugin.

## Global Constraints

- Android only; current target devices are recent Samsung phones.
- MP3 only.
- No internet permission or network dependency.
- Libraries may contain tens of thousands of tracks.
- Background scans must remain cooperative and must not interrupt playback, playlist editing, or normal app use.
- Missing tracks become unavailable; playlists and backup references remain intact.
- Do not run connected instrumentation suites because they can clear app data on the attached phone.

---

### Task 1: Full inventory with incremental metadata

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/ScanModels.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/ScanCoordinator.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/scan/RoomScanCatalog.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/db/LibraryDao.kt`
- Test: `app/src/test/java/com/javelinco/localmusicplayer/data/scan/ScanCoordinatorTest.kt`
- Test: `app/src/test/java/com/javelinco/localmusicplayer/data/db/LibraryDaoTest.kt`

**Interfaces:**
- Consumes: `SourceReader.enumerate(source, checkpoint)` and existing `TrackEntity` fingerprints.
- Produces: `ScanCatalog.existingTracks`, `ScanCatalog.clearCheckpoint`, count-returning `ScanCatalog.reconcile`, and `ScanProgress.removed`.

- [ ] **Step 1: Write failing coordinator and DAO tests**

Add tests proving that an old checkpoint does not hide a deleted entry, an unchanged available entry skips `Mp3MetadataExtractor`, a changed entry is indexed, reconciliation counts only newly unavailable tracks, and explicit cancellation resumes metadata after its checkpoint.

- [ ] **Step 2: Run focused tests and verify the intended failures**

Run:

```powershell
./gradlew testDebugUnitTest --tests '*ScanCoordinatorTest' --tests '*LibraryDaoTest'
```

Expected: compilation or assertion failures because the catalog cannot provide existing tracks, clear stale checkpoints, or report removed tracks yet.

- [ ] **Step 3: Implement inventory, fingerprint reuse, and safe reconciliation**

Make `DefaultScanCoordinator` enumerate each source from the beginning into a lightweight entry list, cooperate with the background runtime gate during enumeration and processing, resume metadata only after an explicit coordinator cancellation, clear checkpoints for fresh/completed scans, and reconcile with the complete MP3 ID set. Treat an existing track as reusable only when it is available and URI, filename, size, and modified time all match.

Make the Room catalog return source tracks, delete checkpoints, and return the number of currently available tracks it marks unavailable.

- [ ] **Step 4: Run focused tests and verify they pass**

Run the focused command from Step 2. Expected: all focused tests pass.

- [ ] **Step 5: Commit the scanner repair**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/data app/src/test/java/com/javelinco/localmusicplayer/data
git commit -m "fix: reconcile removed music during scans"
```

### Task 2: Visible feedback on the scanning screen

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/ScanStatus.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/library/ScanSessionManager.kt`
- Test: `app/src/test/java/com/javelinco/localmusicplayer/library/ScanSessionManagerTest.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/SourcesScreenTest.kt`

**Interfaces:**
- Consumes: `ScanProgress`, scan completion message, prioritize callback, and dismiss callback.
- Produces: reusable `ScanFeedback` and a Music folders screen that displays active and completed scan state.

- [ ] **Step 1: Write failing message and UI tests**

Update the completion-message expectation to include found/indexed/removed counts. Add a Compose instrumentation test that renders `SourcesScreen` with a scan message, verifies the message and dismiss control, and invokes dismissal.

- [ ] **Step 2: Run the JVM message test and compile the UI test**

Run:

```powershell
./gradlew testDebugUnitTest --tests '*ScanSessionManagerTest' compileDebugAndroidTestKotlin
```

Expected: the message assertion or UI compilation fails before implementation.

- [ ] **Step 3: Share and wire scan feedback**

Move active progress and dismissible completion cards into `ScanFeedback`. Use it in both `LibraryScreen` and the Music folders destination, passing `scanProgress`, `scanMessage`, prioritize, and dismiss callbacks through `SourcesScreen`. Update completion copy to report found, indexed, removed, skipped, and error counts.

- [ ] **Step 4: Run JVM tests and Android-test compilation**

Run the command from Step 2. Expected: tests pass and Android tests compile. Do not execute the connected suite.

- [ ] **Step 5: Commit the feedback repair**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer app/src/test/java/com/javelinco/localmusicplayer/library app/src/androidTest/java/com/javelinco/localmusicplayer/ui/SourcesScreenTest.kt
git commit -m "fix: show scan progress and results"
```

### Task 3: Verify, integrate, publish, and validate on the phone

**Files:**
- Verify all changed production, unit-test, UI-test, and documentation files.

**Interfaces:**
- Consumes: the complete implementation from Tasks 1 and 2.
- Produces: a tested APK installed with the current private app data preserved.

- [ ] **Step 1: Run full computer-only verification**

Run:

```powershell
./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin
```

Expected: build succeeds with zero test or lint failures.

- [ ] **Step 2: Inspect the diff and commit documentation**

Confirm only planned files changed and commit both design and plan documents.

- [ ] **Step 3: Merge to main and repeat verification**

Merge the branch into `main` without modifying the existing unstaged `NavigationUiTest.kt`, then repeat the full computer-only verification on the merged tree.

- [ ] **Step 4: Push main and create a fresh private phone-data backup**

Push the verified commit. Stop the app briefly, copy its private data with `run-as`, hash the archive, and restart the app. Do not overwrite the prior safety backup.

- [ ] **Step 5: Install and manually verify the original symptom**

Install with `adb install -r`, confirm database/settings/session sizes survived, open Music folders and scanning, tap Scan quietly, verify visible active or completion feedback, and verify the Library count drops when stale entries are reconciled. Do not restore unless preservation checks fail.

- [ ] **Step 6: Clean only task-owned temporary artifacts**

Remove the exact temporary UI XML files created on the phone and the task-owned worktree after verified integration. Preserve `.worktrees/implement-v1` and the unstaged main-checkout UI test.
