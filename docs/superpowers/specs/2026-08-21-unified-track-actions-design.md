# Unified Track Actions Design

## Goal

Give every indexed track a consistent, discoverable set of actions without crowding the track card. A track can play immediately, be placed deliberately in the queue, be added to a playlist, lead to its artist, explain its indexed metadata, or be deliberately excluded from the library without deleting the MP3.

## Action Set and Presentation

Tapping the body of an available track card remains the fastest `Play now` action. Every track presentation also has a visible overflow button with the accessibility label `More actions for <track title>`. The labeled overflow menu contains, in order:

1. `Play now`
2. `Play next`
3. `Add to queue`
4. `Add to playlist`
5. `Go to artist`
6. `Track information`
7. `Remove from library`

The list deliberately omits `Go to album` and `Go to genre`. It also excludes favorites, tag editing, sharing, and audio-file deletion. Long-press and swipe gestures are not required because they hide important actions.

The same reusable action menu is used for available tracks in Library tracks and search results, artist and genre detail, playlist detail, recently played, and the queue. Unavailable playlist entries retain playlist-entry management but do not expose playback or index actions.

Choosing an action closes the overflow menu before opening a picker or dialog. Playback and queue actions provide a brief non-blocking confirmation such as `Added to queue`; failures remain visible long enough to understand and retry.

## Playback and Queue Semantics

`Play now` uses the existing rule: the currently displayed ordered collection becomes the queue and the selected track starts immediately. From a surface that is not an ordered collection, only the selected track is required.

`Play next` inserts the selected track directly after the current item and does not interrupt the current track. `Add to queue` appends in normal mode. While shuffle is active, it inserts uniformly among the unplayed positions, preserving the true-random shuffle contract. If there is no playback session, either queue command creates a one-track queue and starts it immediately rather than creating an invisible dormant queue.

The Media3 playback list, visible queue screen, and in-memory queue model must represent the same ordered queue. The queue screen must show the actual current queue rather than the entire Library. Track actions must not weaken manual Previous/Next, repeat, Samsung earbud, or shuffle behavior.

## Playlist and Artist Navigation

`Add to playlist` uses the existing playlist picker and append behavior. It does not play the track.

`Go to artist` opens the Library's Artists view and the matching artist detail, regardless of the track's starting surface. A track with no artist opens the existing `Unknown Artist` group. Back returns to the Library artist list; no album or genre navigation is added.

## Track Information

`Track information` opens a read-only dialog showing the indexed title, artist, album, genre, duration, filename, and source label/location. Missing tags are labeled `Unknown` rather than omitted. The dialog performs no file reads and offers no metadata editing.

## Durable Library Exclusion

`Remove from library` first opens a confirmation naming the track and stating all of the following:

- the MP3 file will not be deleted or modified;
- the track will disappear from Library browsing and search;
- future scans will continue to ignore it until it is restored;
- existing playlist references and the current playback queue are not rewritten.

Confirmation creates a user-owned ignored-track record and marks the catalog track unavailable in one Room transaction. The ignored record contains the stable track ID plus portable identity data sufficient for backup relinking. Scanning still recognizes the file as seen, but filters ignored identities before catalog upsert so it cannot become available again. Removing a playing or queued track does not interrupt playback or mutate queue order.

Library tools gains an `Ignored tracks` management view with the ignored count, track identity, and an explicit `Restore to library` action. Restoring deletes the ignore rule and marks the retained catalog row available immediately when present; a later scan may refresh its metadata. No action in this feature deletes an audio file.

Ignored-track rules are user-owned app data. They are included in the versioned USB-visible backup and are relinked through the existing portable track-reference mechanism when restored on another phone. Older backups with no ignored-track field restore an empty ignored set.

## State and Component Boundaries

- A reusable track-action UI component owns only transient menu/dialog state and receives typed callbacks.
- Root navigation coordinates cross-surface playlist picking and artist navigation so every track surface behaves the same way.
- `PlaybackViewModel` exposes explicit `playNext` and `addToQueue` commands and publishes the actual queue IDs for the queue screen.
- `LibraryViewModel` owns ignore/restore operations and exposes ignored tracks for Library tools.
- `LibraryDao` owns transactional ignore/restore operations; the scanner consults the ignore store without touching playlists or playback state.
- The backup data source serializes and restores ignored rules as user data.

These boundaries keep UI presentation, playback mutation, catalog policy, and backup portability independently testable.

## Error and Edge Cases

- Queue actions wait for a ready Media3 controller; a rejected action reports that playback is not ready instead of silently doing nothing.
- Removing an already ignored track is idempotent.
- Restoring an ignore rule whose catalog row is missing removes the rule successfully; the track returns when a later scan finds it.
- Backup restore validates ignored-track data before replacing current user data and retains the existing safety-backup behavior.
- Dedicated scan mode continues to prohibit ordinary playback, queue, playlist, and library-edit actions.

## Testing

- Compose tests verify card-body play, overflow discoverability, each menu callback, playlist-picker routing, information display, and the extra removal confirmation.
- Navigation tests verify `Go to artist` from Library, recent tracks, playlists, and queue.
- Playback tests verify Play Next placement, normal Add to Queue append, uniform shuffled insertion, empty-session behavior, and queue-screen ordering.
- Room tests verify transactional ignore/restore, search and group disappearance, and playlist-reference preservation.
- Scanner tests verify ignored tracks remain ignored across full and incremental rescans.
- Backup tests verify ignored-rule round trips, older backups default to no ignored rules, and cross-phone portable relinking.
- Existing unit, lint, release, privacy, dependency, documentation, and Samsung instrumentation suites remain required.

## Acceptance Criteria

- Every available track presentation offers the same seven actions without crowding the card.
- Tapping a card starts playback immediately.
- Play Next and Add to Queue use the documented normal and true-shuffle placement rules.
- The queue screen reflects the actual playback queue.
- Go to Artist opens the matching artist detail, including Unknown Artist.
- Track information is complete, read-only, and local.
- Removing a track never deletes or modifies its MP3 and requires explicit confirmation.
- Removed tracks remain absent across rescans until restored from Library tools.
- Ignore rules survive supported backup and restore flows.
- No new runtime permission, internet capability, telemetry, or online dependency is introduced.
