# Metadata Group Play All Design

## Goal

Make an album, artist, or genre directly playable without requiring a playlist, while keeping the library rows clean and preserving predictable Next and Previous behavior.

## Interaction design

- Each album, artist, and genre row has one visible Play all action. It starts playback immediately without opening a playlist picker.
- Tapping the rest of a row opens a detail view containing that group's tracks.
- The detail view has a prominent **Play all** button and a secondary **Add all to playlist** button.
- Album details use the same interaction as artist and genre details.
- Track-level actions remain unchanged.

The row-level action prioritizes playback rather than placing multiple bulk-action icons beside every group. Adding the whole group to a playlist remains available one level inside the clearly labeled detail view.

## Playback and ordering

Playing a group starts its first displayed track and replaces the active queue with exactly the tracks displayed for that group. Next and Previous therefore stay within the selected album, artist, or genre.

- Artist and genre tracks retain their deterministic library order.
- Album tracks are ordered by disc number, track number, and filename, matching the existing database/library ordering.
- An album is identified by the pair of normalized album artist and normalized album title. Albums with the same title but different album artists are not combined.
- Empty groups cannot start playback.

## Components and data flow

- `LibraryScreen` derives each group's ordered track list from the already-observed local library data.
- `LibraryActions` gains a group-playback callback that accepts an ordered list of tracks.
- `MainActivity` sends that ordered list to the existing playback entry point, using the first track as the starting item.
- The existing playback engine owns queue replacement, shuffle, repeat, Next, and Previous behavior; no new playback engine or storage format is introduced.
- Album rows and artist/genre rows share the same visible playback affordance and detail behavior, while retaining their distinct group identity models.

## Error and edge behavior

- A group with no matching tracks has a disabled Play all action in its detail view and cannot invoke playback from its row.
- Missing or blank metadata continues to use the existing Unknown Artist, Unknown Album, and Unknown Genre groups.
- The feature requires no new permission, internet access, database migration, or media-file modification.

## Verification

- Unit tests cover artist, genre, and album matching, retained ordering, and composite album identity.
- Compose UI tests cover direct row playback and detail-page playback for the three group types.
- Computer-only unit tests, Android-test compilation, lint, and debug assembly verify integration. No connected-device command is part of this change.
