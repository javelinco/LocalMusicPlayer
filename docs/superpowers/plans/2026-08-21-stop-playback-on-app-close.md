# Stop Playback on App Close Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop Music, Please! playback when its task is removed from Android's Recents screen while preserving normal background playback.

**Architecture:** Opt the existing foreground `PlaybackService` into Android's native stop-with-task lifecycle contract. Protect that manifest behavior with a Robolectric test that inspects the merged application manifest through `PackageManager`.

**Tech Stack:** Android manifest, Kotlin, Robolectric, JUnit 4, Gradle.

## Global Constraints

- Removing the app from Recents stops playback.
- Switching apps, pressing Home, locking the phone, or turning off the screen does not stop playback.
- Existing queue and position persistence remains unchanged.
- Add no permission, dependency, database migration, network access, or media-file write.
- Run computer-only verification; do not invoke connected-device tasks or `adb`.
- Do not modify or stage the pre-existing unstaged `NavigationUiTest.kt` change in the main worktree.

---

### Task 1: Playback service task-removal contract

**Files:**
- Create: `app/src/test/java/com/javelinco/localmusicplayer/playback/service/PlaybackServiceManifestTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: Android's `ServiceInfo.FLAG_STOP_WITH_TASK` manifest flag and the existing `PlaybackService` component.
- Produces: a playback service that Android stops when the owning app task is removed.

- [ ] **Step 1: Write the failing manifest-contract test**

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackServiceManifestTest {
    @Test
    fun removingTheAppTaskStopsPlaybackService() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceInfo = context.packageManager.getServiceInfo(
            ComponentName(context, PlaybackService::class.java),
            PackageManager.ComponentInfoFlags.of(0),
        )

        assertTrue(serviceInfo.flags and ServiceInfo.FLAG_STOP_WITH_TASK != 0)
    }
}
```

The production mutation caught by this test is removing or disabling `android:stopWithTask` on `PlaybackService`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.javelinco.localmusicplayer.playback.service.PlaybackServiceManifestTest"
```

Expected: the assertion fails because the service currently uses the platform default and does not have `FLAG_STOP_WITH_TASK`.

- [ ] **Step 3: Enable the native stop-with-task contract**

Add the attribute only to the existing playback service:

```xml
<service
    android:name=".playback.service.PlaybackService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback"
    android:stopWithTask="true"
    tools:ignore="ExportedService">
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Step 2 command again. Expected: the test passes.

- [ ] **Step 5: Run complete computer-only verification**

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

Expected: every task succeeds without invoking a connected device.

- [ ] **Step 6: Commit the behavior and test**

```powershell
git add app/src/main/AndroidManifest.xml app/src/test/java/com/javelinco/localmusicplayer/playback/service/PlaybackServiceManifestTest.kt
git commit -m "fix: stop playback when app task closes"
```

### Task 2: Integrate and publish

**Files:**
- Verify: all files changed by Task 1 plus the design and implementation-plan documents.

**Interfaces:**
- Consumes: the verified feature branch.
- Produces: synchronized local and GitHub `main` branches.

- [ ] **Step 1: Review scope and repository hygiene**

Run `git diff --check` and confirm no permission, dependency, database, or unrelated file changed. Confirm the main worktree still has only its pre-existing `NavigationUiTest.kt` modification.

- [ ] **Step 2: Fast-forward into main and verify again**

Fast-forward the feature branch into `main`, then rerun:

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
```

- [ ] **Step 3: Push and confirm synchronization**

Fetch `origin/main`, confirm local `main` is not behind, push `main`, and verify `HEAD` equals `origin/main`. Preserve the pre-existing test modification and the user-owned `implement-v1` worktree.
