# 6. Health Trend Visualizations

- **Date:** 2026-09-28
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Users could only see their Weight, Blood Pressure, and Glucose measurements as a flat, reverse-chronological list on the History screen. There was no way to spot a trend (e.g. "is my blood pressure improving?") without mentally comparing many rows. [`openspec/changes/add-health-trend-visualizations/`](../../openspec/changes/add-health-trend-visualizations/) specified per-metric trend visualizations, attached to the existing History filter chips rather than a separate "Trends" tab, so the feature stays discoverable without adding new navigation.

## Functionally, what changed

When the user selects the **Weight**, **Blood Pressure**, or **Glucose** filter chip on the History screen (not **All** or **Activity**), a trend card now appears above the entry list:

- **Shared date-range selector:** a segmented control (7 days / 30 days / 90 days / All) filters every chart and statistic on the screen; it defaults to 30 days.
- **Weight:** a line chart of raw weight readings plus a configurable moving average (3/5/7/10 entries, default 5, UI-only — not persisted), with Latest/Change/Min/Max stat chips and the latest NHG BMI category badge.
- **Blood Pressure:** an overlaid systolic/diastolic line chart, an average-reading summary with NHG-category-colored gauge bars (showing where the average sits between clinically meaningful low/high bounds), min/max stat chips, and a breakdown of how many readings fall into each NHG category (Optimal → Hypertension Grade 3).
- **Glucose:** a single-color trend line (out-of-range excursions are conveyed by the Time-in-Range breakdown and the color-coded "Latest" stat chip rather than per-point line coloring — see Trade-offs), a Time-in-Range breakdown split by measurement context (Fasting vs. Postprandial), and Latest/Average/Time-in-Range%/Entry-count stat chips.
- All charts support **pinch-to-zoom and drag-to-pan** to inspect a narrower slice of the visible range.
- Every chart shows a localized "not enough data yet" message instead of an empty canvas when fewer than two entries are available.
- All new UI text is fully localized (English/Dutch, reusing the existing [Dutch/English localization](../../openspec/changes/add-localization/proposal.md) mechanism) and all colors follow the existing NHG category palette and the light/dark theme, so a chart never introduces a new hardcoded color.

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
