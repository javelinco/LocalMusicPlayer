# Scan Reconciliation Design

## Problem

Manual scans resume each source after its persisted cursor and initialize the
"seen" set with every indexed track. That efficiently discovers newer entries,
but it can never notice a file deleted from the phone. A completed no-change
scan also moves from start to complete too quickly to be visible, and the Music
folders screen does not render the completion message.

## Chosen approach

Every requested scan performs a lightweight, complete inventory of each
available source. The inventory provides the authoritative set used for
reconciliation. Existing, available tracks whose URI, filename, modified time,
and size are unchanged reuse their indexed metadata; only new, changed, or
previously unavailable entries invoke MP3 metadata extraction.

Cancellation checkpoints apply only when the running coordinator explicitly
hands an interrupted scan to another mode. A new manual scan clears an old
checkpoint before processing, which also makes checkpoints written by older
app versions harmless. The complete inventory is repeated on resume so missing
files can still be reconciled safely, while metadata processing resumes after
the checkpoint.

Tracks missing from an inventory are marked unavailable rather than deleted.
This removes them from the Library and search while preserving playlist and
backup references. Ignored tracks remain ignored.

## Feedback

The Music folders and scanning screen shows the same active-scan card and
dismissible completion card as the Library. Completion reports files found,
metadata records indexed, tracks removed from the Library, skipped entries, and
errors. A fast scan therefore still produces persistent feedback.

## Alternatives considered

- Re-extract metadata for every MP3 on every scan: correct but unnecessarily
  expensive for libraries containing tens of thousands of files.
- Probe each indexed URI and keep the current incremental discovery cursor:
  potentially tens of thousands of provider calls and unreliable across
  different Storage Access Framework providers.

## Verification

Unit tests cover deletion reconciliation despite an old checkpoint, metadata
reuse for unchanged files, re-indexing changed files, and cancellation resume.
UI compilation and an instrumentation test cover scan feedback on the Music
folders screen. The release candidate is then installed with data preservation,
and a manual phone scan verifies visible progress/completion and stale-track
removal.
