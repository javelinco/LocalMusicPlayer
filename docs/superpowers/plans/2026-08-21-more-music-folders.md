# Music Folders Under More Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move established-library folder, scanning, and index-management access from an ambiguous Library icon to a written More-menu option while preserving automatic first-run source selection.

**Architecture:** `LibraryScreen` will render `SourcesScreen` only when the source list is empty. `AppNavigation` will add a More-owned destination that reuses `SourcesScreen`, reached through a text-only `Music folders and scanning` row.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4, Compose UI tests

## Global Constraints

- Search remains in the Library header.
- The Library folder icon is removed.
- The More option is named exactly `Music folders and scanning` and has no leading icon.
- First run continues to show source selection automatically when no source exists.
- Existing source, scan, ignored-track, and permission callbacks are reused unchanged.
- Verification must not install, uninstall, or run tests on the personal phone.

---

### Task 1: Limit Library source tools to first run

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/ui/library/LibrarySourcePlacementTest.kt`

**Interfaces:**
- Consumes: `LibraryScreenState.sources: List<MusicSource>`
- Produces: `shouldShowSourceSetupInLibrary(sourceCount: Int): Boolean`

- [ ] **Step 1: Write the failing placement-policy test**

```kotlin
@Test
fun sourceSetupAppearsInLibraryOnlyUntilAFolderExists() {
    assertTrue(shouldShowSourceSetupInLibrary(sourceCount = 0))
    assertFalse(shouldShowSourceSetupInLibrary(sourceCount = 1))
    assertFalse(shouldShowSourceSetupInLibrary(sourceCount = 3))
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `gradlew.bat testDebugUnitTest --tests com.javelinco.localmusicplayer.ui.library.LibrarySourcePlacementTest`

Expected: compilation fails because `shouldShowSourceSetupInLibrary` does not exist.

- [ ] **Step 3: Implement the first-run-only policy and remove the folder button**

Add:

```kotlin
internal fun shouldShowSourceSetupInLibrary(sourceCount: Int): Boolean = sourceCount == 0
```

Remove `toolsOpen`, the `FolderOpen` icon import, and its `IconButton`. Replace `if (state.sources.isEmpty() || toolsOpen)` with `if (shouldShowSourceSetupInLibrary(state.sources.size))`.

- [ ] **Step 4: Run the focused test**

Run: `gradlew.bat testDebugUnitTest --tests com.javelinco.localmusicplayer.ui.library.LibrarySourcePlacementTest`

Expected: PASS.

### Task 2: Add written source management under More

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/MoreMusicFoldersUiTest.kt`

**Interfaces:**
- Consumes: `LibraryScreenState.sources`, `LibraryScreenState.ignoredTracks`, and the existing source/scan callbacks in `LibraryActions`
- Produces: a `Destination.MUSIC_FOLDERS` route opened by `MoreScreen(onMusicFolders = ...)`

- [ ] **Step 1: Write the UI regression test**

Set `AppNavigation` content with one source, select `More`, assert `Music folders and scanning` is displayed, click it, and assert `Music sources` and `Add another folder` are displayed. Also assert the established Library has no node with content description `Library tools`.

- [ ] **Step 2: Implement the written More route**

Add `MUSIC_FOLDERS` to the private destination enum. Add this text-only row to `MoreScreen`:

```kotlin
ListItem(
    headlineContent = { Text("Music folders and scanning") },
    supportingContent = { Text("Add folders, rescan music, and manage ignored tracks") },
    modifier = Modifier.clickable(onClick = onMusicFolders),
)
```

Render the destination with a padded `SourcesScreen` using the existing folder, discovery, scan, ignored-track, and restore callbacks.

- [ ] **Step 3: Compile UI tests and run all local verification**

Run:

```powershell
gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

Expected: all local tasks pass without interacting with a device.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt app/src/test/java/com/javelinco/localmusicplayer/ui/library/LibrarySourcePlacementTest.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui/MoreMusicFoldersUiTest.kt docs/superpowers/plans/2026-08-21-more-music-folders.md
git commit -m "feat: move music folder tools under more"
```
