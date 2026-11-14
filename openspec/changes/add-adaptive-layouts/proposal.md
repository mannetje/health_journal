# Add adaptive layouts

## Why
The app is one column with a top bar, a tab row and a bottom bar, sized for a portrait phone. On a phone held in landscape those bars leave almost no room for the form (on the glucose screen a few lines of the Log tab are visible), and on a tablet or an unfolded foldable the single column stretches across the whole width. The app sets no orientation lock and no resizability limit, so people already meet both cases. This change makes the shell adapt to the window instead of assuming a portrait phone.

## What Changes
- **Navigation rail instead of the bottom bar** when the window is at least 600 dp wide or less than 480 dp high (landscape phone, tablet, unfolded foldable, split screen). The bar stays on a portrait phone. The same destinations, order, labels and selected state apply in both.
- **Content width cap:** on windows wider than 840 dp the screen content is centred in a column of at most 840 dp, so lines, chips and fields stay readable. Charts and the History list use the same cap.
- **Less chrome in landscape phones:** with a window less than 480 dp high the top bar keeps its buttons and title and the Log tab row scrolls with the form instead of staying fixed, so the form gets the screen.
- **No new libraries and no new permissions:** the size classes are plain width and height thresholds read from the window, not an added adaptive library ([ADR 0003](../../../docs/adr/0003-dependency-minimization.md)).
- **No change to data, backup, export, copy or colours.** Orientation stays free; the manifest is unchanged.

## Capabilities
- **Modified Capability:** `platform-android` (adaptive navigation and content width)

## Impact
- **Affected code:** `:app` only: `AppContent.kt` (scaffold chooses bar or rail), `AppChrome.kt` (a rail next to the existing bar), a small window-size helper in `ui/common`, and the content cap applied once around the tab content. Screens themselves are not rewritten.
- **Tests:** unit tests for the size thresholds; an emulator pass on portrait phone, landscape phone and a tablet-sized window (light and dark, English and Dutch).
- **Docs:** README screenshots for the rail, `docs/dev/` note on the thresholds, CHANGELOG.

## Out of scope
- A two-pane History (list next to chart or detail) and other layouts that change what is on screen at once; recorded as a follow-up.
- Foldable posture handling (tabletop, hinge avoidance).
- A different top bar on large screens.
