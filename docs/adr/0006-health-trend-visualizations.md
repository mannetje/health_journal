# 6. Health Trend Visualizations

- **Date:** 2026-09-28
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Users could only see their Weight, Blood Pressure, and Glucose measurements as a flat, reverse-chronological list on the History screen. There was no way to spot a trend (e.g. "is my blood pressure improving?") without mentally comparing many rows. [`openspec/changes/archive/2026-09-28-add-health-trend-visualizations/`](../../openspec/changes/archive/2026-09-28-add-health-trend-visualizations/) specified per-metric trend visualizations, attached to the existing History filter chips rather than a separate "Trends" tab, so the feature stays discoverable without adding new navigation.

## Functionally, what changed

When the user selects the **Weight**, **Blood Pressure**, or **Glucose** filter chip on the History screen (not **All** or **Activity**), a trend card now appears above the entry list:

- **Shared date-range selector:** a segmented control (7 days / 30 days / 90 days / All) filters every chart and statistic on the screen; it defaults to 30 days.
- **Weight:** a line chart of raw weight readings plus a configurable moving average (3/5/7/10 entries, default 5, UI-only — not persisted), with Latest/Change/Min/Max stat chips and the latest NHG BMI category badge.
- **Blood Pressure:** an overlaid systolic/diastolic line chart, an average-reading summary with NHG-category-colored gauge bars (showing where the average sits between clinically meaningful low/high bounds), min/max stat chips, and a breakdown of how many readings fall into each of the three blood pressure bands (Normal, High, Seriously raised).
- **Glucose:** a single-color trend line (out-of-range excursions are conveyed by the Time-in-Range breakdown and the color-coded "Latest" stat chip rather than per-point line coloring — see Trade-offs), a Time-in-Range breakdown split by measurement context (Fasting vs. Postprandial), and Latest/Average/Time-in-Range%/Entry-count stat chips.
- All charts support **pinch-to-zoom and drag-to-pan** to inspect a narrower slice of the visible range.
- Every chart shows a localized "not enough data yet" message instead of an empty canvas when fewer than two entries are available.
- All new UI text is fully localized (English/Dutch, reusing the existing [Dutch/English localization](../../openspec/changes/archive/2026-09-28-add-localization/proposal.md) mechanism) and all colors follow the existing NHG category palette and the light/dark theme, so a chart never introduces a new hardcoded color.

## Technically, how it was built

- **Charting library: [Vico](https://github.com/patrykandpatrick/vico) 3.2.3** (`com.patrykandpatrick.vico:compose` + `compose-m3`), approved as a documented exception to [ADR 0003 (Dependency Minimization)](0003-dependency-minimization.md#approved-exceptions) (see that ADR for why 3.2.3 and not a newer 3.3.x release). Adopting Vico required raising `app`'s `compileSdk` from 35 to 36, since Vico declares `minCompileSdk 36` on every 3.x release; `targetSdk` (35) and `minSdk` (26) are unaffected — `compileSdk` only controls which platform APIs the module compiles against, not runtime device compatibility. Charts were originally hand-drawn with Compose `Canvas` plus a custom `detectTransformGestures` pan/zoom handler; that approach was replaced on 2026-09-29 because the hand-rolled pixel/gesture math had become more code to maintain than adopting a small, Compose-native, MIT-licensed library.
- **New package** `app/src/main/java/nl/healthjournal/app/ui/history/charts/`:
  - `ChartPrimitives.kt` — `ChartPoint`/`ChartSeries` data types, the `LineTrendChart` composable (wraps Vico's `CartesianChartHost` with a `CartesianChartModelProducer`, `rememberLineCartesianLayer`, `VerticalAxis`/`HorizontalAxis`, and a `CartesianValueFormatter` that maps x-values back to `Instant`s for date-axis labels via an `ExtraStore` extra), and a `movingAverage()` helper. Pan and pinch-to-zoom are handled natively by `CartesianChartHost` (`rememberVicoScrollState`/`rememberVicoZoomState`) — no gesture-detector code required. Theming flows from `MaterialTheme.colorScheme` via `compose-m3`'s `rememberM3VicoTheme()` inside a `ProvideVicoTheme` wrapper, so charts stay light/dark-theme-aware without any hardcoded colors.
  - `TrendBuildingBlocks.kt` — the shared `TrendCard`, `StatChip`/`StatChipRow`, `DistributionSegment`/`CategoryDistributionBar`, `DateRangeSelector`, and `GaugeBar` composables reused by all three metrics.
  - `TrendDateRange.kt` — the `TrendDateRange` enum (7/30/90/All days) and a generic `List<T>.filterByDateRange(range) { timestampOf }` extension.
  - `WeightTrendSection.kt`, `BloodPressureTrendSection.kt`, `GlucoseTrendSection.kt` — the per-metric composables that assemble the shared building blocks using each metric's domain model and NHG classification rules (`NhgBmiCategory`, `NhgBloodPressureCategory`, `NhgGlucoseCategory`).
- **Color reuse, not duplication.** `getBmiColor`/`getBpColor`/`getGlucoseColor` were extracted from `LogMetricScreen.kt` (where they were private, duplicated implicitly per screen) into a new shared, public `app/src/main/java/nl/healthjournal/app/ui/nhg/NhgColors.kt`, so the Log screen's badges and the new trend charts always render the same category exactly the same color in both themes.
- **State:** `HistoryViewModel`'s `HistoryUiState` gained `selectedDateRange: TrendDateRange` (default `THIRTY_DAYS`) and a `setDateRange()` action; `HistoryScreen.kt` renders the date-range selector and the matching `*TrendSection` above the existing `LazyColumn` whenever a single, non-Activity metric filter is active, narrowing the data passed to each chart via `filterByDateRange`.
- **Domain layer untouched.** All classification thresholds (`NhgBmiCategory.classify`, `NhgBloodPressureCategory.classify`) already existed in `:domain`; this feature only reads them from `:app`, honoring the Hexagonal boundary from [ADR 0002](0002-hexagonal-architecture.md).

## Consequences

### Positive
- Users get an at-a-glance trend view without leaving the History screen or learning new navigation.
- Category colors and NHG thresholds stay centralized and consistent across the Log and History screens.
- Pan/pinch-zoom, axis layout, and date-label formatting are handled by a well-tested library instead of bespoke pixel/gesture math, reducing future maintenance burden.

### Negative / Trade-offs
- Vico is a new third-party dependency (a small APK size increase and an external release cadence to track), an explicitly approved exception to ADR 0003 — see [ADR 0003's Approved Exceptions](0003-dependency-minimization.md#approved-exceptions) for the justification.
- The glucose chart's line is now a single color instead of per-point NHG-category coloring: Vico's line-styling primitives (`PointProvider`, `LineFill.colorScale`) style points individually or by a continuous value-scale gradient, neither of which cleanly reproduces the previous per-segment coloring against the app's discrete NHG category thresholds. Out-of-range excursions remain visible via the Time-in-Range distribution bars and the color-coded "Latest" stat chip, so no clinical information is lost, but the line itself no longer visually flags individual excursions.
- Scope remained intentionally limited to what the spec required (no HbA1c estimation, no goal-weight lines, no chart export, no tooltips).
- The blood-pressure gauge bar's display range (80–200 mmHg systolic, 40–120 mmHg diastolic) is a presentation-only choice, not a domain constant — chosen because the existing `BloodPressureReading` min/max (40–300/20–200) represent physiologic extremes, not a useful visual scale.

## Update (2026-09-29): rendering fixes and layout

Verifying the charts on an emulator found several defects, fixed in `ChartPrimitives.kt` and `HistoryScreen.kt`:

- **X-axis unit:** x-values are whole hours since the earliest point instead of seconds. Vico derives its x step from the GCD of the x deltas, so seconds gave a 1-second step, a tiny default zoom window and identical axis labels. The axis item placer's spacing is counted in that step, so label spacing is computed from the GCD of the hour deltas.
- **Zoom:** the chart opens with `Zoom.Content`, showing the whole selected range.
- **Y-range:** a custom `CartesianLayerRangeProvider` fits the y-axis to the data with 10% padding. Vico's default always includes 0, which flattened weight and BP lines. The y-axis shows about 5 labels with one decimal.
- **Layout:** the date-range selector and trend section are now the first item of the History `LazyColumn`, so the chart scrolls together with the entries below it. Entries under a single-metric filter follow the selected date range; "All" still lists everything.
- **Moving-average chips** in the weight card scroll horizontally so the 10-day chip is no longer squeezed.

## Update (2026-09-29): "All" and Activity filters

The History **All** filter now merges weight, blood pressure, glucose and activity entries into one chronological list, and an **Activity** chip shows activity sessions alone. The trend chart and date-range selector still appear only for the single-metric Weight, Blood Pressure and Glucose filters. See [ADR 0010](0010-responsive-dutch-ui-layout.md) for the data flow. The History title and the Import/Export buttons were also re-laid-out so they stay aligned in Dutch.

## Update (2026-09-30): relative date ranges and zoom-aware axis labels

- **Range anchored to the data:** `filterByDateRange` measures 7/30/90 days back from the newest entry of the list, not from `Instant.now()`. Imported historical data older than the window used to leave the chart on "not enough data". An empty list gives an empty list. If the window holds fewer than two entries (sparse data: the newest entry is months after the one before it), it is widened back to the second-newest entry, otherwise the chart would still show the empty state. This applies to Weight, Blood Pressure and Glucose alike, because `HistoryScreen` calls it for all three (and the entry lists follow the same window).
- **Zoom-aware calendar axis:** `LineTrendChart` (shared by all three charts) uses `CalendarItemPlacer` (`AxisTicks.kt`), which puts labels and guidelines on calendar boundaries and picks the step from the span currently visible, keeping at most 7 labels across the screen: 6/12 hours, 1/2 days, 1/2 weeks (Mondays), 1/2/3/6 months (1st of the month), 1/2/5 years. Zooming in adds finer ticks, zooming out merges them, live while pinching or panning. Labels: `2026` for years, `Jan 2026` for months (never a bare 1 Jan), `d MMM` for days and weeks, with the year added on the first label and on the first label after a new year starts (`1 Jan 2026`). Vico gives value formatters no visible range, so the placer stores its decision in an `AxisTickState` that the formatter reads. The rules are pure functions (`chooseTickStep`, `calendarTicks`, `formatTick`), unit-tested in `TrendChartLogicTest`.

## Update (2026-10-03): neutral range colours

Category colours no longer use the green, yellow, orange and red traffic-light palette. They follow a neutral five-step blue-grey ramp (light and dark variants, label contrast at least 4.5:1): a darker step is a higher band and never a verdict. Blood pressure uses steps 1, 3 and 4, BMI steps 0 to 3, glucose steps 0 to 3. The blood pressure distribution groups entries by the three bands and wraps long labels. See `openspec/changes/reword-range-labels/`.
