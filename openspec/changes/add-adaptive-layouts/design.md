## Context

`AppContent` is a `Scaffold` with an `AppTopBar`, an `AppNavigationBar` and the tab content. There is no window-size handling anywhere. Compose already provides the window size through `LocalConfiguration` (or `BoxWithConstraints`); no extra library is needed.

## Goals / Non-Goals

**Goals:** usable landscape phone, sensible tablet and foldable, identical behaviour on portrait phone, no new dependency.
**Non-Goals:** a two-pane History, posture handling, new screens.

## Decisions

### 1. Thresholds in one place
A small pure function maps window width and height in dp to a `NavigationLayout` (`Bar` or `Rail`) and a content max width. Rail when width >= 600 dp or height < 480 dp; content cap 840 dp. The function is plain Kotlin with unit tests, so the rules are checked without a device.

### 2. Rail beside the content, bar otherwise
The scaffold keeps the top bar. With `Rail` the bottom bar is omitted and a `NavigationRail` with the same destinations sits at the start of the content row. Selection, labels and icons come from `AppNavDestination`, so both bars cannot drift apart.

### 3. Cap applied once
The tab content is wrapped in a centred column with `widthIn(max = 840.dp)`, applied at the scaffold, so no screen needs its own width logic. Full-bleed items (the top bar, banners) stay full width.

### 4. Landscape phones
With height < 480 dp the Log tab row scrolls with the form. This reuses the existing scroll state; nothing about the form changes.

### 5. Why not an adaptive library
The adaptive navigation suite and window-size-class artifacts add dependencies for two thresholds. ADR 0003 prefers none. If a two-pane History is added later, that decision can be revisited with that change.

## Risks / Trade-offs
- Compose and the Compose BOM are on 1.11.1; `NavigationRail` is in the Material 3 version already used.
- Split-screen and freeform windows change size at runtime; the layout reads the current window each composition, so it follows.
