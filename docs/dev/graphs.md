# Graphs

How the History graph view is built, using two worked examples: the **waist** graphs and the **glucose** graphs. Read it before adding or changing a graph. The rules the graphs follow (figures only, neutral wording, NHG limits in one place) come from the OpenSpec capability `health-trends` (`openspec/specs/health-trends/spec.md`) and [ADR 0006](../adr/0006-health-trend-visualizations.md).

## The idea in one picture

Every metric has the same three layers, so a graph is easy to test and easy to find:

| Layer | What it is | Example (glucose) |
|-------|------------|-------------------|
| Statistics | Pure Kotlin on plain values. No Compose, no Android, tested on the JVM. | `GlucoseStatistics.kt` |
| Section | Composables that turn statistics into cards, tables and charts. | `GlucoseStatisticsSection.kt` |
| Trend | The line chart at the top, built from `ChartSeries`. | `GlucoseTrendSection.kt` |

`HistoryScreen.kt` shows the trend and the statistics section for the selected filter (`HistoryFilter.GLUCOSE -> { GlucoseTrendSection(...); GlucoseStatisticsSection(...) }`). The list view is untouched.

All of this lives in `app/src/main/java/nl/healthjournal/app/ui/history/charts/`.

## The building blocks

| Block | File | Use it for |
|-------|------|------------|
| `LineTrendChart(series, emptyMessage)` | `ChartPrimitives.kt` | A Vico line chart with pan and zoom. Needs at least two points in total. |
| `ChartSeries(points, color, strokeWidthDp, pointColors, selection)` | `ChartPrimitives.kt` | One line. `pointColors` (parallel to `points`) draws a dot per entry in that colour. `selection` makes its entries tappable. |
| `SeriesSelection(name, format)` | `ChartSelection.kt` | Optional name and a function that formats a value with its unit, shown when an entry is selected. Leave `selection` null on limit lines and averages: they are not selectable. |
| `PointSelection` | `PointSelection.kt` | Pure logic: `nearest` entry in time (newest on a tie), `next` selection (select, compare, start over, clear) and `difference`. |
| `TrendCard`, `StatChipRow`, `CategoryDistributionBar` | `TrendBuildingBlocks.kt` | Card frame, row of figures, bar of category shares. |
| `ChangePeriod` | `WeightStatistics.kt` | Month, 3 months, 6 months, year for "change over a period". |
| `getGlucoseColor`, `getWaistColor` | `ui/nhg/NhgColors.kt` | Category colours, so a band has one colour everywhere. |
| `label()` | `ui/nhg/NhgLabels.kt` | Localised category names. |

Three tricks worth knowing:

- **Selecting entries.** `LineTrendChart` handles taps itself. Vico only reports taps when a marker is set, so the chart sets an invisible one, a `TapSelectionController` turns each tap into an x-value, `PointSelection.nearest` snaps it to a real entry of a series that has `selection`, and the selected entries are drawn as persistent guideline markers. The text (date, value, difference) is a Compose `SelectionSummary` under the chart, so it is translated, wraps and is read by TalkBack. To make a new chart selectable, give its data series a `SeriesSelection`.
- **Horizontal reference lines.** `LineTrendChart` has no "limit line" feature. A limit is a series with two points (first and last timestamp) at the same value, drawn thin (`strokeWidthDp = 1.5f`). Glucose draws three (3.9, 6.0 and 7.8 mmol/L); waist draws two.
- **A dot per entry.** Pass `pointColors`. For band-coloured points use the category colour of each entry; for a plain dot use one colour for all (`rawPoints.map { rawColor }` on the weight chart).

## Example 1: waist graphs

Change: `openspec/changes/archive/` (`add-waist-graphs`). Files: `WaistStatistics.kt`, `WaistTrendSection.kt`, `WaistStatisticsSection.kt`, test `WaistStatisticsTest.kt`.

1. **Decide the figures first** (the proposal): NHG limit lines, extremes, change per period, distance to the limits, waist-to-height ratio, and waist against weight.
2. **Put the limits in one place.** `WaistLimits.forSex(sex)` returns the increased-risk and high-risk limits. A test (`limits equal the NHG classification`) checks them against `NhgWaistCircumferenceCategory.classify`, so the lines can never drift from the colours.
3. **Write the maths as pure functions** in `WaistStatistics`: `changeOver(period)`, `distanceToLimit`, `heightRatioPercent`, `pair` (each waist with the nearest weight within three days) and `fit` (least squares, needs five pairs with varying weight). Ties for lowest/highest take the newest entry.
4. **Draw the trend** in `WaistTrendSection`: a series for the entries with `pointColors` from the band, plus one limit series per limit. Without a sex in the profile there are no limits and a note points to the Profile.
5. **Build the cards** in `WaistStatisticsSection`. The waist against weight scatter is drawn with a `Canvas` because Vico has no scatter chart: map each value to pixels with `x(w) = left + (w - xMin) / (xMax - xMin) * width`, draw dots, then the fitted line from `LinearFit`.
6. **Test** the pure part only (`WaistStatisticsTest`): boundaries, short history, pairing, the fit.

## Example 2: glucose graphs

Change: `openspec/changes/archive/` (`add-glucose-graphs`). Files: `GlucoseStatistics.kt`, `GlucoseTrendSection.kt`, `GlucoseStatisticsSection.kt`, test `GlucoseStatisticsTest.kt`.

1. **Fasting and post-meal never mix.** They have different NHG limits, so every average and change is per context. The trend shows two lines (fasting, post-meal) with points coloured by the stored NHG category.
2. **Convert at the edge.** Statistics work in mmol/L. The section converts to the display unit (`DisplayUnits.shown`, `shownSigned`), so mg/dL users get correct numbers and limit lines (3.9 / 6.0 / 7.8 mmol/L become 70 / 108 / 140 mg/dL).
3. **Small pure types** carry the results: `GlucoseChange`, `GlucoseSpread` (average, sample standard deviation, range), `GlucosePeriod`, `GlucoseRise`, `TimeOfDayStats`. A figure that cannot be computed is `null` and the screen shows a dash.
4. **Windows are anchored on the newest reading,** not on today, so imported history still shows (`last30Days` and `previous30Days`: the 30 days ending at the newest reading and the 30 before).
5. **The largest rise** looks at consecutive readings less than three hours apart (`RISE_WINDOW_HOURS`) and returns the biggest increase; a tie takes the newest.
6. **Time of day** buckets by local hour: night 0-6, morning 6-12, afternoon 12-18, evening 18-24 (`TimeOfDay.of`).
7. **Test** each rule with fixed dates in UTC (`GlucoseStatisticsTest`): tie breaking, short history, sample SD, bucket edges, window edges, the three-hour limit.

## Checklist for a new graph

1. Propose it: an OpenSpec change with a `health-trends` delta (see [CONTRIBUTING.md](../../CONTRIBUTING.md)).
2. Statistics class and unit test first, in `charts/`.
3. Trend section: `ChartSeries` per line, limit lines as two-point series, `pointColors` for dots, `SeriesSelection` on the entries series.
4. Statistics section: cards from `TrendBuildingBlocks.kt`, `Canvas` for anything Vico cannot draw.
5. Hook both into `HistoryScreen.kt` under the metric's filter.
6. Strings in **both** `values/strings.xml` and `values-nl/strings.xml` (see [Add a translation](how-to/add-a-translation.md)). Wording is neutral: figures, no advice.
7. Update the docs: `design.md` (History row), `testing.md` (new test file), README feature list and `CHANGELOG.md`.

## Pitfalls

- `@Composable` functions (`MaterialTheme.colorScheme`, `getGlucoseColor`, `stringResource`) cannot be called from a plain local `fun` or non-inline lambda. Inline lambdas (`map`, `buildList`, `forEach`) are fine; otherwise compute colours first and pass them in.
- `LineTrendChart` shows the empty message with fewer than two points in total. Guard series construction the way `WaistTrendSection` does.
- Point markers shrink with the length of the series (`markerSizeDp` in `ChartPrimitives.kt`: 9 dp up to 30 points, 3 dp from 300). Do not give long series a fixed marker size, otherwise the dots merge into a thick band.
- Keep selection state keyed on the series' points, not on the `ChartSeries` objects. They hold lambdas (`SeriesSelection.format`), so a rebuilt copy is never equal and would reset the selection on every recomposition.
- Chart x-values are whole hours (`hoursSince`), so entries within the same hour share a position and a tap there selects the earliest of them. They are visually indistinguishable, so this is accepted.
- Do not trust the clock: use the newest entry as "now", and test with fixed dates.
