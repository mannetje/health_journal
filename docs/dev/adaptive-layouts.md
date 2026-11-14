# Adaptive layouts

How the app adapts to the window size. Spec: [platform-android](../../openspec/specs/platform-android/spec.md) (requirement "Navigation and content adapt to the window"). Specs: also [add-adaptive-history-panes](../../openspec/changes/archive/2026-11-14-add-adaptive-history-panes/proposal.md). Screenshots: [screenshots.md](../screenshots.md#screenshots).

There is no window-size library (see [ADR 0003](../adr/0003-dependency-minimization.md)). `AdaptiveLayout` in `ui/common/AdaptiveLayout.kt` holds the thresholds as a pure function, and `AppContent` reads `LocalConfiguration.screenWidthDp` and `screenHeightDp`.

| Window | Navigation |
|---|---|
| width < 600 dp and height >= 480 dp (portrait phone) | bottom `NavigationBar` |
| width >= 600 dp (tablet, unfolded foldable) | `NavigationRail` on the left |
| height < 480 dp (phone in landscape) | `NavigationRail` on the left |

The screen content is capped at 840 dp (`CONTENT_MAX_WIDTH_DP`) and centred. Rail and bar are built from the same destinations in `AppChrome.kt`, so labels and icons cannot drift apart. The Log screen scrolls as a whole, so the tab row moves with the form on short windows. Rotation recreates the Activity; the selected tab and typed values survive it.

## History on wide windows

From 900 dp wide (`HISTORY_TWO_PANE_MIN_WIDTH_DP`) and with a metric that has a chart selected, `HistoryList` shows two panes side by side: the entry timeline on the left, the chart and statistics on the right. The range chips stay above both and the graph/list toggle is hidden, since both are visible; the stored graph-or-list choice per metric is ignored, not changed, so a narrower window restores it. Tapping a day on the migraine graph filters the list next to it. All and Activity have no chart and stay a single list at 840 dp. While History has two panes the content cap is 1240 dp (`contentMaxWidthDp`); other tabs keep 840 dp. A Pixel Tablet in portrait (800 dp) keeps one pane, in landscape (1280 dp) it gets two.

Testing: `AdaptiveLayoutTest` covers the thresholds, the pane mode and the cap. For a manual check, resize a running emulator with `adb shell wm size 1080x2400` and `adb shell wm density 420`, rotate with `settings put system user_rotation 1`, and undo it with `wm size reset` and `wm density reset`.
