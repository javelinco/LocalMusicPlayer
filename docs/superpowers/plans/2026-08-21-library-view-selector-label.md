# Library View Selector Label Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Library view dropdown visibly identify itself while retaining the selected view.

**Architecture:** Add one presentation property to `LibraryView` that formats the selector text, then have `LibraryScreen` render that property. Keep dropdown item labels unchanged.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit 4

## Global Constraints

- The selector text is exactly `View: <selection>`.
- The dropdown remains in its current compact location.
- Search behavior and navigation remain unchanged.
- Do not use the connected personal phone for verification.

---

### Task 1: Label the Library view selector

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryView.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/ui/library/LibraryViewTest.kt`

**Interfaces:**
- Consumes: `LibraryView.label: String`
- Produces: `LibraryView.selectorLabel: String`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun selectorLabelStatesItsPurposeAndSelection() {
    assertEquals("View: Tracks", LibraryView.TRACKS.selectorLabel)
    assertEquals("View: Playlists", LibraryView.PLAYLISTS.selectorLabel)
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `gradlew.bat testDebugUnitTest --tests com.javelinco.localmusicplayer.ui.library.LibraryViewTest`

Expected: compilation fails because `selectorLabel` does not exist.

- [ ] **Step 3: Add the minimal presentation property**

```kotlin
val LibraryView.selectorLabel: String
    get() = "View: $label"
```

Replace the selector button's `Text(state.selectedView.label)` with `Text(state.selectedView.selectorLabel)`. Do not change the menu-item text.

- [ ] **Step 4: Verify the focused test and local build gates**

Run:

```powershell
gradlew.bat testDebugUnitTest --tests com.javelinco.localmusicplayer.ui.library.LibraryViewTest
gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Expected: all tasks pass without touching a connected device.

- [ ] **Step 5: Commit the implementation**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryView.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt app/src/test/java/com/javelinco/localmusicplayer/ui/library/LibraryViewTest.kt docs/superpowers/plans/2026-08-21-library-view-selector-label.md
git commit -m "feat: label the library view selector"
```
