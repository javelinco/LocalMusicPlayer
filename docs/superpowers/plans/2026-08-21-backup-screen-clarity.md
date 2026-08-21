# Backup and Restore Screen Clarity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the confusing Backup and Restore presentation with a branded, guided screen that shows the selected folder as a readable path and makes each next action explicit.

**Architecture:** Keep backup behavior and callbacks unchanged. Add one pure formatter for persisted Storage Access Framework tree URIs, then rebuild `BackupScreen` as a single lazy scrolling surface with branded introduction, numbered sections, clear action hierarchy, status, empty states, backup rows, and the existing safe-restore dialog.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Storage Access Framework URI strings, JUnit 4, Android Compose UI tests, Gradle Android plugin.

## Global Constraints

- Preserve the app label **Music, Please!** and stable package ID `com.javelinco.localmusicplayer`.
- Do not change backup file format, filename prefix, scheduling, retention, validation, safety-backup behavior, permissions, or storage grants.
- Never imply that MP3 files are included in app-data backups.
- Do not add dependencies, internet access, telemetry, or connected-device test commands.
- Keep the existing restore confirmation and its safety explanation.

---

### Task 1: Readable Backup Location

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupLocationFormatter.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/ui/library/BackupLocationFormatterTest.kt`

**Interfaces:**
- Produces: `internal fun backupFolderDisplayPath(treeUri: String): String`.
- Consumes: the persisted Storage Access Framework tree URI already passed to `BackupScreen`.

- [ ] **Step 1: Write the failing formatter tests**

```kotlin
class BackupLocationFormatterTest {
    @Test fun internalStoragePathIsReadable() {
        assertEquals(
            "Internal storage / Music, Please! Backups",
            backupFolderDisplayPath(
                "content://com.android.externalstorage.documents/tree/" +
                    "primary%3AMusic%2C%20Please%21%20Backups",
            ),
        )
    }

    @Test fun removableStoragePathIsReadable() {
        assertEquals(
            "Storage 1234-5678 / Backups / Phone",
            backupFolderDisplayPath(
                "content://com.android.externalstorage.documents/tree/" +
                    "1234-5678%3ABackups%2FPhone",
            ),
        )
    }

    @Test fun knownAndUnknownProvidersStillShowALocation() {
        assertEquals("Downloads", backupFolderDisplayPath("content://provider/tree/downloads"))
        assertEquals("Documents", backupFolderDisplayPath("content://provider/tree/home"))
        assertEquals("content://provider", backupFolderDisplayPath("content://provider"))
    }
}
```

- [ ] **Step 2: Run the focused unit test and verify RED**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.ui.library.BackupLocationFormatterTest"
```

Expected: compilation fails because `backupFolderDisplayPath` does not exist.

- [ ] **Step 3: Implement percent-safe tree-path decoding**

```kotlin
internal fun backupFolderDisplayPath(treeUri: String): String {
    val encodedTreeId = treeUri.substringAfter("/tree/", "")
        .substringBefore('/')
        .takeIf(String::isNotBlank)
        ?: return treeUri
    val documentId = URLDecoder.decode(
        encodedTreeId.replace("+", "%2B"),
        StandardCharsets.UTF_8,
    )
    val root = documentId.substringBefore(':')
    val relative = documentId.substringAfter(':', "")
    val rootLabel = when (root.lowercase()) {
        "primary" -> "Internal storage"
        "downloads" -> "Downloads"
        "home" -> "Documents"
        "raw" -> "Device storage"
        else -> if (relative.isBlank()) root else "Storage $root"
    }
    return (listOf(rootLabel) + relative.split('/').filter(String::isNotBlank))
        .filter(String::isNotBlank)
        .joinToString(" / ")
        .ifBlank { treeUri }
}
```

If the URI has no `/tree/` segment, return the saved URI unchanged so the UI never hides a configured location.

- [ ] **Step 4: Run the focused unit test and verify GREEN**

Run the Step 2 command. Expected: all formatter cases pass.

- [ ] **Step 5: Commit the formatter slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupLocationFormatter.kt app/src/test/java/com/javelinco/localmusicplayer/ui/library/BackupLocationFormatterTest.kt
git commit -m "feat: show readable backup folder paths"
```

### Task 2: Branded Guided Backup Screen

**Files:**
- Create: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupScreenGuidance.kt`
- Create: `app/src/test/java/com/javelinco/localmusicplayer/ui/library/BackupScreenGuidanceTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupScreen.kt`
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/BackupUiTest.kt`

**Interfaces:**
- Consumes: `backupFolderDisplayPath(treeUri: String)` from Task 1.
- Produces: `backupScreenGuidance(selectedFolder: String?, backupCount: Int): BackupScreenGuidance` with `folderPath`, `folderButtonLabel`, and `emptyRestoreMessage`.
- Preserves: the current `BackupScreen` parameters and all seven callback/data contracts.

- [ ] **Step 1: Write failing guidance-model tests**

```kotlin
@Test fun noFolderPointsBackToTheFirstStep() {
    val guidance = backupScreenGuidance(null, backupCount = 0)

    assertNull(guidance.folderPath)
    assertEquals("Choose backup folder", guidance.folderButtonLabel)
    assertEquals(
        "Choose a backup folder in step 1 before looking for backups.",
        guidance.emptyRestoreMessage,
    )
}

@Test fun configuredFolderShowsItsPathAndUsefulEmptyState() {
    val guidance = backupScreenGuidance(
        "content://provider/tree/primary%3AMusic%2C%20Please%21%20Backups",
        backupCount = 0,
    )

    assertEquals("Internal storage / Music, Please! Backups", guidance.folderPath)
    assertEquals("Change folder", guidance.folderButtonLabel)
    assertEquals(
        "No backups found in this folder. Create one above or copy a Music, Please! backup ZIP here from your computer.",
        guidance.emptyRestoreMessage,
    )
}
```

- [ ] **Step 2: Run the focused guidance test and verify RED**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.ui.library.BackupScreenGuidanceTest"
```

Expected: compilation fails because `backupScreenGuidance` does not exist.

- [ ] **Step 3: Implement the minimal presentation model**

```kotlin
internal data class BackupScreenGuidance(
    val folderPath: String?,
    val folderButtonLabel: String,
    val emptyRestoreMessage: String?,
)

internal fun backupScreenGuidance(
    selectedFolder: String?,
    backupCount: Int,
): BackupScreenGuidance = BackupScreenGuidance(
    folderPath = selectedFolder?.let(::backupFolderDisplayPath),
    folderButtonLabel = if (selectedFolder == null) "Choose backup folder" else "Change folder",
    emptyRestoreMessage = when {
        backupCount > 0 -> null
        selectedFolder == null -> "Choose a backup folder in step 1 before looking for backups."
        else -> "No backups found in this folder. Create one above or copy a Music, Please! backup ZIP here from your computer."
    },
)
```

- [ ] **Step 4: Run the focused guidance test and verify GREEN**

Run the Step 2 command. Expected: both guidance states pass.

- [ ] **Step 5: Replace the old Compose expectations with clarity tests**

Add a no-folder test that renders `BackupScreen(null, ...)` and verifies these visible strings:

```kotlin
compose.onNodeWithText("Music, Please!").assertIsDisplayed()
compose.onNodeWithText("Backup & restore").assertIsDisplayed()
compose.onNodeWithText("1. Choose a backup location").assertIsDisplayed()
compose.onNodeWithText("Choose backup folder").assertIsDisplayed()
compose.onNodeWithText("Choose a backup folder in step 1", substring = true).assertIsDisplayed()
```

Add a configured-folder test using the encoded internal-storage URI from Task 1. Verify:

```kotlin
compose.onNodeWithText("Current backup location").assertIsDisplayed()
compose.onNodeWithText("Internal storage / Music, Please! Backups").assertIsDisplayed()
compose.onNodeWithText("Change folder").assertIsDisplayed()
compose.onNodeWithText("2. Create a backup").assertIsDisplayed()
compose.onNodeWithText("Create backup now").assertIsDisplayed()
compose.onNodeWithText("3. Restore from a backup").performScrollTo().assertIsDisplayed()
compose.onNodeWithText("No backups found in this folder", substring = true)
    .performScrollTo().assertIsDisplayed()
```

- [ ] **Step 6: Rebuild `BackupScreen` as one guided lazy list**

Use `LazyColumn` with `contentPadding = PaddingValues(16.dp)` and spaced items. The header must render:

```kotlin
Text("Music, Please!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
Text("Backup & restore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
Text("Protect your playlists and app setup. Your MP3 files are never copied into a backup.")
```

The first card renders the numbered title, either the setup explanation or `guidance.folderPath`, and the action named by `guidance.folderButtonLabel`.

The second card renders **2. Create a backup**, concise contents and retention copy, **Create backup now**, and the optional status in a distinct nested surface.

The third section renders **3. Restore from a backup**, safety guidance, **Refresh list**, then either `guidance.emptyRestoreMessage` or the existing backup rows and **Restore** buttons. Keep the existing confirmation dialog verbatim.

- [ ] **Step 7: Compile Android tests and run all JVM tests**

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin
```

Expected: all JVM tests pass and all Compose tests compile. Do not execute connected tests on the user's phone.

- [ ] **Step 8: Commit the guided UI slice**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupScreenGuidance.kt app/src/test/java/com/javelinco/localmusicplayer/ui/library/BackupScreenGuidanceTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/BackupScreen.kt app/src/androidTest/java/com/javelinco/localmusicplayer/ui/BackupUiTest.kt
git commit -m "feat: clarify backup and restore screen"
```

### Task 3: Full Computer-Only Verification

**Files:**
- Verify all modified files.

**Interfaces:**
- Consumes: the formatter and guided screen from Tasks 1-2.
- Produces: a verified debug APK and clean feature branch ready for integration.

- [ ] **Step 1: Run the complete verification suite**

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

Expected: `BUILD SUCCESSFUL`; no connected-device command is used.

- [ ] **Step 2: Inspect final version-control state**

```powershell
git diff --check
git status --short
git log --oneline --decorate -8
```

Expected: no uncommitted feature changes and no whitespace errors.
