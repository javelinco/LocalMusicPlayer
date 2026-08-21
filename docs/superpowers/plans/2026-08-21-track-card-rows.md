# Track Card Rows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace visually continuous Library track rows with compact, clearly separated tonal cards.

**Architecture:** Keep the existing lazy, stable-keyed `TrackList` data flow and change only its row presentation. Each track becomes a themed Material 3 `Card` that owns its click action and a stable test tag; metadata remains a simple two-line hierarchy with ellipsis.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android Compose instrumentation tests, Gradle, ADB.

## Global Constraints

- Apply the change only to the Library `TrackList`, which also renders track search results.
- Retain `LazyColumn` and stable `TrackEntity.trackId` keys for large libraries.
- Use 12 dp rounded tonal cards with 8 dp separation and compact internal padding.
- Keep the full card clickable and do not add artwork, per-track controls, state, animation, permissions, dependencies, network access, persistence, or playback changes.
- Preserve the stable package ID `com.javelinco.localmusicplayer`, app label `Music, Please!`, and backup prefix `LocalMusicPlayer-`.

---

### Task 1: Render Library tracks as distinct clickable cards

**Files:**
- Modify: `app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt`
- Modify: `app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt`

**Interfaces:**
- Consumes: `TrackList(tracks: List<TrackEntity>, onPlay: (TrackEntity) -> Unit)` and `TrackEntity` metadata.
- Produces: Per-track semantic nodes tagged `track-card:<trackId>` whose full surface invokes `onPlay(track)`.

- [ ] **Step 1: Add the failing Compose test**

Add imports for `assertHasClickAction`, `onNodeWithTag`, `performClick`, `TrackEntity`, `TrackList`, and `assertEquals`. Add this test and helper to `LibraryUiTest`:

```kotlin
@Test fun tracksRenderAsSeparateClickableCards() {
    var playedTrackId: String? = null
    compose.setContent {
        TrackList(
            tracks = listOf(track("one", "First track"), track("two", "Second track")),
            onPlay = { playedTrackId = it.trackId },
        )
    }

    compose.onNodeWithTag("track-card:one").assertIsDisplayed().assertHasClickAction()
    compose.onNodeWithTag("track-card:two").assertIsDisplayed().assertHasClickAction().performClick()
    compose.runOnIdle { assertEquals("two", playedTrackId) }
}

private fun track(id: String, title: String) = TrackEntity(
    trackId = id,
    sourceId = "source",
    contentUri = "content://music/$id",
    fileName = "$title.mp3",
    title = title,
    artist = "Artist $id",
    albumTitle = "Album $id",
    albumArtist = "Artist $id",
    genre = "Genre",
    normalizedTitle = title.lowercase(),
    normalizedArtist = "artist $id",
    normalizedAlbumTitle = "album $id",
    normalizedAlbumArtist = "artist $id",
    normalizedGenre = "genre",
    discNumber = 1,
    trackNumber = 1,
    durationMs = 180_000,
    modifiedAtEpochMs = 1,
    sizeBytes = 1,
    available = true,
)
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.javelinco.localmusicplayer.ui.LibraryUiTest#tracksRenderAsSeparateClickableCards'
```

Expected: FAIL because no nodes tagged `track-card:one` or `track-card:two` exist.

- [ ] **Step 3: Implement the minimal card row presentation**

In `LibraryScreen.kt`, remove the now-unused `clickable` import and add `PaddingValues`, `RoundedCornerShape`, `CardDefaults`, `testTag`, and `TextOverflow`. Replace only the non-empty `LazyColumn` body in `TrackList` with:

```kotlin
LazyColumn(
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(tracks, key = TrackEntity::trackId) { track ->
        Card(
            onClick = { onPlay(track) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("track-card:${track.trackId}"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = track.title ?: track.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(track.artist, track.albumTitle)
                        .joinToString(" — ")
                        .ifBlank { track.fileName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the command from Step 2 again.

Expected: PASS; both track-card nodes are displayed and clickable, and clicking the second reports track ID `two`.

- [ ] **Step 5: Run host checks and commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease
git diff --check
```

Then commit:

```powershell
git add app/src/androidTest/java/com/javelinco/localmusicplayer/ui/LibraryUiTest.kt app/src/main/java/com/javelinco/localmusicplayer/ui/library/LibraryScreen.kt
git commit -m "feat: separate tracks with card rows"
```

### Task 2: Verify, document, sign, install, and publish

**Files:**
- Modify: `docs/testing/samsung-acceptance-checklist.md`
- Generate: `dist/Music-Please-v0.1.0-development-signed.apk` (ignored release artifact)

**Interfaces:**
- Consumes: the verified release APK and attached Samsung SM-S928U.
- Produces: recorded device evidence, a signed update APK, and published `main`.

- [ ] **Step 1: Run the complete release and device suites**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
.\scripts\check_release_manifest.ps1
.\scripts\check_packaged_dependencies.ps1
.\scripts\check_documentation_links.ps1
.\gradlew.bat :app:connectedDebugAndroidTest
git diff --check
```

Expected: all host tasks and all Android instrumentation tests pass; permission, packaged-dependency, and documentation gates pass.

- [ ] **Step 2: Record Samsung verification and commit**

Update the full instrumentation-suite row in `docs/testing/samsung-acceptance-checklist.md` to mention separate clickable track cards while retaining the existing evidence. Do not change pending personal-library, earbuds, USB restore, or second-phone rows.

```powershell
git add docs/testing/samsung-acceptance-checklist.md
git commit -m "docs: record track card verification"
```

- [ ] **Step 3: Sign and verify the release APK**

Use Android build-tools 37.0.0 and the existing debug keystore to sign `app-release-unsigned.apk` as `dist/Music-Please-v0.1.0-development-signed.apk`. Verify the certificate digest remains `724d17783107e9393423bf1032620665ef074706cfc4e77ae5088aa24ed6c942`, `aapt dump badging` reports package `com.javelinco.localmusicplayer` and label `Music, Please!`, and record the APK SHA-256.

- [ ] **Step 4: Verify the signed in-place Samsung update**

Install the prior signed APK, record `firstInstallTime`, install the new APK with `adb install -r`, revoke `READ_MEDIA_AUDIO`, cold-launch `.MainActivity`, and confirm the install time is unchanged and the crash buffer is empty.

- [ ] **Step 5: Integrate and publish the already-selected path**

Fast-forward the feature branch into local `main`, copy the signed APK into the main checkout, rerun `:app:testDebugUnitTest` on merged `main`, remove only the owned clean feature worktree and merged branch, push `main`, and verify `git ls-remote origin refs/heads/main` matches local `HEAD`.

- [ ] **Step 6: Run the final completion audit**

Confirm `main` is clean, local `HEAD` equals `origin/main`, the feature branch is removed, the signed APK identity and SHA remain correct, the installed app retains denied audio permission, and the Samsung crash buffer is empty.
