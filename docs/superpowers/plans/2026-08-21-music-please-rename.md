# Music, Please! Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the app and public project to **Music, Please!** without changing the Android application identity or portable-backup compatibility.

**Architecture:** Treat branding as a presentation and publication concern: the Android resource label and current user-facing prose change, while packages, persisted identifiers, backup format values, and backup filename prefixes remain stable. Verify the label and package identity together on Android, retain the existing backup tests as compatibility gates, then publish a newly signed update before renaming the GitHub repository last.

**Tech Stack:** Kotlin, Android resources, AndroidX instrumentation testing, Gradle, PowerShell, Android SDK `adb`/`apksigner`, Git, GitHub

## Global Constraints

- Display name is exactly **Music, Please!**, including the comma and exclamation mark.
- Repository name is exactly `MusicPlease`.
- Published signed APK stem is exactly `Music-Please`.
- Short description is exactly **A private, offline Android music player for your own MP3 collection.**
- Android namespace and application ID remain `com.javelinco.localmusicplayer`.
- Kotlin/Java packages, `LocalMusicPlayerApp`, Room names, DataStore keys, backup schema fields, and persisted identifiers remain unchanged.
- Backup format and the `LocalMusicPlayer-` backup filename prefix remain unchanged.
- Current icon, color system, and interface remain unchanged.
- Historical design records and implementation plans are not rewritten.
- No new dependency or Android permission is introduced.

## File Structure

- Create `app/src/androidTest/java/com/javelinco/localmusicplayer/BrandingTest.kt` to verify the installed application label and stable package identity together.
- Modify `app/src/main/res/values/strings.xml` as the single Android display-name source.
- Modify `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt` and `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt` only where current permission/source copy names the app.
- Modify `settings.gradle.kts` only for the Gradle project display name; leave build-cache paths and package identifiers intact.
- Modify `README.md`, `CHANGELOG.md`, `docs/permissions.md`, `docs/backup-format-v1.md`, and `docs/installing-signed-apks.md` as current project documentation.
- Modify `docs/testing/samsung-acceptance-checklist.md` to record update-install and branding verification.
- Produce `dist/Music-Please-v0.1.0-development-signed.apk` locally; `dist/` remains ignored by Git.
- Change the public repository setting and local `origin` URL only after code, tests, signing, and device checks pass.

---

### Task 1: Lock Branding and Compatibility in Tests

**Files:**
- Create: `app/src/androidTest/java/com/javelinco/localmusicplayer/BrandingTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/com/javelinco/localmusicplayer/data/backup/BackupManagerTest.kt`

**Interfaces:**
- Consumes: Android manifest label reference `@string/app_name`; `BackupManager.createManual()` and `BackupStorage.listNames()`
- Produces: Android display label `Music, Please!`; executable guards for package identity and legacy backup filename prefix

- [ ] **Step 1: Add the failing Android branding test**

```kotlin
package com.javelinco.localmusicplayer

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrandingTest {
    @Test
    fun labelChangesWithoutChangingApplicationIdentity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val label = context.applicationInfo.loadLabel(context.packageManager).toString()

        assertEquals("Music, Please!", label)
        assertEquals("com.javelinco.localmusicplayer", context.packageName)
    }
}
```

- [ ] **Step 2: Run the branding test and verify the old label fails**

Run:

```powershell
./gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.BrandingTest'
```

Expected: FAIL with the actual label `LocalMusicPlayer` instead of `Music, Please!`; the package assertion remains unchanged.

- [ ] **Step 3: Change only the Android display-name resource**

```xml
<resources>
    <string name="app_name">Music, Please!</string>
</resources>
```

Preserve every other existing string in `strings.xml` exactly.

- [ ] **Step 4: Re-run the branding test**

Run:

```powershell
./gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.BrandingTest'
```

Expected: PASS, proving the installed label changed while the package identity did not.

- [ ] **Step 5: Add an explicit legacy backup-prefix characterization test**

Append inside `BackupManagerTest`:

```kotlin
@Test
fun manualBackupKeepsLegacyFilenamePrefixAfterBrandRename() = runTest {
    val storage = FakeBackupStorage()
    val instant = Instant.parse("2026-08-21T12:34:56Z")
    val manager = BackupManager(
        storage = storage,
        snapshot = { bundle(instant.toEpochMilli()) },
        nowEpochMs = { instant.toEpochMilli() },
    )

    val name = manager.createManual()

    assertEquals("LocalMusicPlayer-manual-20260821-123456.zip", name)
    assertTrue(name in storage.files)
}
```

- [ ] **Step 6: Run the backup compatibility test**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest '--tests=com.javelinco.localmusicplayer.data.backup.BackupManagerTest'
```

Expected: PASS without changing `BackupManager`, `BackupModels`, or the format/prefix constants.

- [ ] **Step 7: Commit the tested identity boundary**

```powershell
git add -- app/src/androidTest/java/com/javelinco/localmusicplayer/BrandingTest.kt app/src/main/res/values/strings.xml app/src/test/java/com/javelinco/localmusicplayer/data/backup/BackupManagerTest.kt
git commit -m "feat: rename app to Music, Please"
```

### Task 2: Update Current Product Copy and Documentation

**Files:**
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt`
- Modify: `settings.gradle.kts`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/permissions.md`
- Modify: `docs/backup-format-v1.md`
- Modify: `docs/installing-signed-apks.md`

**Interfaces:**
- Consumes: approved display name, short description, stable backup prefix, and signed artifact stem from the design specification
- Produces: consistent current-facing copy and build-project identity without altering runtime interfaces

- [ ] **Step 1: Record the current-facing old-name baseline**

Run:

```powershell
rg -n "LocalMusicPlayer" README.md CHANGELOG.md settings.gradle.kts app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt docs/permissions.md docs/backup-format-v1.md docs/installing-signed-apks.md
```

Expected: matches in the README/title copy, two in-app explanatory sentences, Gradle project name, privacy documentation, backup prose/prefix examples, and old APK examples.

- [ ] **Step 2: Update the two in-app explanatory sentences**

Use these exact sentences:

```kotlin
"Android will grant audio-only access. Selected folders and files continue to work without it. Music, Please! has no internet or all-files permission."
```

```kotlin
"Choose only what Music, Please! may see. Folder and file choices need no broad audio permission."
```

- [ ] **Step 3: Update the current project identity and README copy**

Set `rootProject.name = "MusicPlease"`. Change the README heading to `# Music, Please!` and its opening sentence to:

```markdown
Music, Please! is a private, offline Android MP3 player for large personal libraries. It has no internet permission, ads, telemetry, accounts, cloud service, microphone access, location access, or all-files access.
```

Add this line beneath the heading before the longer description:

```markdown
**A private, offline Android music player for your own MP3 collection.**
```

- [ ] **Step 4: Update current documentation while explaining the legacy backup name**

Make these exact changes:

- `docs/permissions.md`: replace the prose subject `LocalMusicPlayer` with `Music, Please!`.
- `docs/backup-format-v1.md`: begin with `A Music, Please! backup is an unencrypted ZIP...` and add before the filename list: `For compatibility with existing installations and restores, backup filenames retain the original LocalMusicPlayer prefix.`
- `docs/installing-signed-apks.md`: use `Music-Please-v0.1.0-development-signed.apk` in both `adb install` examples.
- `CHANGELOG.md`: add `- Renamed the app and public project to Music, Please! while preserving application and backup compatibility.` under `Unreleased`.

- [ ] **Step 5: Verify old-name occurrences are intentional**

Run:

```powershell
rg -n "LocalMusicPlayer" README.md CHANGELOG.md settings.gradle.kts app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt docs/permissions.md docs/backup-format-v1.md docs/installing-signed-apks.md
```

Expected: matches only in `docs/backup-format-v1.md` where the stable `LocalMusicPlayer` filename prefix is explicitly explained and shown. Lowercase source-package paths and historical records are outside this branding check.

- [ ] **Step 6: Run documentation, host, and lint gates**

```powershell
./scripts/check_documentation_links.ps1
./gradlew.bat :app:testDebugUnitTest :app:lintRelease
git diff --check
```

Expected: all commands PASS; backup tests still assert `LocalMusicPlayer-` filenames.

- [ ] **Step 7: Commit current copy and documentation**

```powershell
git add -- app/src/main/java/com/javelinco/localmusicplayer/MainActivity.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/SourcesScreen.kt settings.gradle.kts README.md CHANGELOG.md docs/permissions.md docs/backup-format-v1.md docs/installing-signed-apks.md
git commit -m "docs: apply Music, Please branding"
```

### Task 3: Verify, Sign, and Install the Renamed Update

**Files:**
- Modify: `docs/testing/samsung-acceptance-checklist.md`
- Produce locally: `dist/Music-Please-v0.1.0-development-signed.apk`

**Interfaces:**
- Consumes: unchanged `applicationId`, existing debug signing certificate, release build, attached Samsung device
- Produces: verified signed update APK, SHA-256 digest, and recorded physical-device result

- [ ] **Step 1: Run complete host verification and build the unsigned release**

```powershell
./gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
./scripts/check_release_manifest.ps1
./scripts/check_packaged_dependencies.ps1
./scripts/check_documentation_links.ps1
git diff --check
```

Expected: every command PASS; the merged manifest contains no Internet or all-files permission.

- [ ] **Step 2: Run the complete instrumentation suite on the attached Samsung**

Run:

```powershell
./gradlew.bat :app:connectedDebugAndroidTest
```

Expected: all instrumentation tests, including `BrandingTest`, pass on SM-S928U / Android 16.

- [ ] **Step 3: Resolve build and signing paths without changing the signing identity**

```powershell
$renameCheckoutName = Split-Path -Leaf (Get-Location)
$renameUnsignedApk = Join-Path $env:TEMP "LocalMusicPlayer-build/$renameCheckoutName/app/outputs/apk/release/app-release-unsigned.apk"
$renameSignedApk = Join-Path (Get-Location) "dist/Music-Please-v0.1.0-development-signed.apk"
$renameBuildTools = Join-Path $env:LOCALAPPDATA "Android/Sdk/build-tools/37.0.0"
$renameDebugKeystore = Join-Path $env:USERPROFILE ".android/debug.keystore"
Test-Path -LiteralPath $renameUnsignedApk
Test-Path -LiteralPath $renameDebugKeystore
```

Expected: both `Test-Path` calls return `True`. The debug keystore is the same key used for the currently installed development build.

- [ ] **Step 4: Sign and verify the renamed artifact**

```powershell
& "$renameBuildTools/apksigner.bat" sign --ks $renameDebugKeystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out $renameSignedApk $renameUnsignedApk
& "$renameBuildTools/apksigner.bat" verify --verbose --print-certs $renameSignedApk
Get-FileHash -Algorithm SHA256 -LiteralPath $renameSignedApk
```

Expected: signature verification succeeds with the same certificate identity as the installed development build; record the SHA-256 value for the handoff.

- [ ] **Step 5: Install as an update and cold-launch without broad audio permission**

```powershell
adb devices
adb install -r $renameSignedApk
adb shell pm revoke com.javelinco.localmusicplayer android.permission.READ_MEDIA_AUDIO
adb logcat -c
adb shell am force-stop com.javelinco.localmusicplayer
adb shell am start -W -n com.javelinco.localmusicplayer/.MainActivity
adb logcat -d -b crash
```

Expected: `adb install -r` reports `Success`, proving the stable package/signing identity allows an update; launch completes; no runtime crash appears; the app shows `Music, Please!` in Android system surfaces. Confirm the pre-update local state remains present rather than showing data loss.

- [ ] **Step 6: Record the Samsung result**

Add a checklist row recording that the signed renamed release installed over the previous build, preserved local state, launched without `READ_MEDIA_AUDIO`, showed the new label, and produced no crash log. Keep the personal-library, Galaxy Buds, USB restore, and second-phone rows pending unless they are actually performed.

- [ ] **Step 7: Commit the device record and confirm a clean tree**

```powershell
git add -- docs/testing/samsung-acceptance-checklist.md
git commit -m "docs: record Music, Please device verification"
git status --short
```

Expected: the checklist commit succeeds and `git status --short` is empty; the ignored signed APK remains in `dist/`.

### Task 4: Publish and Rename the GitHub Repository

**Files:**
- Modify local Git metadata: `origin` URL
- Modify external repository setting: GitHub repository name

**Interfaces:**
- Consumes: clean, fully verified `main` branch and the already authorized public repository `javelinco/LocalMusicPlayer`
- Produces: canonical public repository `https://github.com/javelinco/MusicPlease` with local `origin` tracking it

- [ ] **Step 1: Push the verified commits to the current canonical remote**

```powershell
git status --short
git branch --show-current
git remote get-url origin
git push origin main
```

Expected: the tree is clean, branch is `main`, current URL is `https://github.com/javelinco/LocalMusicPlayer.git`, and the non-force push succeeds.

- [ ] **Step 2: Rename the repository in GitHub**

In the signed-in GitHub repository, open **Settings → General → Repository name**, replace `LocalMusicPlayer` with `MusicPlease`, and confirm **Rename**. Do not change visibility, owner, default branch, issues, or any destructive setting.

Expected: GitHub opens the repository at `https://github.com/javelinco/MusicPlease` and retains history and the `main` branch.

- [ ] **Step 3: Point the local checkout at the canonical URL**

```powershell
git remote set-url origin https://github.com/javelinco/MusicPlease.git
git remote -v
git ls-remote --exit-code origin refs/heads/main
```

Expected: fetch and push URLs both use `MusicPlease`, and `refs/heads/main` resolves successfully.

- [ ] **Step 4: Verify transition URLs and final repository state**

Open `https://github.com/javelinco/MusicPlease` and confirm the README heading is **Music, Please!**. Open `https://github.com/javelinco/LocalMusicPlayer` and confirm GitHub redirects to the new repository. Confirm the repository remains public.

- [ ] **Step 5: Report the completed rename**

Report the final commit, canonical repository URL, signed APK path, SHA-256 digest, Samsung update result, stable package ID, stable backup prefix, and any hands-on acceptance rows still pending.
