# Library Drill-In and Playlist Actions Design

## Goal

Make artists, genres, playlists, and tracks directly useful from the Library: users can open collections to see their tracks and add one track or every track in an artist or genre to a playlist without navigating through playlist editing. Completed and failed scan-result banners can be dismissed.

## Scope

This change covers the Library's Tracks, Artists, Genres, Playlists, and track-search presentations; artist and genre detail views; the existing playlist detail view; playlist selection for additions; and scan-result dismissal. Albums, queue behavior, playback semantics, scanning execution, backup data, permissions, dependencies, and persistent schemas are unchanged.

## Library Drill-In

Artist and genre rows become full-row open targets with a visible forward affordance. Opening a row replaces the current Library content area with an in-Library detail view. The detail view contains a back control, the collection name, its available-track count, an `Add all to playlist` action, and the same card-style track rows used by the Tracks view. Back returns to the previously selected Library category; primary navigation does not change.

The detail view filters the already-observed available-track snapshot by exact normalized artist or genre. Filtering is memoized against the track snapshot, selected category, and normalized group name. Track order remains the Library order. Unknown Artist and Unknown Genre groups work through their existing empty normalized values.

Playlist rows remain backed by the existing playlist-detail implementation. They gain a visible forward affordance so it is clear that tapping a playlist opens its ordered entries. The detail header names the selected playlist and retains play, rename, delete, reorder, remove, and add-track behavior.

## Adding to Playlists

Every Library track card gains a trailing playlist-add icon with a descriptive content label. Tapping the icon requests playlist addition without playing the track; tapping the rest of the card still plays it.

Artist and genre list rows also expose a playlist-add icon. It requests all currently available tracks in that normalized group. The artist or genre detail header repeats this as a labeled `Add all to playlist` button. Both entry points use the same playlist picker.

The picker shows existing playlists and their track counts. Choosing one appends the requested track IDs in current Library order and dismisses the picker. The playlist repository's existing duplicate-entry behavior remains unchanged, so deliberately adding the same selection again creates another set of entries. If no playlist exists, the picker explains that one must be created and offers `Go to playlists`, which switches the Library view to Playlists and closes the picker.

## Scan-Result Banner

A completed or failed scan message is rendered as a compact themed banner containing the result text and a close icon labeled `Dismiss scan result`. Dismissing clears the current in-memory scan message. Starting a later scan already clears the old message, and the later scan may publish a new result normally.

## State and Interfaces

- `LibraryScreen` owns transient open-group and pending-playlist-selection UI state.
- `LibraryActions` adds bulk playlist addition and scan-result dismissal callbacks.
- `LibraryViewModel` converts a list of string track IDs to `TrackId` values and delegates one repository `addTracks` call, preserving order.
- `ScanSessionManager` exposes `dismissMessage()`, which sets its message flow to null.
- No navigation destination, database query, entity, migration, or persistent preference is added.

## Error and Empty States

- An artist or genre with no matching available tracks shows the existing empty-track message and disables bulk addition.
- A playlist picker with no playlists provides the explicit route to Playlists rather than silently doing nothing.
- If tracks disappear after the picker opens, the requested IDs represent the visible snapshot at the time the action was initiated; repository behavior remains authoritative.
- Scan failures use the same dismissible banner as successful summaries.

## Testing

- A Compose test verifies that a track's playlist-add icon invokes addition without invoking play.
- Compose tests verify artist and genre rows open their matching track details and bulk-add the correct ordered IDs through the playlist picker.
- A Compose test verifies playlist rows visibly signal and open their ordered-track detail.
- A unit test verifies scan completion messages can be dismissed.
- A Compose test verifies the scan-result banner's dismiss action.
- Existing unit, lint, release, privacy, dependency, documentation, and Samsung instrumentation suites remain required.

## Acceptance Criteria

- A track can be added to an existing playlist directly from its Library card.
- An artist or genre can be added to an existing playlist from its list row or detail header.
- Tapping an artist or genre shows only that group's available tracks.
- Tapping a playlist clearly opens its ordered tracks.
- Playlist additions preserve visible Library order and the existing duplicate policy.
- Track add controls do not accidentally start playback.
- Completed and failed scan-result banners can be dismissed until a later scan publishes another message.
- No new permission, dependency, network access, database schema, or primary navigation destination is introduced.
