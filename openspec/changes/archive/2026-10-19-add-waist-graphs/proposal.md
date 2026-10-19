# Add Waist Graphs

## Why
Waist circumference (middelomtrek) is logged and classified, but the History tab only lists the entries. There is no graph view, no overview of how the waist changed, and nothing that relates the waist to the height or the weight, although both are stored.

## What Changes
- **Graph view:** under the Waist filter an icon in the History title row switches between the entries and the graph, like Weight, Blood Pressure and Glucose.
- **Waist trend:** a line chart of the waist in cm over the selected range. When the profile has a sex, two reference lines show the NHG limits (increased risk from 94 cm for men and 80 cm for women, high risk from 102 cm and 88 cm). The points take the colour of their band (the existing neutral ramp).
- **Waist statistics:** lowest and highest waist with their dates, and the change over the last month, three months, six months and year in cm and percent (a dash when the history is shorter), from all waist entries.
- **Distance to the limits:** the newest waist minus each NHG limit in cm. Needs a sex in the profile.
- **Waist-to-height ratio:** the waist divided by the height as a percentage, as a trend line with a reference line at 50 % (the waist at half the height, a common rule of thumb, labelled as such and not as an NHG limit). Needs a height in the profile.
- **Waist against weight:** a scatter chart with the weight across and the waist up. Every waist entry is paired with the nearest weight entry within 3 days; with at least 5 pairs it shows a least-squares trend line and one neutral sentence ("about 0.8 cm of waist per kg").
- **Missing profile data:** without a height the ratio card, and without a sex the limits and distance, are replaced by one short note that says what to fill in on the Profile screen. Nothing else is hidden.
- **Neutral wording:** figures only; no good or bad colour, no advice, no warnings.
- **Languages:** every label, hint, note and content description exists in English and Dutch.

## Capabilities
- **Modified Capability:** `health-trends` (waist graphs)

## Impact
- **Database:** none.
- **Affected code:** a pure `WaistStatistics` (limits, extremes, change, distance, ratio, pairing, regression) with JVM tests; `WaistTrendSection` and `WaistStatisticsSection`; the History graph switch; strings in English and Dutch.
- **Dependencies:** none.
- **Docs:** README, CHANGELOG, `docs/dev/design.md`, `docs/dev/testing.md`, the `health-trends` spec, screenshots.

## Non-goals
- No combined weight and waist line chart, no waist against BMI.
- No ratio risk bands: the NHG guidance uses the cm limits only.
- No change to how waist entries are stored, classified or exported.
