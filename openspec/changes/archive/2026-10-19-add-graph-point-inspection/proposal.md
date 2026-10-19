# Add Graph Point Inspection

## Why
The trend charts can be panned and zoomed, but a single reading cannot be read off: you cannot see the exact value and date of a point, or how two readings differ, without going to the list view.

## What Changes
- **Select an entry:** tapping a trend chart selects the nearest entry. A vertical line marks it and a label shows its date, time and value (with the unit).
- **Entries only:** only real entries can be selected. The line snaps to an entry; reference lines (NHG limits), moving averages and any interpolated position cannot be selected.
- **Compare two entries:** with one entry selected, tapping a second entry shows both lines and the difference between them: the change in value, the percentage and the time between them. Tapping a third entry starts over with that entry; tapping the selected entry again clears the selection.
- **Neutral wording and languages:** figures only, no good or bad colour; every text in English and Dutch. Pan and zoom keep working.
- **Where:** every chart that uses the shared line chart: weight, blood pressure (systolic and diastolic together), glucose (fasting and post-meal each snap to their own entries), waist, waist-to-height ratio.

## Capabilities
- **Modified Capability:** `health-trends` (point inspection)

## Impact
- **Database:** none.
- **Affected code:** `LineTrendChart` and `ChartSeries` (which series are selectable, a marker with a guideline); a pure `PointSelection` (nearest entry, difference between two) with JVM tests; strings in English and Dutch.
- **Docs:** README, CHANGELOG, `docs/dev/graphs.md`, `docs/dev/testing.md`, the `health-trends` spec.

## Non-goals
- No dragging a range, no values at positions between entries, no selecting the scatter chart (waist against weight) in this change.
- No new colours: the selection uses the existing theme colours.
