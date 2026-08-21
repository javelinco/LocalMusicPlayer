# Music, Please! Naming Design

## Goal

Rename the app from **LocalMusicPlayer** to **Music, Please!** in every user-facing and project-facing context while preserving the technical identities that existing installations and portable backups depend on.

The new name should feel playful, immediate, and centered on listening. It does not change the product's core promise: a private, offline Android MP3 player that obeys the user's playback choices.

## Approved Identity

- Display name: **Music, Please!**
- Repository name: `MusicPlease`
- Release artifact stem: `Music-Please`
- Short description: **A private, offline Android music player for your own MP3 collection.**

The comma and exclamation mark are part of the display name. Filesystem and repository names omit punctuation that would make links or filenames awkward.

The existing icon, color system, and interface remain unchanged for this naming pass. The playful name is enough personality; it does not require a mascot or novelty styling.

## Rename Surface

The implementation updates:

- Android launcher and system-visible application label.
- In-app references to the old product name.
- README title, opening description, and project-facing documentation where the product is named in prose.
- Signed APK filenames produced or published after the rename.
- The public GitHub repository name and local Git remote after the code change is ready.

Historical design records may retain `LocalMusicPlayer` when they describe the project as it was named at the time. They are records, not current user documentation.

## Compatibility Boundaries

The following identifiers remain unchanged:

- Android namespace and application ID: `com.javelinco.localmusicplayer`.
- Kotlin and Java package paths and class names, including `LocalMusicPlayerApp`.
- Room database names, DataStore keys, preferences, backup schema fields, and other persisted identifiers.
- Existing backup filename prefix: `LocalMusicPlayer-`.
- Backup format version and restore behavior.

Keeping the Android application ID allows the renamed build to install as an update instead of becoming a separate app. Keeping persisted and backup identifiers avoids migrations that provide no user benefit and ensures backups made before the rename remain restorable on the same or another phone.

The backup documentation will explain that `LocalMusicPlayer-*.zip` is the stable compatibility filename even though the app is now named Music, Please! New backups continue using that prefix in this version.

## Repository Rename

The GitHub repository moves from `javelinco/LocalMusicPlayer` to `javelinco/MusicPlease` only after the local naming change is committed and verified. The local `origin` URL is then updated to the new canonical URL. GitHub's redirect for the old repository URL is useful during the transition, but current documentation should link directly to the new URL.

The local checkout directory does not need to be renamed as part of this change. Renaming it could disrupt the existing worktree arrangement and has no effect on the app or public project identity.

## Preliminary Conflict Screen

A preliminary web, Google Play, and Apple App Store search on 2026-08-21 found no obvious music-player application using the exact name **Music, Please!** This is a practical collision check, not trademark or legal clearance.

## Verification

The rename is complete when:

- The installed app and launcher show **Music, Please!**.
- Existing app data survives an update from the current signed build.
- A backup created before the rename validates and restores successfully.
- A newly created backup retains the compatible `LocalMusicPlayer-` filename and validates successfully.
- Current README and user documentation use **Music, Please!**, except where the old technical identifier is intentionally documented.
- The signed APK uses the `Music-Please` artifact stem.
- The release build, automated tests, lint, privacy gates, and Samsung smoke test pass.
- The public repository is reachable at its new canonical `MusicPlease` URL and the local remote points to it.

## Deferred Work

- A new icon, logo, mascot, or animation.
- Renaming the Android application ID or source packages.
- Renaming backup-format identifiers or legacy backup filenames.
- Formal trademark registration or legal review.
