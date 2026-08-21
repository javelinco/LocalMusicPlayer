# Library-Centered UI Refinement Design

## Goal

Make LocalMusicPlayer feel simpler, more intentional, and more attractive while keeping its offline, permission-minimal behavior. Source selection and scanning become Library concerns, first-run setup flows directly into the initial scan, dedicated scanning exits automatically at completion, and navigation prioritizes listening rather than app administration.

## Information Architecture

The persistent bottom navigation contains three destinations:

- **Home**
- **Library**
- **More**

Search, Playlists, and Sources are no longer bottom-navigation destinations.

Home is contextual:

- While music is actively playing, Home displays Now Playing.
- When music is not actively playing and listening history exists, Home displays recently played songs and playlists.
- When no listening history exists, app startup opens Library with Tracks selected.

The mini-player remains above bottom navigation whenever a playback session exists. Tapping it opens Now Playing. Pausing music does not destroy the session, but Home returns to Recently Played because the contextual rule is based on active playback.

More contains Backup and Restore, Appearance, and the concise offline/privacy statement. Queue remains reachable from Now Playing rather than becoming a permanent navigation destination.

## Library

Library owns all music content and maintenance workflows. Its header contains:

- A remembered view selector showing one of Tracks, Artists, Albums, Genres, or Playlists.
- A search icon that expands an inline search field.
- A Library tools icon that opens source and scan controls.

The selected Library view is persisted in DataStore and restored on the next launch. Tracks is the default when no selection has been saved.

Search exists only inside Library and follows the selected view:

- Tracks searches track metadata and filenames.
- Artists searches artist names.
- Albums searches album titles and album-artist names.
- Genres searches genre names.
- Playlists searches playlist names.

Clearing or closing search returns to the unfiltered selected view without changing that selection.

Playlists retain create, rename, delete, ordering, and membership controls. Starting playback from a playlist records that playlist in recent history and uses its ordered entries as the playback queue.

Favorites are deferred. Favorite controls and markers are removed from the UI. Existing persistence structures remain untouched for backup/schema compatibility, but Favorites are not presented as a current feature.

## First Run and Sources

When there are no configured music sources, Library displays a focused setup state before the normal content list. It explains that the user controls exactly what the app may see and offers:

- Choose a folder.
- Choose specific MP3 files.
- Find all music on this device, with the existing explanation before Android's audio-only permission prompt.

Adding the first source immediately pauses playback if necessary and starts dedicated scanning. There is no intermediate scan-confirmation page. The dedicated screen becomes the second step of first-run setup.

After the first source exists, source management is available through Library tools. Adding any later source starts a quiet background scan automatically so the existing library remains usable.

Library tools also provides a manual **Scan library** action. Manual scan choices are **Scan quietly** and **Dedicated scan**. Dedicated scan is never a persistent menu or navigation item.

## Scan Presentation and Completion

Background scanning is represented by a compact contextual status surface inside Library. It shows the phase and useful counts without covering content, and offers **Prioritize scan** to switch into dedicated scanning.

Dedicated scanning remains a full-screen, keep-awake mode. It pauses playback, gives scanning priority, shows phase and progress counts, and retains an explicit leave-and-checkpoint action for intentional early exit.

When a dedicated scan reaches `ScanPhase.COMPLETE`, the app automatically:

1. Clears dedicated mode.
2. Returns to the previously selected Library view.
3. Shows a brief completion summary containing indexed, skipped, and error counts.

If the scan throws an unrecoverable error, dedicated mode also exits so the user cannot become trapped. Library shows a concise failure message while preserving the last checkpoint. An intentional early exit continues to cancel and checkpoint before returning to Library.

## Home and Recent Playback

The app stores a bounded, device-local listening history containing recent track IDs and playlist IDs with their last-played timestamps. Duplicate plays update recency rather than creating repeated rows. Unavailable tracks and deleted playlists are omitted when reading Home.

Home displays at most five recent songs and five recent playlists. The history is a convenience surface, not user-authored library data, and is not added to the portable backup format. Playlist definitions and entries remain backed up as before.

A track is recorded when playback begins. A playlist is recorded when playback begins from that playlist. Selecting a recent song starts playback in the current Library track context; selecting a recent playlist starts its saved playlist order.

## Visual Design

The selected direction is bold and music-centered:

- Saturated accent surfaces and high contrast establish hierarchy.
- Rounded surfaces are used for meaningful state and primary content, not around every row.
- Real Material icons replace letter-only navigation icons and text pretending to be icons.
- Light and dark themes share the same hierarchy; dark mode is not forced.
- Spacing and typography distinguish headings, metadata, and actions without stacks of full-width buttons.
- Large-library lists remain lazy and compact enough for tens of thousands of tracks.

Now Playing uses two control rows:

- Primary row: Previous, a large Play/Pause control, and Next.
- Secondary row: Shuffle, Repeat, and Queue.

Shuffle and Repeat communicate active state with color and concise labels. Repeat labels explicitly distinguish Off, All, and One. The progress slider and elapsed/total times remain above the controls. The previously proposed level/equalizer graphic is removed entirely.

## State and Component Boundaries

- `LibraryView` is a stable enum shared by settings, Library rendering, and view-aware search.
- `AppSettings` persists the last selected Library view alongside theme, reduced motion, and backup location.
- A recent-play repository owns bounded listening-history writes and valid-item reads without coupling Home to playback-service internals.
- `LibraryViewModel` coordinates Library selection, source-addition scan policy, scan summaries, and view-aware search.
- `PlaybackViewModel` reports active playback and emits playback-start events with optional playlist context.
- `AppNavigation` owns only top-level Home, Library, and More routing; Library-internal selection remains inside the Library feature.
- Dedicated completion is driven by the scan job result/progress state, not by a timer or UI polling.

These boundaries keep scan policy, playback history, and composable rendering independently testable.

## Accessibility and Responsiveness

All icon-only actions have content descriptions and at least 48 dp touch targets. Selected navigation, Library view, Shuffle, and Repeat states are exposed semantically as well as visually. Text respects system scaling, lists use stable keys, and reduced-motion preference disables decorative motion. Search remains debounced and database-backed where the data set can be large.

## Testing

Implementation follows test-driven development and covers:

- Startup routes to Library/Tracks with no history and to Recently Played when history exists.
- Active playback makes Home show Now Playing.
- Library selection persists and defaults safely when no valid value exists.
- Search filters the selected Tracks, Artists, Albums, Genres, or Playlists view.
- First source selection starts dedicated scanning immediately.
- Later source selection starts background scanning immediately.
- Background progress exposes Prioritize scan contextually.
- Dedicated completion clears dedicated mode and emits a completion summary.
- Dedicated failure exits safely and preserves an error message.
- Manual exit requests cancellation/checkpoint and leaves dedicated mode.
- Recent history is deduplicated, bounded, and excludes deleted or unavailable items.
- Playlist playback records playlist history and preserves playlist order.
- Now Playing has separate primary and secondary control rows, no level indicator, and no Favorite control.
- Sources, Search, and Playlists are absent from persistent bottom navigation.
- Icon controls expose accessibility descriptions and state.

The complete host unit suite, Compose instrumentation tests, release lint/build, manifest allowlist, packaged-dependency privacy gate, and Samsung device smoke checks must pass before publication.
