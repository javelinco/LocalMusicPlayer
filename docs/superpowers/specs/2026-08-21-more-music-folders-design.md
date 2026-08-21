# Music folders and scanning under More

## Goal

Remove the ambiguous folder icon from the Library header and make folder, scanning, and index-management tools discoverable through written navigation under More.

## Navigation

More gains a written row named **Music folders and scanning** with supporting text explaining that it adds folders, rescans music, and manages ignored tracks. Selecting it opens the existing sources screen as a More subpage.

The Library header retains the Search icon because search belongs to the current Library view. The folder icon and its expandable Library-tools state are removed.

When no music source exists, the Library continues to show the folder-selection experience automatically. This preserves the approved first-run flow: choose sources, then begin the first scan.

## Reuse and scope

The existing `SourcesScreen` and its callbacks remain the single implementation of folder selection, whole-device discovery, scanning modes, source listing, and ignored-track restoration. No scanner, search, index, or permission behavior changes.

## Testing

Navigation tests verify that More exposes the written option and that it opens source management. Library tests verify that an established library no longer exposes the folder-icon action while the empty-library first-run prompt remains available.
