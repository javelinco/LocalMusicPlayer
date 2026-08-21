# Dedicated Scan Branding Design

## Goal

Make the full-screen dedicated scanning mode unmistakably part of **Music, Please!** while preserving its focused progress information and existing scan behavior.

## Visual Hierarchy

The screen begins with a compact branded header containing:

- A small music-note icon inside an accent-colored circular surface.
- The exact app name **Music, Please!** in prominent title typography.

The existing **Dedicated scanning** label remains immediately below as the screen heading. The app name identifies the product; the heading identifies the current mode. Neither competes with the scan status or implies a new navigation destination.

The explanatory sentence, progress indicator, scan phase/counts, and **Leave scanning mode and save progress** control retain their existing order and wording. Spacing may be adjusted only enough to make the branded header and mode heading read as one intentional top section.

The header uses the current Material color scheme so it remains clear in light and dark modes. The music-note icon is decorative because the adjacent text supplies its meaning; accessibility must not announce the same identity twice.

## Scope and Boundaries

This change is isolated to `DedicatedScanScreen` and its Compose UI test. It does not change:

- Scan priority, progress, checkpointing, completion, or error handling.
- Keep-screen-awake behavior.
- Back handling or the exit action.
- Navigation, theming preferences, or application identity.
- The launcher icon or any other screen.
- Animation or reduced-motion behavior.

No new dependency, resource file, persistence value, or Android permission is required. The existing Material icon set supplies the music-note mark.

## Testing and Acceptance

The dedicated scan Compose test must display an active progress state and assert that all of these are visible together:

- **Music, Please!**
- **Dedicated scanning**
- Scan progress text beginning with **Scanning:**
- **Leave scanning mode and save progress**

The complete host unit suite, release lint/build, manifest and packaged-dependency privacy gates, documentation link gate, full Samsung instrumentation suite, and a signed-update smoke check remain the completion gates.
