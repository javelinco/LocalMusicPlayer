# Dedicated Scan Branding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a compact Music, Please! identity header to the dedicated scanning screen without changing scanning behavior.

**Architecture:** Keep the change inside the existing `DedicatedScanScreen` composable. Extend its Compose instrumentation test first, then add a Material music-note mark and app-name title above the existing mode heading; all scan, lifecycle, navigation, and persistence code remains untouched.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Material icons, AndroidX Compose UI testing, Gradle, Android SDK signing and device tools

## Global Constraints

- The app name is exactly **Music, Please!**.
- The app identity is a compact header above **Dedicated scanning**.
- The music-note icon is decorative and has no content description.
- Existing explanatory, progress, and exit copy remains unchanged.
- Scan behavior, keep-awake behavior, Back handling, navigation, theming preferences, and animation remain unchanged.
- No new dependency, resource file, persistence value, or Android permission is introduced.
- Android application ID remains `com.javelinco.localmusicplayer`.
- Backup filenames retain the `LocalMusicPlayer-` prefix.

## File Structure

- Modify `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/DedicatedScanUiTest.kt` to require app identity alongside existing dedicated-scan semantics.
- Modify `app/src/main/java/com/javelinco/localmusicplayer/ui/library/DedicatedScanScreen.kt` to render the compact branded header and typography.
- Modify `docs/testing/samsung-acceptance-checklist.md` to record the new instrumentation count and identity check.
- Produce `dist/Music-Please-v0.1.0-development-signed.apk` locally using the existing development certificate.

---

### Task 1: Add the Dedicated-Scan Identity Header Test-First

**Files:**
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/DedicatedScanUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/DedicatedScanScreen.kt`

**Interfaces:**
- Consumes: `DedicatedScanScreen(progress: ScanProgress?, onExit: () -> Unit)` and the existing Material icon dependency
- Produces: visible `Music, Please!` identity above the unchanged dedicated-scan content

- [ ] **Step 1: Extend the Compose test with the missing identity assertion**

Add this assertion immediately before the existing `Dedicated scanning` assertion:

```kotlin
compose.onNodeWithText("Music, Please!").assertIsDisplayed()
```

- [ ] **Step 2: Run the focused test and verify it fails for the missing name**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.DedicatedScanUiTest'
```

Expected: FAIL because no node contains `Music, Please!`; the existing scanning assertions still describe the required behavior.

- [ ] **Step 3: Add the compact header and intentional typography**

Add the required Compose imports, then replace only the first plain heading inside the existing `Column` with:

```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    Text(
        text = "Music, Please!",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}
Text(
    text = "Dedicated scanning",
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold,
)
```

Use these imports:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
```

Do not change `DisposableEffect`, `BackHandler`, `ScanStatus`, the explanation, or the exit button.

- [ ] **Step 4: Re-run the focused Compose test**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.DedicatedScanUiTest'
```

Expected: PASS with `Music, Please!`, `Dedicated scanning`, active progress text, and the explicit exit control all visible.

- [ ] **Step 5: Run format and host regression checks**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
git diff --check
```

Expected: unit tests and release lint PASS; no whitespace errors.

- [ ] **Step 6: Commit the focused UI change**

```powershell
git add -- app/src/androidTest/java/com/javelinco/localmusicplayer/ui/DedicatedScanUiTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/DedicatedScanScreen.kt
git commit -m "feat: brand the dedicated scan screen"
```

### Task 2: Verify, Sign, Install, and Publish

**Files:**
- Modify: `docs/testing/samsung-acceptance-checklist.md`
- Produce locally: `dist/Music-Please-v0.1.0-development-signed.apk`

**Interfaces:**
- Consumes: verified release build, existing Android debug certificate, attached Samsung SM-S928U, public `javelinco/MusicPlease` repository
- Produces: verified signed APK, Samsung acceptance record, published `main` commit

- [ ] **Step 1: Run complete host and privacy verification**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
.\scripts\check_release_manifest.ps1
.\scripts\check_packaged_dependencies.ps1
.\scripts\check_documentation_links.ps1
git diff --check
```

Expected: all commands PASS; no Internet or all-files permission appears.

- [ ] **Step 2: Run the complete Samsung instrumentation suite**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Expected: all 13 instrumentation tests pass on SM-S928U / Android 16, including the updated dedicated scan test and branding identity test.

- [ ] **Step 3: Sign and verify the release with the existing certificate**

```powershell
$scanCheckoutName = Split-Path -Leaf (Get-Location)
$scanUnsignedApk = Join-Path $env:TEMP "LocalMusicPlayer-build/$scanCheckoutName/app/outputs/apk/release/app-release-unsigned.apk"
$scanSignedApk = Join-Path (Get-Location) "dist/Music-Please-v0.1.0-development-signed.apk"
$scanBuildTools = Join-Path $env:LOCALAPPDATA "Android/Sdk/build-tools/37.0.0"
$scanDebugKeystore = Join-Path $env:USERPROFILE ".android/debug.keystore"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $scanSignedApk) | Out-Null
& "$scanBuildTools/apksigner.bat" sign --ks $scanDebugKeystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out $scanSignedApk $scanUnsignedApk
& "$scanBuildTools/apksigner.bat" verify --verbose --print-certs $scanSignedApk
& "$scanBuildTools/aapt.exe" dump badging $scanSignedApk | Select-String "package:|application-label:'Music, Please!'"
Get-FileHash -Algorithm SHA256 -LiteralPath $scanSignedApk
```

Expected: APK verifies with certificate SHA-256 `724d17783107e9393423bf1032620665ef074706cfc4e77ae5088aa24ed6c942`, package `com.javelinco.localmusicplayer`, and label `Music, Please!`; record the new APK SHA-256.

- [ ] **Step 4: Recreate the signed baseline, install the update, and smoke-check it**

```powershell
$scanAdb = Join-Path $env:LOCALAPPDATA "Android/Sdk/platform-tools/adb.exe"
$scanPreviousApk = "C:/Users/javel/OneDrive/Documents/Development/LocalMusicPlayer/dist/Music-Please-v0.1.0-development-signed.apk"
& $scanAdb install $scanPreviousApk
$scanFirstInstallBefore = & $scanAdb shell dumpsys package com.javelinco.localmusicplayer | Select-String "firstInstallTime"
& $scanAdb install -r $scanSignedApk
& $scanAdb shell pm revoke com.javelinco.localmusicplayer android.permission.READ_MEDIA_AUDIO
& $scanAdb logcat -c
& $scanAdb shell am force-stop com.javelinco.localmusicplayer
& $scanAdb shell am start -W -n com.javelinco.localmusicplayer/.MainActivity
$scanFirstInstallAfter = & $scanAdb shell dumpsys package com.javelinco.localmusicplayer | Select-String "firstInstallTime"
$scanFirstInstallBefore
$scanFirstInstallAfter
& $scanAdb logcat -d -b crash
```

The complete instrumentation task removes its temporary app installation, so install the previously published signed APK as the explicit baseline before using `-r`. Expected: the baseline install succeeds; the update and cold launch succeed; first-install time is unchanged across the `-r` update; the optional audio permission is revoked; and the crash buffer is empty.

- [ ] **Step 5: Update and commit the Samsung acceptance record**

Update the instrumentation-suite row to note that the dedicated scan now visibly identifies **Music, Please!** while preserving its mode title, progress, and exit control. Do not change pending personal-library, earbuds, USB restore, or second-phone rows.

```powershell
git add -- docs/testing/samsung-acceptance-checklist.md
git commit -m "docs: record dedicated scan branding verification"
git status --short
```

Expected: clean tracked worktree; signed APK remains ignored in `dist/`.

- [ ] **Step 6: Integrate and publish without force-pushing**

Fast-forward the verified branch to local `main`, rerun `:app:testDebugUnitTest` on merged `main`, copy the signed APK into the main checkout's `dist/` folder, remove only the temporary worktree, and push `main` to `https://github.com/javelinco/MusicPlease.git`.

Expected: local and remote `main` resolve to the same commit; the repository remains public; report commit, APK path/hash, Samsung result, and unchanged compatibility identifiers.
