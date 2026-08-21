# Library view selector label

## Goal

Make the Library view dropdown's purpose obvious without adding visual clutter or consuming another row.

## Design

The existing dropdown button will show `View: <selection>` instead of only the selected value. Examples are `View: Tracks` and `View: Artists`. The dropdown choices remain unchanged because their meaning is clear once the control itself is labeled.

The label stays inside the existing button. This preserves the compact Library layout and avoids the extra height of a separate caption or an outlined field.

## Accessibility and testing

The visible label will also be exposed in Compose semantics, so assistive technology and UI tests identify the selector by both its purpose and current selection. A UI regression test will verify that the initial selector reads `View: Tracks`.

## Scope

This change only clarifies the selector. Search behavior and library navigation are unchanged.
