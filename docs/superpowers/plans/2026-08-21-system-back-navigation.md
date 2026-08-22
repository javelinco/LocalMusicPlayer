# System Back Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Android system Back unwind the last Music, Please! screen and fall back to Home instead of finishing the activity.

**Architecture:** Add a small immutable navigation-history reducer beside `AppNavigation`, persist it with a string-based Compose saver, and route all app-level navigation through it. Add narrowly enabled local Back handlers for Library metadata/search and playlist details so the most specific visible screen consumes Back first.

**Tech Stack:** Kotlin, Jetpack Compose `BackHandler`, Compose saveable state, JUnit 4, AndroidX Compose UI tests, Gradle.

## Global Constraints

- Back with history returns to the most recently visited screen.
- Back with no history selects Home.
- Back on root Home remains on Home and does not finish the activity.
- Nested library and playlist screens handle Back before the app-level stack.
- Dedicated scanning retains its existing leave-dedicated-mode behavior.
- Add no permission, internet access, dependency, database migration, or media-file write.
- Run computer-only verification; do not invoke connected-device tasks.
- Do not modify or stage the pre-existing unstaged `NavigationUiTest.kt` changes in the main worktree.

---

## File map

- `ui/navigation/NavigationHistory.kt`: pure destination/history model and saveable string representation.
- `ui/navigation/AppNavigation.kt`: app-level destination initialization, navigation dispatch, and system Back handling.
- `ui/library/LibraryScreen.kt`: local Back priority for metadata details and search.
- `ui/library/PlaylistScreen.kt`: local Back priority for an open playlist.
- `NavigationHistoryTest.kt`: reducer behavior and save/restore coverage.
- `SystemBackUiTest.kt`: Compose behavior contract without touching the pre-existing dirty test file.

### Task 1: Pure navigation history

**Files:**
- Create: `app/src/test/java/com/javelinco/localmusicplayer/ui/navigation/NavigationHistoryTest.kt`
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/NavigationHistory.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`

**Interfaces:**
- Consumes: the existing destination names `HOME`, `LIBRARY`, `MORE`, `NOW_PLAYING`, `QUEUE`, `MUSIC_FOLDERS`, `BACKUP`, and `SETTINGS`.
- Produces: `internal enum class Destination`, `internal data class NavigationHistory(val current: Destination? = null, val previous: List<Destination> = emptyList())`, `navigateTo(target)`, `goBack()`, `resolvedCurrent`, `saveNavigationHistory`, and `restoreNavigationHistory`.

- [ ] **Step 1: Write failing reducer tests**

Create `NavigationHistoryTest.kt` with hand-derived expectations:

```kotlin
@Test
fun backUnwindsVisitedScreensInReverseOrderThenStaysHome() {
    val settings = NavigationHistory(Destination.HOME)
        .navigateTo(Destination.MORE)
        .navigateTo(Destination.SETTINGS)

    val more = settings.goBack()
    val home = more.goBack()
    val rootHome = home.goBack()

    assertEquals(Destination.MORE, more.current)
    assertEquals(Destination.HOME, home.current)
    assertEquals(NavigationHistory(Destination.HOME), rootHome)
}

@Test
fun emptyHistoryFallsBackToHomeAndDuplicateNavigationAddsNothing() {
    assertEquals(
        NavigationHistory(Destination.HOME),
        NavigationHistory(Destination.LIBRARY).goBack(),
    )
    assertEquals(
        NavigationHistory(Destination.LIBRARY),
        NavigationHistory(Destination.LIBRARY).navigateTo(Destination.LIBRARY),
    )
}

@Test
fun stringSnapshotRoundTripsNavigationState() {
    val state = NavigationHistory(
        current = Destination.QUEUE,
        previous = listOf(Destination.HOME, Destination.NOW_PLAYING),
    )

    assertEquals(state, restoreNavigationHistory(saveNavigationHistory(state)))
}
```

The production mutation caught by these tests is losing, reordering, duplicating, or failing to restore navigation history.

- [ ] **Step 2: Run the focused unit test and verify RED**

```powershell
./gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.ui.navigation.NavigationHistoryTest"
```

Expected: compilation fails because `NavigationHistory` and its operations do not exist.

- [ ] **Step 3: Implement the minimal immutable reducer**

Move `Destination` from `AppNavigation.kt` into `NavigationHistory.kt` as an internal enum. Implement:

```kotlin
internal data class NavigationHistory(
    val current: Destination? = null,
    val previous: List<Destination> = emptyList(),
) {
    val resolvedCurrent: Destination get() = current ?: Destination.LIBRARY

    fun navigateTo(target: Destination): NavigationHistory {
        val source = resolvedCurrent
        return if (target == source) this
        else NavigationHistory(target, previous + source)
    }

    fun goBack(): NavigationHistory = if (previous.isNotEmpty()) {
        NavigationHistory(previous.last(), previous.dropLast(1))
    } else {
        NavigationHistory(Destination.HOME)
    }
}

internal fun saveNavigationHistory(history: NavigationHistory): List<String> =
    listOf(history.current?.name.orEmpty()) + history.previous.map(Destination::name)

internal fun restoreNavigationHistory(values: List<String>): NavigationHistory = NavigationHistory(
    current = values.firstOrNull()?.takeIf(String::isNotEmpty)?.let(Destination::valueOf),
    previous = values.drop(1).map(Destination::valueOf),
)
```

- [ ] **Step 4: Run the focused unit test and verify GREEN**

Run the Task 1 Gradle command again. Expected: all `NavigationHistoryTest` tests pass.

- [ ] **Step 5: Commit the reducer**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/NavigationHistory.kt app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt app/src/test/java/com/javelinco/localmusicplayer/ui/navigation/NavigationHistoryTest.kt
git commit -m "feat: model in-app navigation history"
```

### Task 2: App-level Back handling

**Files:**
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/SystemBackUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt`

**Interfaces:**
- Consumes: `NavigationHistory`, `navigateTo`, `goBack`, `saveNavigationHistory`, and `restoreNavigationHistory` from Task 1.
- Produces: one saveable navigation state and an always-enabled app-level Compose `BackHandler`.

- [ ] **Step 1: Write failing Compose system-Back contracts**

Create `SystemBackUiTest.kt` rather than editing the dirty `NavigationUiTest.kt`. Reuse a local `setNavigationContent()` fixture and add:

```kotlin
@Test
fun systemBackReturnsFromAppearanceToMoreThenHome() {
    setNavigationContent()
    compose.onNodeWithText("More").performClick()
    compose.onNodeWithText("Appearance").performClick()

    pressBack()
    compose.onNodeWithText("Backup and restore").assertIsDisplayed()
    pressBack()
    compose.onNodeWithText("Recently played").assertIsDisplayed()
}

@Test
fun systemBackWithNoHistoryFallsBackToHomeAndDoesNotExit() {
    setNavigationContent(recentPlaylists = emptyList())
    compose.onNodeWithText("Library").assertIsDisplayed()

    pressBack()
    compose.onNodeWithText("Home").assertIsDisplayed()
    pressBack()
    compose.onNodeWithText("Home").assertIsDisplayed()
}
```

These tests catch removal or miswiring of the real Compose Back handler.

- [ ] **Step 2: Compile Android tests and verify RED at the behavior boundary**

Run `./gradlew.bat compileDebugAndroidTestKotlin`, then use the Task 1 reducer tests as the runnable computer boundary. The new Compose tests must compile; their existing equivalent in the dirty main-worktree test reproduces the current device failure. Do not run connected tests.

- [ ] **Step 3: Replace destination assignment with saveable history dispatch**

In `AppNavigation`:

```kotlin
val navigationSaver = listSaver(
    save = ::saveNavigationHistory,
    restore = ::restoreNavigationHistory,
)
var navigation by rememberSaveable(stateSaver = navigationSaver) {
    mutableStateOf(NavigationHistory())
}
fun navigateTo(target: Destination) {
    navigation = navigation.navigateTo(target)
}
BackHandler { navigation = navigation.goBack() }
```

Keep startup initialization history-free by assigning `NavigationHistory(initialDestination)` only while `navigation.current == null`. Replace every app-level `destination = ...` assignment—primary navigation, mini-player, queue, More entries, artist routing, and playlist routing—with `navigateTo(...)`. Render `navigation.resolvedCurrent`.

- [ ] **Step 4: Run reducer tests and Android-test compilation for GREEN**

```powershell
./gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.ui.navigation.*" compileDebugAndroidTestKotlin
```

Expected: all navigation unit tests pass and Android tests compile.

- [ ] **Step 5: Commit app-level system Back handling**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/ui/navigation/AppNavigation.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui/SystemBackUiTest.kt
git commit -m "fix: keep system back inside the app"
```

### Task 3: Nested library and playlist Back priority

**Files:**
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/SystemBackUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt`

**Interfaces:**
- Consumes: Compose `BackHandler` and existing `openedGroup`, `searchOpen`, and `selectedId` states.
- Produces: nested-screen Back consumption before `AppNavigation.goBack()`.

- [ ] **Step 1: Add failing nested-screen Compose contracts**

Add tests that open an artist detail, library search, and a playlist detail, invoke `pressBack()`, and assert respectively that the artist list, non-search Library header, and playlist list remain visible. Use literal visible labels and the real `LibraryScreen`/`PlaylistScreen` components; do not assert on mocks.

- [ ] **Step 2: Compile the nested Back tests before production changes**

Run `./gradlew.bat compileDebugAndroidTestKotlin`. Expected: tests compile against current APIs; the real device symptom and absence of nested `BackHandler`s establish RED. Do not run them on the connected phone.

- [ ] **Step 3: Add narrowly enabled local Back handlers**

In `LibraryScreen`:

```kotlin
BackHandler(enabled = openedGroup != null || state.searchOpen) {
    if (openedGroup != null) openedGroup = null
    else actions.onCloseSearch()
}
```

In `PlaylistScreen`:

```kotlin
BackHandler(enabled = selected != null) { selectedId = null }
```

Place these handlers after their state declarations so they are composed below—and therefore take precedence over—the app-level handler.

- [ ] **Step 4: Run all computer-only focused checks**

```powershell
./gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
```

Expected: all tasks succeed.

- [ ] **Step 5: Commit nested Back behavior**

```powershell
git add app/src/androidTest/java/com/javelinco/localmusicplayer/ui/SystemBackUiTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/PlaylistScreen.kt
git commit -m "fix: unwind nested library screens on back"
```

### Task 4: Review, verify, and integrate

**Files:**
- Verify all files changed in Tasks 1-3.

**Interfaces:**
- Consumes: complete Back-navigation feature branch.
- Produces: reviewed, computer-verified `main` ready to push.

- [ ] **Step 1: Review the complete diff against the design**

Confirm all destination assignments use `navigateTo`, history is saved/restored, root Home consumes Back, nested handlers are narrowly enabled, dedicated scan still owns Back, and no permission/dependency/schema file changed. Run `git diff --check`.

- [ ] **Step 2: Run the full computer-only verification suite**

```powershell
./gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

Expected: every task succeeds. Do not run `connectedDebugAndroidTest` or any `adb` command.

- [ ] **Step 3: Fast-forward into main and verify again**

Confirm main still contains only the pre-existing unstaged `NavigationUiTest.kt` change, fast-forward the feature branch, and rerun the full verification suite on merged main.

- [ ] **Step 4: Push and verify synchronization**

Push `main` to `origin`; confirm `HEAD == origin/main`, the pre-existing unstaged test remains untouched, and only the established `implement-v1` worktree remains registered.
