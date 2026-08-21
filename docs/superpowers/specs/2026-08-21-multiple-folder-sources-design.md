# Multiple folder sources design

## Goal

Make it obvious that Music, Please! can use several user-selected music folders while keeping source access narrowly scoped. Remove the option to select individual MP3 files as a new source.

## Selected approach

Use Android's Storage Access Framework folder picker repeatedly. Android selects one document tree per picker launch, so Music, Please! will make the additive behavior explicit instead of implying that all folders must be chosen in one system dialog.

- With no sources, the primary action is **Choose a music folder**.
- With at least one source, the action is **Add another folder**.
- Supporting text explains that the action may be repeated for every folder the user wants to include.
- Every accepted folder is appended to the existing source registry. Selecting a folder never replaces earlier folders.
- The source list remains visible so the user can confirm which folders are included.
- The first source starts the existing dedicated first scan. Later sources use the existing quiet/background source-addition scan behavior, so playback and Library use remain responsive.

This preserves Android's narrow, persistent, read-only folder grants and avoids requesting broad filesystem access.

## Removed interaction

The **Choose specific MP3 files** action and its activity-result launcher are removed. Source-acquisition policy no longer exposes an individual-file command or registration path.

The existing `SAF_DOCUMENT` model, database decoding, reader, and backup compatibility remain in place. This allows an installation or restored backup created by an older build to continue reading previously registered file sources; it does not allow new file sources to be selected.

## Whole-device discovery

The optional **Find all device music** action remains separate. It continues to explain and request Android's audio-only permission only after deliberate user confirmation. Its copy refers only to selected folders as the narrow-access alternative.

## Data and permissions

No database migration, new permission, internet access, or all-files permission is introduced. Folder identity continues to be the persisted tree URI. Selecting the same tree again remains deduplicated by the source registry.

## Verification

- Unit tests prove folder selection takes a read grant, appends distinct folder URIs, and deduplicates a repeated URI.
- UI tests prove first run offers a music-folder action but no specific-file action.
- UI tests prove an existing multi-folder Library lists both folders and offers **Add another folder**.
- Existing source-addition tests continue to prove the first source starts dedicated scanning and later sources scan quietly.
- The full unit, lint, release, privacy, and Samsung instrumentation gates run before publication.
