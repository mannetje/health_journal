# Design: Graph View and Weight Statistics

## Toggle
- State: a per-filter flag kept with `rememberSaveable` in `HistoryScreen` (survives rotation, not stored on disk). The default is the list, so the screen opens as it does today.
- The toggle is an icon button in the title row, left of the three-dot menu. Its description names the view it switches to ("Show graph" / "Show entries"). The icon is a small drawn `ImageVector`, because the core icon set has no chart icon and the extended set is not worth its size.
- Graph view: the timeline rows and the entry list are not composed. List view: the chart is no longer a list item, so the timeline offset (`leading`) becomes 0 for every filter.

## Weight statistics (pure Kotlin, JVM tests)
`WeightStatistics(entries, zone)` over all weight entries, metric (kg); `DisplayUnits` converts at the edge.
- `lowest`, `highest`: smallest and largest weight; on a tie the newest.
- `changeOver(period)`: newest weight minus the weight of the newest entry on or before the start of the period (newest date minus 7 or 15 days, or 1, 3, 6 or 12 calendar months). Null when no entry is that old. Per week: change divided by the real days between the two entries divided by 7. Percent: change divided by the starting weight.
- `monthlyChange(year)`: for each month with entries, the last weight of the month minus the last weight of the previous month that has entries; the first month of data has no bar.
- `yearlyChange()`: the same per year.

## Bar chart
A `BarTrendChart` on the existing chart primitives: a zero line, gain above and loss below it, the value printed at the end of each bar, labels under the bars (month names or years). One neutral colour. The year chips reuse the chip style of the timeline rows and offer only years that have entries.

## Edge cases
- Fewer than 2 entries: lowest and highest only; the table and bar charts show the insufficient-data message of the line chart.
- Several entries on one day: the last of the day counts.
