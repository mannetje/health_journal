# Add Glucose Graphs

## Why
The Glucose graph view shows one trend line and a two-segment in-range bar. It does not show where the readings sit against the NHG limits, how they spread over the day, how they changed over time, or how the last month compares with the one before.

## What Changes
- **Trend with NHG lines:** fasting and post-meal readings as two lines, points coloured by NHG category (neutral ramp), reference lines at 3.9 (low), the fasting limit and the post-meal limit.
- **Time in range per NHG category:** for fasting and for post-meal, the share of readings in each NHG category as bars.
- **By time of day:** average, lowest and highest reading and the count for night (0-6), morning (6-12), afternoon (12-18) and evening (18-24).
- **Statistics:** lowest and highest reading with dates, the change of the newest fasting and post-meal reading over a month, three months, six months and year (dash when the history is shorter), and spread (average, standard deviation, range) per context.
- **Last 30 days against the 30 before:** average and share in range per context, with the change.
- **Largest rise:** the biggest increase between two consecutive readings less than 3 hours apart.
- **Neutral wording and languages:** figures only; every text in English and Dutch. The list view and the existing trend statistics stay.

## Capabilities
- **Modified Capability:** `health-trends` (glucose graphs)

## Impact
- **Database:** none.
- **Affected code:** a pure `GlucoseStatistics` with JVM tests; `GlucoseTrendSection` (lines, coloured points); `GlucoseStatisticsSection`; strings in English and Dutch.
- **Docs:** README, CHANGELOG, `docs/dev/design.md`, `docs/dev/testing.md`, the `health-trends` spec.

## Non-goals
- No day overlay chart, no per-reading notes or meal icons on the chart.
- No change to how glucose is stored or classified.
