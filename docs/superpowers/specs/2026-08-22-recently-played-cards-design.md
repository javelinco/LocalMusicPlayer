# Recently Played Cards Design

## Goal

Make the Recently Played screen attractive and easy to scan while preserving
its queue semantics and all existing song and playlist actions.

## Layout

The screen remains a single vertical list beneath the shared `Music, Please!` /
`Recently played` application header. Songs and playlists use separate sections
with a clear label and item count. Items within each section have consistent
vertical spacing rather than running together as undivided list rows.

## Song cards

Each recent song is a full-width, rounded Material card. The complete card is a
play target and keeps the current behavior of starting the displayed recent-song
queue at that track.

The card contains:

- a compact primary-container music tile;
- a prominent, single-line song title;
- artist text and album context on separate, quieter lines;
- the existing per-track actions menu, including removal from Recently Played.

No album artwork is introduced because artwork is not part of the current
index. Missing artist or album metadata uses the existing `Unknown artist` and
`Unknown album` fallbacks.

## Playlist cards

Recent playlists use the same outer dimensions but a distinct playlist tile.
The playlist name is prominent and the supporting line reports its track count.
The whole card plays the playlist and the existing menu removes only its recent
history entry.

## Visual system

Cards use the existing Material theme, `surfaceContainerLow`, rounded corners,
and a subtle elevation so dark and light themes both work. Spacing and type
hierarchy provide separation without making a long recent history excessively
tall. No permissions, dependencies, navigation, playback, or data changes are
introduced.

## Verification

Compose UI coverage verifies that multiple songs render as distinct tagged
cards, metadata and section counts are visible, playlist cards are distinct,
card taps preserve queue behavior, and action menus continue to remove only the
selected recent entry. The complete computer-only unit, lint, APK, and Android
test compilation suite must pass.
