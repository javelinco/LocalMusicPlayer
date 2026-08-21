# Changelog

## Unreleased

- Renamed the app and public project to Music, Please! while preserving application and backup compatibility.
- Consolidated navigation to Home, Library, and More, with search, playlists, sources, and scanning handled contextually inside Library.
- Added remembered Library views and view-aware database-backed search.
- Added bounded local recently played history for tracks and playlists.
- Made first-source dedicated scanning automatic and dedicated mode self-closing on completion or failure.
- Refined light and dark visual themes and split Now Playing controls into transport and playback-mode rows.
- Removed Favorites controls and the decorative playing-level indicator from the interface while retaining compatible stored data.

## 0.1.0 — 2026-08-20

- First working Android V1 candidate.
- Offline MP3 discovery through selected folders/files and optional device audio access.
- Resumable background and dedicated metadata scanning.
- Room/FTS library tested with 50,000 tracks.
- Media3 background playback and Samsung MediaSession controls.
- Uniform secure shuffle, explicit repeat modes, and history-aware Previous behavior.
- Playlists, favorites, theme/reduced-motion controls, and playing indicator.
- Versioned USB-visible backup/restore with automatic rotation and conservative cross-device relinking.
- Release permission and packaged-dependency gates.
