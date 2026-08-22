# System Back Navigation Design

## Goal

Make Android's system Back action return to the last in-app screen. When no earlier screen exists, Back returns to Home and never finishes the activity from root Home.

## Root cause

`AppNavigation` stores only one destination value and replaces it on every navigation action. It has no history and no system `BackHandler`, so Android receives Back and finishes `MainActivity`. Library metadata, search, and playlist drill-downs are separate local states that also lack Back handling.

## Navigation policy

- Every app-level navigation records the current destination before opening a different destination.
- Back returns to the most recently recorded destination and removes it from history.
- Back with empty history selects Home.
- Back on Home with empty history remains on Home and is consumed.
- Navigating to the already-current destination does not add duplicate history.
- The initial startup destination begins with empty history.
- Dedicated scanning retains its existing Back behavior, which deliberately leaves dedicated mode.

## Nested screens

The most specific visible screen handles Back before the app-level stack:

- An artist, album, or genre detail returns to its library group list.
- An open playlist returns to the playlist list.
- Library search closes before Library itself navigates away.
- Existing dialogs and menus continue to dismiss themselves first.

## Architecture

Introduce a small immutable navigation-history model with `navigateTo` and `goBack` operations. `AppNavigation` stores it with a saveable string-based saver and routes every destination change through the model. Compose's app-level `BackHandler` calls `goBack`; nested library and playlist `BackHandler`s consume Back only while their local detail state is open.

This retains the current screen components and navigation bar rather than introducing a new navigation framework.

## Verification

- Unit tests prove history order, duplicate suppression, empty-history fallback to Home, and root-Home retention.
- Compose tests cover Appearance to More, primary-screen history, library details, search, and playlist details.
- Computer-only unit tests, Android-test compilation, lint, and debug assembly verify integration. No connected-device test or installation is part of this fix unless separately requested.
