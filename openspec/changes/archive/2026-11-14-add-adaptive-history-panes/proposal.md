# Add adaptive History panes

## Why
[add-adaptive-layouts](../2026-11-14-add-adaptive-layouts/proposal.md) gives wide windows a navigation rail and a centred 840 dp column, and left a two-pane History as a follow-up. On a tablet the History tab still shows one thing at a time: the entries, or the chart behind the graph toggle. With a metric selected, a wide window has room for both, and the chart and the entries it is drawn from belong together (the date range already drives both).

## What Changes
- **Entries and chart side by side** on windows at least 900 dp wide when a metric with a chart is selected (Weight, Blood pressure, Glucose, Waist, Migraine, Oxygen): the entry timeline on the left, the chart and statistics on the right. The range chips stay above both.
- **No graph/list toggle** in that layout, since both are visible. On narrower windows the toggle and the single pane stay exactly as they are.
- **Wider cap for History:** with two panes the History content is capped at 1240 dp instead of 840 dp. Other tabs keep 840 dp.
- **"All" and "Activity"** have no chart and stay a single list, capped at 840 dp and centred.
- **Migraine day from the chart** (tap a day on the migraine graph) filters the entry list next to the chart instead of switching to the list.
- **No change to data, backup, export, copy or colours; no new library or permission.**

## Capabilities
- **Modified Capability:** `platform-android` (History panes on wide windows)

## Impact
- **Affected code:** `:app` only: `HistoryScreen.kt` (list and chart become two panes that are shown separately or side by side), `AppContent.kt` (passes the pane mode and the wider cap), the window helper in `ui/common`.
- **Tests:** unit tests for the pane threshold and the cap; an emulator pass on a tablet-sized window, light and dark.
- **Docs:** screenshots, `docs/dev/adaptive-layouts.md`, CHANGELOG.

## Out of scope
- A detail pane for a single entry (entries are cards with in-place edit and delete, there is no detail screen).
- Foldable posture handling.
