# Design: Health Trend Visualizations

## Context
`HistoryScreen.kt` today has four filter chips (All/Weight/BP/Glucose) that just narrow a `LazyColumn` of cards. There is no aggregation, no chart, and no derived statistic anywhere in the presentation layer. `HistoryViewModel` already loads full history via `GetHealthHistoryUseCase`, so all data needed for trend rendering is already available client-side — this is purely a presentation-layer addition.

The user supplied three screenshots as style references:
1. The app's current (plain) History screen — light Material 3, card list.
2. A dark-themed "Graph" tab overlaying SYSTOLIC/DIASTOLIC/PULSE/Weight as colored lines over a date range, with a bottom pan/zoom scrubber (MyHeart app).
3. A dark-themed "Average blood pressure" summary card (big number + category + gradient SYS/DIA gauge bars), a SYS/DIA trend line chart, and a "Measurement distribution" section with percentage bars per category and Max/Min stats (MyHeart app, different tab).

## Goals
- One coherent "Trends" experience across Weight, Blood Pressure, and Glucose — same card shapes, spacing, typography, and color semantics — even though the inspiration comes from three different apps.
- Reuse the NHG category color mapping that already exists in `LogMetricScreen.kt` (`getBmiColor`, `getBpColor`, `getGlucoseColor`) so a "High Normal" blood pressure reading is the same color on the Log screen, the History list, and the new Trends chart.
- Stay within the app's own Material 3 `HealthJournalTheme` (light/dark aware) instead of hard-coding the reference apps' dark palettes.
- No new dependency: implement charts as custom Compose `Canvas` drawings.

## Non-goals
- No HbA1c estimation (mySugr-style) — out of scope; the domain has no HbA1c model today and would need real clinical modeling to add responsibly.
- No goal-weight lines or weight-loss projections (Libra also offers this) — out of scope for this change; can be a follow-up once a "goal" concept exists in the Profile domain.
- No PDF/report export of charts — the existing CSV export already covers data portability; charts remain in-app only for now.
- No changes to the underlying domain/data modules; this is purely `:app` presentation work.

## Trends placement (resolved)
No separate "Trends" tab or top-level nav destination. The existing History filter chips gain the behavior directly:
- **Weight / BP / Glucose selected (one at a time)**: the chart + stat summary for that metric renders above the existing reverse-chronological card list for that metric. The list stays — the chart is additive, not a replacement.
- **All selected**: behaves exactly as today — flat list of every metric type, no chart. Mixing metrics with different units/axes into one chart would not read cleanly, so this is an explicit non-goal rather than an oversight.
- **Activity selected**: no chart in this change (no reference app/design was requested for it); flat list as today.

This means `HistoryViewModel` needs to expose trend-chart input data (already-loaded history, filtered/derived per metric) only when a single metric filter is active, and `HistoryScreen` conditionally renders the relevant `*TrendSection` composable above the `LazyColumn` based on the active filter.

## Shared building blocks (new, reused across all three metrics)
- `TrendCard` — a Material 3 `Card` wrapper with the consistent title/subtitle/content padding used by all three trend sections.
- `LineTrendChart` — a Canvas-based composable that draws one or more colored polylines against a time x-axis and value y-axis, with horizontal drag-to-scroll and pinch-to-zoom over the visible date range (mirrors reference screenshot 2's scrubber, implemented as a Compose `Modifier.pointerInput` drag/transform gesture rather than a fixed slider bar).
- `CategoryDistributionBar` — a horizontal stacked/segmented bar (or a small ordered list of percentage bars) showing what fraction of readings fall into each NHG category, colored with the shared category color functions (mirrors reference screenshot 3's "Measurement distribution").
- `StatChipRow` — a row of small stat chips (e.g. Latest / Average / Max / Min) shown under each chart.
- A shared date-range selector: chip options 7 days / 30 days / 90 days / All, **defaulting to 30 days**, so the chart opens showing a meaningful recent trend but the user can always widen it (this was an open question — resolved: default 30, not "All", since "All" can be visually noisy for a long-running log and the user confirmed 30-day-default-but-extendable is the right behavior).

## Per-metric design

### Weight (Libra-style)
- `LineTrendChart` with a single weight series plus a computed moving-average overlay line (simple N-point rolling average). **N is user-configurable** via a small segmented control (options: 3 / 5 / 7 / 10 readings) next to the chart, **defaulting to 5** — a sane middle ground that smooths day-to-day noise without lagging too far behind a real trend change. The chosen window is a UI-only, in-memory preference (no new persistence requirement in this change).
- `StatChipRow`: Latest, Change vs. previous entry, Min, Max (over selected range).
- Latest point annotated with its NHG BMI category badge (reusing `getBmiColor`), matching the badge already shown on the Log screen.
- No goal-line (see Non-goals).

### Blood Pressure (MyHeart-style)
- `TrendCard` "Average blood pressure" summary: big `SYS/DIA` number, NHG category label, and two horizontal gradient gauge bars (SYS, DIA) showing where the average sits between "Optimal" and "Hypertension Grade 3", reusing the existing NHG blood-pressure threshold values from `NhgBloodPressureCategory`.
- `LineTrendChart` with two overlaid series (systolic in one accent color, diastolic in another), pan/zoom over the date range.
- `CategoryDistributionBar` showing % of readings per NHG category across the selected range (e.g. "High Normal 67%, Stage 1 Hypertension 33%"), plus Max/Min stat chips.

### Glucose (mySugr/LibreLink/Glucobyte-style)
- `LineTrendChart` with a single glucose series whose line/point color changes per-segment based on the NHG category of each reading (reusing `getGlucoseColor`), so out-of-range excursions are visually obvious without a legend.
- A **Time-in-Range** `CategoryDistributionBar` variant: % of readings classified `NORMAL` ("in range") vs. all other categories ("out of range"), split by `GlucoseContext` (fasting vs. postprandial), since NHG target ranges differ per context.
- `StatChipRow`: Latest, Average, Time-in-Range %, count of readings in the selected range.
- No HbA1c estimate (see Non-goals).

## Theming
All new composables (`TrendCard`, `LineTrendChart`, `CategoryDistributionBar`, `StatChipRow`, the gauge bars) read colors exclusively from `MaterialTheme.colorScheme` and the shared NHG category color functions — never a hard-coded `Color(0x...)` literal — so they render correctly once `add-dark-theme` lands `DarkColorScheme`. This change does not duplicate or fork theme decisions; it depends on `add-dark-theme` for the actual light/dark color tokens.

## Localization
Every string introduced by this change (chart titles, stat-chip labels like "Latest"/"Average"/"Time in Range", the date-range chip labels, empty-state "not enough data yet" messages, and the moving-average control's labels) is written as a string resource from the start (`stringResource(R.string.trend_...)`), per `add-localization`. NHG category names shown on badges/distribution bars reuse that change's per-enum `label()` extensions rather than introducing a second mapping.

## Glucose thresholds: NHG and DVN cross-check
The existing `NhgGlucoseCategory` thresholds (`domain/src/main/kotlin/nl/healthjournal/domain/model/nhg/NhgGlucoseCategory.kt`) were checked against Diabetesvereniging Nederland's patient-facing glucose pages ([dvn.nl/diabetes/bloedwaarden/glucosewaarden](https://www.dvn.nl/diabetes/bloedwaarden/glucosewaarden), [.../hba1c](https://www.dvn.nl/diabetes/bloedwaarden/hba1c)) as requested. They align closely:

| | App (`NhgGlucoseCategory`) | DVN |
|---|---|---|
| Fasting normal | ≤ 6.0 mmol/L | < 6.1 mmol/L |
| Fasting impaired | 6.0–6.9 mmol/L | 6.1–7.0 mmol/L |
| Fasting diabetes range | > 6.9 mmol/L | > 7.0 mmol/L |
| Postprandial normal | < 7.8 mmol/L | < 7.8 mmol/L |
| Postprandial impaired | 7.8–11.0 mmol/L | 7.8–11.0 mmol/L |
| Postprandial diabetes range | > 11.0 mmol/L | > 11.0 mmol/L |
| Hypoglycaemia | < 3.5 mmol/L | ≤ 3.9 mmol/L |

Note DVN's own page does not itself cite NHG as its source, so it is being used here as a secondary, patient-facing confirmation rather than the primary clinical source — the app's thresholds should still be considered NHG-derived first.

One discrepancy is flagged, not silently changed: the app's hypoglycaemia cutoff (3.5 mmol/L) is lower than DVN's stated 3.9 mmol/L. This is an existing value in `health-metrics/glucose`, out of scope for a chart-only proposal to change unilaterally — flagging it here for a separate, explicit decision (and possible `health-metrics/glucose` spec change) rather than adjusting it as a side effect of adding charts.

## Alternatives considered
- **Adopt a charting library (e.g. Vico, MPAndroidChart)**: rejected to stay compliant with ADR 0003 (Dependency Minimization) and because the required chart types (overlaid lines, gauge bars, distribution bars) are all achievable with plain Compose `Canvas` without excessive complexity.
- **Literally reproduce each reference app's dark theme per metric**: rejected — would fragment the app's visual identity into three inconsistent sub-themes. Instead we borrow the *information layout* (what's shown, how it's grouped) from each reference app, but render it through the app's single existing Material 3 theme and shared color/typography tokens.
- **One universal chart type for all three metrics**: rejected — weight, blood pressure, and glucose have genuinely different clinically-relevant summaries (a single trend line vs. two overlaid vitals vs. a time-in-range framing), so per-metric composables are used, sharing only the lower-level building blocks (`TrendCard`, `LineTrendChart`, `CategoryDistributionBar`, `StatChipRow`).
- **A dedicated "Trends" tab/nav destination**: rejected in favor of attaching the chart to the existing per-metric filter chips — avoids adding a 4th/5th nav concept for what is really just "more detail about the metric you already selected."
