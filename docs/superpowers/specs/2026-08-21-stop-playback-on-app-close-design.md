# Stop Playback When the App Is Closed

## Goal

Removing Music, Please! from Android's Recents screen must stop active playback. Merely placing the app in the background, locking the phone, or turning off the screen must not stop playback.

## Root cause

`PlaybackService` is a foreground `MediaSessionService`. Its manifest does not set `android:stopWithTask`, whose Android default is `false`, so the service and player survive removal of the app task.

## Design

Set `android:stopWithTask="true"` on `PlaybackService` in `AndroidManifest.xml`. Android will then stop the service when the user removes the task rooted in `MainActivity` from Recents. The existing `PlaybackService.onDestroy()` path will save the current queue and position, detach the persistence listener, and release the player and media session.

This uses the platform lifecycle contract instead of duplicating it in `onTaskRemoved()`. It also avoids tying playback to `Activity.onDestroy()`, which can run for reasons other than an intentional task dismissal.

## Behavior boundaries

- Swipe the app away from Recents: stop playback and remove the media notification.
- Switch to another app, press Home, lock the phone, or turn off the screen: continue playback.
- Reopen after closing: restore the saved queue and position in a paused state, preserving the existing session behavior.
- No new permission, dependency, database migration, network access, or media-file write.

## Verification

Add a Robolectric manifest-contract test that resolves `PlaybackService` through `PackageManager` and asserts `ServiceInfo.FLAG_STOP_WITH_TASK`. Run the complete computer-only unit-test, Android-test compilation, lint, and debug APK build gates. Do not run connected-device tests or install the app as part of this change.

Android documents that `android:stopWithTask="true"` automatically stops a service when the user removes an app-owned task: https://developer.android.com/guide/topics/manifest/service-element#stwt
