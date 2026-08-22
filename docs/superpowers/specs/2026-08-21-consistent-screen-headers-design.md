# Consistent Screen Headers Design

## Goal

Every full-screen destination clearly identifies both the application and the
current screen using one consistent, compact header.

## Header pattern

The shared header contains two lines:

1. `Music, Please!` in the theme's primary color and label typography.
2. The current screen title in bold headline typography.

The normal navigation shell renders this header above destination content, so a
new destination cannot accidentally omit the application identity. Dedicated
scanning is outside that shell and renders the same shared component itself.

## Screen titles

- Home while idle: `Recently played`
- Home while playing, and the explicit player destination: `Now playing`
- Library: `Library`
- More: `More`
- Queue: `Queue`
- Music folders: `Music folders & scanning`
- Backup: `Backup & restore`
- Settings: `Appearance`
- Dedicated scan: `Dedicated scanning`

Library artist, album, genre, and playlist drill-ins remain under the `Library`
header and keep their own content-level title. Dialogs and Android system
pickers are not full-screen app destinations and do not gain another header.

## Integration

Existing one-off screen titles and branding are removed where the navigation
header replaces them. Screen-specific explanatory copy, controls, and content
remain unchanged. The header uses the existing Material theme and introduces no
permissions, dependencies, network access, or navigation behavior.

## Verification

A unit test covers the title for every navigation destination, including the
dynamic Home title. A Compose test covers the shared header's application name
and screen title. Existing UI tests continue to compile, and the full
computer-only build, unit tests, lint, and Android-test compilation must pass.
