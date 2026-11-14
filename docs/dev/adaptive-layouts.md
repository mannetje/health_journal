# Adaptive layouts

How the app adapts to the window size. Spec: [platform-android](../../openspec/specs/platform-android/spec.md) (requirement "Navigation and content adapt to the window"). Screenshots: [screenshots.md](../screenshots.md#screenshots).

There is no window-size library (see [ADR 0003](../adr/0003-dependency-minimization.md)). `AdaptiveLayout` in `ui/common/AdaptiveLayout.kt` holds the thresholds as a pure function, and `AppContent` reads `LocalConfiguration.screenWidthDp` and `screenHeightDp`.

| Window | Navigation |
|---|---|
| width < 600 dp and height >= 480 dp (portrait phone) | bottom `NavigationBar` |
| width >= 600 dp (tablet, unfolded foldable) | `NavigationRail` on the left |
| height < 480 dp (phone in landscape) | `NavigationRail` on the left |

The screen content is capped at 840 dp (`CONTENT_MAX_WIDTH_DP`) and centred. Rail and bar are built from the same destinations in `AppChrome.kt`, so labels and icons cannot drift apart. The Log screen scrolls as a whole, so the tab row moves with the form on short windows. Rotation recreates the Activity; the selected tab and typed values survive it.

Testing: `AdaptiveLayoutTest` covers the thresholds. For a manual check, resize a running emulator with `adb shell wm size 1080x2400` and `adb shell wm density 420`, rotate with `settings put system user_rotation 1`, and undo it with `wm size reset` and `wm density reset`.
