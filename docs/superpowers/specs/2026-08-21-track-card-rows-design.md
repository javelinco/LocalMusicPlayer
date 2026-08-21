# Track Card Rows Design

## Goal

Make the Library's Tracks view easy to follow by giving every track a clear visual boundary, while keeping the list efficient and compact for libraries containing tens of thousands of songs.

## Scope

This change applies to the reusable `TrackList` shown by the Library's Tracks view and track search results. It does not change queue rows, playlist editing rows, metadata browsing, playback behavior, sorting, scanning, permissions, or stored data.

## Visual Design

Each track is rendered as a full-width, low-contrast tonal card rather than an edge-to-edge `ListItem`. Cards use the Material theme's surface-container color so they remain readable in light and dark mode without appearing heavily elevated.

Rows have rounded 12 dp corners and an 8 dp gap between cards. The list keeps small vertical content padding. Within each card, the track title uses a semibold title style. Artist and album remain on a quieter secondary line using the theme's on-surface-variant color. Both lines are limited to one line with an ellipsis so unusually long metadata cannot make the list irregular or slow to scan.

The entire card remains the play target. No per-track button, artwork lookup, animation, or extra control is added.

## Performance and Accessibility

The implementation retains `LazyColumn`, stable track-ID keys, and simple theme primitives. It does not load images or add per-row state. Every card is exposed as a distinct clickable semantic node and keeps its visible title and supporting metadata as child content.

## Testing

An Android Compose test supplies two tracks and verifies that each appears as a separate clickable card container. Clicking the second card must invoke playback for the second track. Existing library, navigation, playback, scan, backup, unit, lint, privacy, and packaging checks remain required.

## Acceptance Criteria

- Adjacent tracks have clearly separated rounded tonal backgrounds.
- An 8 dp gap makes the start of each track immediately visible.
- Title and artist/album hierarchy remains readable in light and dark themes.
- The entire row plays the selected track.
- Empty-library messaging and large-list lazy rendering remain unchanged.
- No permissions, dependencies, network access, persistence, or playback behavior changes.
