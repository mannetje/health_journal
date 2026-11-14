# 40. Adaptive layouts without a window-size library

- **Date:** 2026-11-14
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The app was built for a portrait phone. On a tablet, an unfolded foldable or a phone in landscape the bottom bar and the full-width content wasted space and were awkward to reach. [ADR 0010](0010-responsive-dutch-ui-layout.md) covers long Dutch text, not window size.

Material guidance is to use a navigation rail on wider windows and to cap the content width. The usual way is `androidx.compose.material3.adaptive` and its window size classes. [ADR 0003](0003-dependency-minimization.md) asks for a reason before a new dependency, and the rules here are three numbers.

## Decision

- **No window-size library.** `AdaptiveLayout` (`ui/common/AdaptiveLayout.kt`) is a pure object with the thresholds and three functions. `AppContent` reads `LocalConfiguration.screenWidthDp` and `screenHeightDp` and passes the result down.
- **Navigation:** a `NavigationRail` when the width is 600 dp or more, or the height is below 480 dp (a phone in landscape). Otherwise the bottom `NavigationBar`. Both show the same destinations.
- **Content width:** capped at 840 dp and centred.
- **History:** from 900 dp wide it shows the entry timeline and the chart with statistics side by side, and the cap becomes 1240 dp. The graph/list toggle is hidden then. The stored graph choice is ignored, not changed, so a narrower window still shows what the user picked.
- Rotation keeps all state, because the navigation state is saved with a `Saver` and the ViewModels outlive the Activity.

## Consequences

- Positive: no new dependency, and the thresholds are unit-tested at both sides of every edge (`AdaptiveLayoutTest`).
- Positive: phone portrait is unchanged.
- Negative: no handling of fold postures or multi-window resizing beyond what the configuration reports. Adding that later is the moment to reconsider the library.
- Details and screenshots: [Adaptive layouts](../dev/adaptive-layouts.md), [screenshots](../screenshots.md#screenshots).
