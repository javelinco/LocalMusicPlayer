# Backup format V1

A LocalMusicPlayer backup is an unencrypted ZIP created inside a user-selected Storage Access Framework folder. That folder is intended to be easy to find over USB.

The ZIP contains exactly:

- `manifest.json`: format name, schema version, creation time, and app version.
- `user-data.json`: playlists (including duplicate entries and order), favorites, settings metadata, logical source descriptions, queue metadata when available, and portable track references.

It never contains MP3 audio, cached artwork, scan checkpoints, scan errors, Room database files, or the FTS search index.

## Naming and retention

- Automatic: `LocalMusicPlayer-auto-YYYYMMDD-HHMMSS.zip`
- Manual: `LocalMusicPlayer-manual-YYYYMMDD-HHMMSS.zip`
- Pre-restore safety copy: `LocalMusicPlayer-safety-YYYYMMDD-HHMMSS.zip`

At most one automatic backup is created per UTC day. The newest seven automatic files are retained. Manual and safety backups are not removed by automatic rotation.

## Atomic write and restore

Creation writes a sibling `.tmp` document, reopens it, validates the complete archive and both JSON documents, then promotes it to the final name. Unexpected entries, duplicate entries, path traversal, oversized expanded content, malformed JSON, wrong format identifiers, and newer unsupported schema versions are rejected.

Restore validates first, then creates and validates a safety backup before replacing user data in one Room transaction. Music and the scan index are never replaced by restore.

## Moving to another phone

Storage-provider content URIs are phone-specific. After choosing/scanning music on the new phone, relinking tries an exact relative path first. If that fails, it requires corroborating file size, duration, normalized title, and artist evidence. Equally ranked candidates remain unavailable rather than being guessed. Unavailable playlist entries remain visible and can be repaired after the correct source is selected.

