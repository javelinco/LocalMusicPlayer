# Multiple Folder Sources Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make folder selection explicitly additive for any number of user-selected folders and remove new individual-MP3 selection.

**Architecture:** Continue using one `OpenDocumentTree` launch per folder because that is Android's narrow, persistent Storage Access Framework contract. Append every distinct returned tree URI to the existing source registry, update source-management copy and actions, and preserve the legacy `SAF_DOCUMENT` model solely for restored or upgraded data.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Activity Result APIs, Storage Access Framework, coroutines, JUnit, AndroidX Compose UI tests, Gradle.

## Global Constraints

- No database migration or new permission.
- No internet or all-files permission.
- Selecting a folder must never replace existing folders.
- A repeated folder URI remains deduplicated.
- Existing `SAF_DOCUMENT` database and backup records remain readable.
- The first source keeps the dedicated first-scan behavior; later sources keep quiet/background scanning.

---

### Task 1: Folder-only source acquisition policy

**Files:**
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/data/source/SourceAcquisitionTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/data/source/SourcePickerContracts.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`

**Interfaces:**
- Consumes: `SourceRegistry.add(MusicSource)` and `SafPermissionStore.takeReadPermission(String)`.
- Produces: `SourceSelectionHandler.registerFolder(uri: String, label: String)` as the only scoped source-registration entry point exposed by this file.

- [ ] **Step 1: Write the failing additive-folder tests**

Replace the mixed folder/file policy test and document-registration test with assertions that two distinct calls to `registerFolder` retain both URI identities in order, both grants are taken, and a repeated URI does not create a third source:

```kotlin
@Test
fun folderActionNeverRequestsDeviceWidePermission() {
    val coordinator = SourceAcquisitionCoordinator()

    assertEquals(AcquisitionCommand.OPEN_FOLDER, coordinator.chooseFolder())
    assertFalse(coordinator.devicePermissionWasRequested)
}

@Test
fun individualFileAcquisitionIsNotExposed() {
    assertFalse(AcquisitionCommand.entries.any { it.name == "OPEN_MP3_FILES" })
    assertFalse(SourceAcquisitionCoordinator::class.java.methods.any { it.name == "chooseFiles" })
    assertFalse(SourceSelectionHandler::class.java.methods.any { it.name == "registerDocuments" })
}

@Test
fun selectedFoldersAppendDistinctTreesAndTakeReadGrants() = runTest {
    val registry = InMemorySourceRegistry()
    val permissions = RecordingSafPermissionStore()
    var nextId = 0
    val handler = SourceSelectionHandler(registry, permissions) { SourceId("folder-${nextId++}") }

    handler.registerFolder("content://tree/music", "Music")
    handler.registerFolder("content://tree/concerts", "Concerts")
    handler.registerFolder("content://tree/music", "Music again")

    assertEquals(
        listOf("content://tree/music", "content://tree/concerts"),
        registry.observeSources().first().map(MusicSource::identity),
    )
    assertEquals(setOf("content://tree/music", "content://tree/concerts"), permissions.taken)
}
```

- [ ] **Step 2: Run the focused tests and confirm RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.javelinco.localmusicplayer.data.source.SourceAcquisitionTest"
```

Expected: `individualFileAcquisitionIsNotExposed` fails because the obsolete enum value and methods still exist.

- [ ] **Step 3: Remove new individual-file acquisition**

In `SourcePickerContracts.kt`:

- Remove `chooseFiles`; retain `MP3_MIME_TYPE` because source readers use it to recognize MP3 content.
- Remove `OPEN_MP3_FILES` and `SourceAcquisitionCoordinator.chooseFiles()`.
- Remove `SelectedDocument` and `SourceSelectionHandler.registerDocuments()`.
- Keep `chooseFolder`, `requestPermission`, and `registerFolder()` unchanged.
- Remove the `MainActivity` file-picker launcher, imports, display-name helper, and `onChooseFiles` wiring in the same change so removing the contract does not leave dangling references.

Do not remove `SafDocumentSource`, `SafDocumentReader`, database enum decoding, or backup models.

- [ ] **Step 4: Run focused tests and confirm GREEN**

Run the focused Gradle command from Step 2. Expected: all `SourceAcquisitionTest` tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/javelinco/localmusicplayer/data/source/SourceAcquisitionTest.kt app/src/main/java/com/javelinco/localmusicplayer/data/source/SourcePickerContracts.kt app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt docs/superpowers/plans/2026-08-21-multiple-folder-sources.md
git commit -m "refactor: use folder-only scoped source selection"
```

### Task 2: Explicit additive multi-folder Library UI

**Files:**
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`

**Interfaces:**
- Consumes: `LibraryScreenState.sources: List<MusicSource>` and `LibraryActions.onChooseFolder: () -> Unit`.
- Produces: first-run action text `Choose a music folder`, existing-source action text `Add another folder`, and no file-selection callback.

- [ ] **Step 1: Write failing Compose UI tests**

Update the first-run test to assert `Choose a music folder` is displayed and `Choose specific MP3 files` has zero nodes. Add:

```kotlin
@Test
fun existingFoldersRemainVisibleAndAnotherCanBeAdded() {
    compose.setContent {
        LibraryScreen(
            state = LibraryScreenState(
                sources = listOf(
                    SafTreeSource(SourceId("music"), "content://tree/music", "Music"),
                    SafTreeSource(SourceId("concerts"), "content://tree/concerts", "Concerts"),
                ),
            ),
            actions = LibraryActions(),
        )
    }

    compose.onNodeWithText("Add another folder").assertIsDisplayed()
    compose.onNodeWithText("Music").assertIsDisplayed()
    compose.onNodeWithText("Concerts").assertIsDisplayed()
    compose.onAllNodesWithText("Choose specific MP3 files").assertCountEquals(0)
}
```

- [ ] **Step 2: Run the focused Library UI test class and confirm RED**

Run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '--project-prop=android.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest'
```

Expected: the new first-run and multi-folder tests fail because the old button text and specific-file button are still rendered.

- [ ] **Step 3: Implement the folder-only UI**

Change `SourcesScreen` to remove `onChooseFiles`. Render:

```kotlin
Text("Choose only the folders Music, Please! may see. No broad audio permission is needed.")
Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) {
    Text(if (sources.isEmpty()) "Choose a music folder" else "Add another folder")
}
Text(
    "Repeat this for every folder you want in your Library.",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

Remove `LibraryActions.onChooseFiles` and stop passing that callback from `LibraryScreen`.

- [ ] **Step 4: Run the focused Library UI test class on the Samsung device**

Run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '--project-prop=android.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest'
```

Expected: all Library UI tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt
git commit -m "feat: make folder sources explicitly additive"
```

### Task 3: Remove activity file picker and update current documentation

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Modify: `docs/permissions.md`
- Modify: `docs/testing/samsung-acceptance-checklist.md`

**Interfaces:**
- Consumes: `SourcePickerContracts.chooseFolder` and `LibraryActions.onChooseFolder`.
- Produces: activity wiring with no individual-file launcher and permission copy that identifies selected folders as the narrow-access alternative.

- [ ] **Step 1: Remove obsolete activity wiring**

Confirm the `filePicker`, `SelectedDocument`, `Uri`, `OpenableColumns`, `displayName()`, and `onChooseFiles` activity wiring removed in Task 1 have not returned. Change the device-permission dialog sentence to:

```text
Android will grant audio-only access. Selected folders continue to work without it. Music, Please! has no internet or all-files permission.
```

- [ ] **Step 2: Update current operational documentation**

In `docs/permissions.md`, describe repeatable read-only user-selected folder grants and note that legacy individual-file grants may still be restored. In the Samsung checklist, replace the folder/file source-flow row with a multiple-folder source-flow row and update the instrumentation count after the full run.

- [ ] **Step 3: Compile all Kotlin test source sets**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin
```

Expected: build succeeds and `rg -n "onChooseFiles|chooseFiles|OPEN_MP3_FILES|Choose specific MP3 files" app/src` returns no production matches.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt docs/permissions.md docs/testing/samsung-acceptance-checklist.md
git commit -m "docs: document multiple folder source flow"
```

### Task 4: Full verification and delivery

**Files:**
- Modify only if verification evidence changes: `docs/testing/samsung-acceptance-checklist.md`
- Generate ignored artifact: `dist/Music-Please-v0.1.0-development-signed.apk`

**Interfaces:**
- Consumes: the complete merged app and existing debug signing key.
- Produces: verified `main`, installed signed APK, and matching public GitHub `origin/main`.

- [ ] **Step 1: Run host release gates**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
.\scripts\check_release_manifest.ps1
.\scripts\check_packaged_dependencies.ps1
.\scripts\check_documentation_links.ps1
```

Expected: every command exits zero.

- [ ] **Step 2: Run the full Samsung instrumentation suite**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Expected: all tests pass with zero failures, errors, or skips.

- [ ] **Step 3: Sign, verify, install, and cold-launch**

Use Android build-tools 37.0.0 and `C:\Users\javel\.android\debug.keystore`. Verify certificate SHA-256 `724d17783107e9393423bf1032620665ef074706cfc4e77ae5088aa24ed6c942`, package `com.javelinco.localmusicplayer`, and label `Music, Please!`. Install with `adb install -r`, revoke `READ_MEDIA_AUDIO`, cold-launch `MainActivity`, and require an empty crash log.

- [ ] **Step 4: Integrate and publish**

Fast-forward the verified feature branch into `main`, rerun the merged-tree verification, clean up only the owned feature worktree, push `main` to `https://github.com/javelinco/MusicPlease.git`, and confirm local and remote commit IDs match.
