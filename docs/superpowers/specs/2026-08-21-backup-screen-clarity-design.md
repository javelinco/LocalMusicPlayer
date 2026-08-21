# Backup and Restore Screen Clarity Design

## Goal

Make Backup and Restore immediately recognizable as part of **Music, Please!**, show the selected backup location as a readable storage path, and guide a first-time user through choosing a folder, creating a backup, and restoring one.

## Screen Structure

The screen is one vertically scrolling surface with a clear hierarchy:

1. A small **Music, Please!** brand label and a large **Backup & restore** title.
2. One sentence explaining that this screen protects app data and does not copy MP3 files.
3. A **1. Choose a backup location** card.
4. A **2. Create a backup** card.
5. A **3. Restore from a backup** section with refresh, empty-state guidance, and available backup rows.

This keeps routine actions on one screen without turning the feature into a multi-page wizard.

## Folder Location

When no folder is configured, the first card says that Music, Please! needs a phone folder that can be found over USB and presents **Choose backup folder** as the primary next action.

When a folder is configured, the card shows **Current backup location** followed by a readable path. Standard Android Storage Access Framework tree URIs are decoded as follows:

- `primary:Folder/Subfolder` becomes `Internal storage / Folder / Subfolder`.
- A removable-storage volume such as `1234-5678:Backups` becomes `Storage 1234-5678 / Backups`.
- `downloads` becomes `Downloads` and `home` becomes `Documents`.
- An unfamiliar provider falls back to a decoded path or the saved URI so a selected location is never hidden.

The selection button changes to **Change folder** after configuration.

## Backup Guidance

The second card explains in plain language that backups contain playlists, settings, queue state, source descriptions, ignored-track rules, and portable track references, but never MP3s. It retains the automatic-daily and seven-automatic-backup policy in secondary text. The main button is labeled **Create backup now** and stays disabled until a folder is selected.

Operation status appears near this action in a visually distinct status surface.

## Restore Guidance

The restore section says to choose a backup below and explains that Music, Please! validates it and creates a safety backup before replacing current app data. **Refresh list** is a secondary action.

When no folder is selected, the empty state directs the user back to step 1. When a folder is selected but no backups are found, it explains that the user can create one above or copy a Music, Please! backup ZIP into the displayed folder from a computer. Existing backup rows retain their written **Restore** buttons and existing confirmation dialog.

## Architecture

`BackupScreen` remains presentation-only and continues receiving the saved tree URI, backup names, status, and callbacks. A small pure formatter converts a persisted Storage Access Framework tree URI into a readable location. No database, backup schema, permission, storage, or restore behavior changes.

## Testing

- JVM unit tests cover internal storage, removable storage, known roots, percent-encoded names, and unfamiliar-provider fallback.
- Compose tests verify Music, Please! branding, numbered guidance, no-folder next action, selected readable path, changed button label, backup action, restore guidance, and empty states.
- Existing backup manager, codec, navigation, unit, lint, Android-test compilation, and debug assembly checks remain required.

## Scope

This change does not add backup encryption, automatic scheduling controls, deletion or sharing of backup files, a new backup format, or filesystem access outside the existing user-selected folder grant.
