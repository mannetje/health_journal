# Add Blood Pressure Graphs

## Why
The blood pressure graph view shows the trend line, the average with its gauges, min/max and the band distribution. It cannot show where the readings sit against the NHG bands, how much they vary from one reading to the next, or whether pulse, mean arterial pressure and pulse pressure moved compared with the period before. The line chart also gives every reading the same marker, so the band of a reading is only visible in the list.

## What Changes
- **Band markers:** the points on the blood pressure line chart are drawn in the colour of the band of their reading (the existing neutral ramp), with the line colour as outline.
- **Blood pressure graph:** a scatter chart with diastolic across and systolic up, the three NHG bands as shaded areas and every reading in the selected range as a dot. Newer readings are more prominent. The axes cover at least 40-120 diastolic and 80-180 systolic and widen for outliers.
- **How steady are your readings:** the usual change (median) and the biggest jump between two consecutive readings, systolic and diastolic, over the selected range. Needs two readings.
- **Compared to the previous period:** for systolic, diastolic, pulse, mean arterial pressure (MAP) and pulse pressure the newest value, the range of the period and the change of the average against the period of the same length before it. A change under 2 mmHg (bpm for pulse) is shown as "stable". With the range All, or without an earlier period, the change is a dash. Pulse is only shown when readings have one.
- **Where:** all of it in the blood pressure graph view, under the existing chart. The list view and the entry data are unchanged.
- **Neutral wording:** figures only; no target, no good or bad colour, no praise or warning text.
- **Languages:** every label, hint and content description exists in English and Dutch.

## Capabilities
- **Modified Capability:** `health-trends` (blood pressure graphs)

## Impact
- **Database:** none.
- **Affected code:** a pure `BloodPressureStatistics` (window, steadiness, metric summaries, scatter axes) with JVM tests; `BloodPressureStatisticsSection` (scatter, steadiness, comparison); per-point marker colours in `LineTrendChart`; strings in English and Dutch.
- **Dependencies:** none (Vico point provider and Compose `Canvas`).
- **Docs:** README, CHANGELOG, `docs/dev/design.md`, `docs/dev/testing.md`, the `health-trends` spec, screenshots.

## Non-goals
- No context insights (tags, weather): the app does not record them.
- No sparklines in the comparison card.
- No targets, thresholds beyond the existing NHG bands, or advice.
