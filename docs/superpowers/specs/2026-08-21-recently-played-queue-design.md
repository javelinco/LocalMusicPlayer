# Recently Played Queue Design

## Goal

Make the Recently Played song list act as an intentional playback source: selecting any song starts a queue made from the currently displayed recent songs, with previous and next following that captured order. Let the user remove songs and playlists from Recently Played without affecting their music, playlists, or active playback queue.

## Behavior

- The Songs section remains ordered newest first.
- Tapping a song captures the displayed song list and starts playback at the tapped song.
- The song row and its `Play now` menu action use the same captured recent-song queue.
- Previous moves toward the row above the starting song and next moves toward the row below it, subject to the existing player restart-current-track behavior.
- Once playback starts, later history updates do not mutate the active queue.
- Removing a song deletes only its track-history entry.
- Removing a playlist deletes only its playlist-history entry.
- History removal is immediate and does not require confirmation because it is reversible by playing the item again and cannot delete audio or playlist data.
- Removing an item from history does not modify an already-playing queue.

## Architecture

`HomeScreen` receives a dedicated recent-track playback callback that includes both the selected track and the displayed recent-track snapshot. `AppNavigation` wires this callback separately from the library-wide `Play now` behavior. `MainActivity` passes the snapshot into the existing `PlaybackViewModel.play(track, view)` entry point, so no new queue engine is required.

`RecentPlayDao` gains a keyed delete query. `RecentPlayRepository` exposes explicit `removeTrack` and `removePlaylist` methods, and `LibraryViewModel` launches those operations for the UI. Song and playlist overflow menus expose `Remove from recently played`; the existing song action menu accepts this contextual action only on Home.

## Data and Error Handling

Room remains the source of truth, so successful removal automatically updates the observed Home lists. The delete is idempotent: removing a stale or already-removed history entry has no effect. Empty or one-song recent lists continue to work through the existing playback method. Unavailable tracks are already excluded by the DAO query.

## Testing

- Repository tests prove track and playlist history entries can be removed independently without deleting their underlying records.
- Pure callback/queue-selection tests prove Home forwards the selected song with the complete displayed order.
- Compose UI tests prove song and playlist menus expose written removal actions and invoke the correct callbacks.
- Existing playback, navigation, database, lint, and assembly checks remain green.

## Scope

This change does not add manual history reordering, clearing all history, restoring removed history, changing shuffle/repeat semantics, or synchronizing history edits into the active queue.
