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
- **Glucose:** a trend line colored per-point by its NHG category (Hypoglycaemia/Normal/Impaired/Diabetes Range), a Time-in-Range breakdown split by measurement context (Fasting vs. Postprandial), and Latest/Average/Time-in-Range%/Entry-count stat chips.
- All charts support **pinch-to-zoom and drag-to-pan** to inspect a narrower slice of the visible range.
- Every chart shows a localized "not enough data yet" message instead of an empty canvas when fewer than two entries are available.
- All new UI text is fully localized (English/Dutch, reusing the existing [Dutch/English localization](../../openspec/changes/add-localization/proposal.md) mechanism) and all colors follow the existing NHG category palette and the light/dark theme, so a chart never introduces a new hardcoded color.

## Technically, how it was built

- **No new dependency.** Per [ADR 0003 (Dependency Minimization)](0003-dependency-minimization.md), charts are hand-drawn with Jetpack Compose's `androidx.compose.foundation.Canvas`, not a third-party charting library. Pan/zoom uses the built-in `Modifier.pointerInput` + `detectTransformGestures` gesture detector — again no external gesture library.
- **New package** `app/src/main/java/nl/healthjournal/app/ui/history/charts/`:
  - `ChartPrimitives.kt` — `ChartPoint`/`ChartSeries` data types, the `LineTrendChart` Canvas composable (draws one or more polylines against a time x-axis / value y-axis with gridlines, axis labels via `rememberTextMeasurer()`, and the pinch/pan gesture handling), and a `movingAverage()` helper.
  - `TrendBuildingBlocks.kt` — the shared `TrendCard`, `StatChip`/`StatChipRow`, `DistributionSegment`/`CategoryDistributionBar`, `DateRangeSelector`, and `GaugeBar` composables reused by all three metrics.
  - `TrendDateRange.kt` — the `TrendDateRange` enum (7/30/90/All days) and a generic `List<T>.filterByDateRange(range) { timestampOf }` extension.
  - `WeightTrendSection.kt`, `BloodPressureTrendSection.kt`, `GlucoseTrendSection.kt` — the per-metric composables that assemble the shared building blocks using each metric's domain model and NHG classification rules (`NhgBmiCategory`, `NhgBloodPressureCategory`, `NhgGlucoseCategory`).
- **Color reuse, not duplication.** `getBmiColor`/`getBpColor`/`getGlucoseColor` were extracted from `LogMetricScreen.kt` (where they were private, duplicated implicitly per screen) into a new shared, public `app/src/main/java/nl/healthjournal/app/ui/nhg/NhgColors.kt`, so the Log screen's badges and the new trend charts always render the same category exactly the same color in both themes.
- **State:** `HistoryViewModel`'s `HistoryUiState` gained `selectedDateRange: TrendDateRange` (default `THIRTY_DAYS`) and a `setDateRange()` action; `HistoryScreen.kt` renders the date-range selector and the matching `*TrendSection` above the existing `LazyColumn` whenever a single, non-Activity metric filter is active, narrowing the data passed to each chart via `filterByDateRange`.
- **Domain layer untouched.** All classification thresholds (`NhgBmiCategory.classify`, `NhgBloodPressureCategory.classify`) already existed in `:domain`; this feature only reads them from `:app`, honoring the Hexagonal boundary from [ADR 0002](0002-hexagonal-architecture.md).

## Consequences

### Positive
- Users get an at-a-glance trend view without leaving the History screen or learning new navigation.
- Zero new third-party dependencies or APK size increase from a charting library.
- Category colors and NHG thresholds stay centralized and consistent across the Log and History screens.

### Negative / Trade-offs
- Hand-rolled Canvas charting is more code to maintain than a library, and lacks library conveniences (e.g. built-in tooltips); scope was intentionally kept to what the spec required (no HbA1c estimation, no goal-weight lines, no chart export).
- The blood-pressure gauge bar's display range (80–200 mmHg systolic, 40–120 mmHg diastolic) is a presentation-only choice, not a domain constant — chosen because the existing `BloodPressureReading` min/max (40–300/20–200) represent physiologic extremes, not a useful visual scale.
