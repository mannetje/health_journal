# Add Health Trend Visualizations

## Why
The History screen (`HistoryScreen.kt`) currently renders every metric as a flat, reverse-chronological list of cards. There is no graph, trend line, or summary statistic anywhere in the app, even though the README's roadmap already (incorrectly) claims "trend visualizations" were delivered in Phase 3. For a user logging weight, blood pressure, and glucose over weeks or months, a list of numbers makes it hard to see whether they are trending up, down, or stable, or how their readings distribute across NHG clinical categories.

The user has pointed to three reference apps whose graphing style should inform this feature, one per metric:
- **Weight** → [Libra](https://play.google.com/store/apps/details?id=net.cachapa.libra): a clean single-metric trend line with a smoothed/moving-average overlay.
- **Blood Pressure** → [MyHeart](https://play.google.com/store/apps/details?id=com.szyk.myheart): an overlaid systolic/diastolic/pulse line graph with pan/zoom, a color-graded average summary card, and a category-distribution breakdown.
- **Glucose** → apps such as mySugr, Glucose Buddy, FreeStyle LibreLink, and Glucobyte: a colour-coded trend line plus a "Time in Range" (TIR) style summary of how many readings fall in/out of the healthy NHG band.

The user also asked that the three graphs feel like one coherent feature ("liefst stijlen op elkaar afstemmen") rather than three unrelated widgets bolted together.

## What Changes
- **No separate "Trends" tab or nav destination.** The existing History filter chips (All/Weight/BP/Glucose/Activity) gain graphing behavior directly: selecting a single metric type (Weight, BP, or Glucose) shows that metric's trend chart and stat summary above its existing list of entries. Selecting **All** (or **Activity**, which has no chart design in this change) shows only the flat list, exactly as today — no chart is rendered when multiple metric types are mixed together, since an overlaid multi-metric chart would not read cleanly.
- **Weight trends** (shown when the Weight filter is selected): a Libra-style line chart of weight over time with a smoothed trend line, latest/min/max stat chips, and an NHG BMI-category badge on the latest point. The moving-average window is user-configurable (a small stepper/segmented control near the chart, e.g. 3/5/7/10 readings) with a sane default of 5.
- **Blood pressure trends** (shown when the BP filter is selected): a MyHeart-style overlaid systolic/diastolic line chart with pan/zoom over the selected date range, an average-reading summary card colored by NHG category, and a category-distribution breakdown (% of readings per NHG category, e.g. "High Normal 67%, Stage 1 Hypertension 33%").
- **Glucose trends** (shown when the Glucose filter is selected): a colour-coded trend line (segment color reflects the NHG category of each reading) plus a Time-in-Range summary (% of readings within the NHG "Normal" band vs. below/above), inspired by mySugr/LibreLink/Glucobyte, split by fasting vs. postprandial context, cross-checked against Diabetesvereniging Nederland's published patient-facing glucose ranges (see `design.md`).
- **Shared design language across all three**: one reusable chart container/card style, one shared date-range selector (default 30 days, extendable to 7/90/All — see `design.md`), and NHG category colors reused from the existing `LogMetricScreen.kt` color-mapping functions (`getBmiColor`, `getBpColor`, `getGlucoseColor`) so trend colors match the colors already shown on the Log and History screens.
- **Depends on `add-dark-theme`**: charts must render correctly in both light and dark mode; this change reuses that change's theme tokens rather than hard-coding colors.
- **Depends on `add-localization`**: all new chart labels, stat-chip captions, and empty-state messages are written against string resources from the start, not hard-coded literals.
- **No new external dependency**: charts are implemented as custom Jetpack Compose `Canvas` drawings, consistent with [ADR 0003: Dependency Minimization Policy](../../../docs/adr/0003-dependency-minimization.md). No third-party charting library is introduced.
- Update `README.md` to describe the per-metric trend charts once implemented.

## Capabilities
- **Added Capability:** `health-trends/weight`
- **Added Capability:** `health-trends/blood-pressure`
- **Added Capability:** `health-trends/glucose`

These are new presentation-layer capabilities (built on top of the existing, unchanged `health-metrics/*` retrieval requirements) rather than modifications to the domain metric specs, which stay data-focused and UI-agnostic.

## Impact
- Affected code: `app/src/main/java/nl/healthjournal/app/ui/history/HistoryScreen.kt`, `HistoryViewModel.kt`, and new Compose chart components under `app/src/main/java/nl/healthjournal/app/ui/history/charts/`.
- No domain (`:domain`) or data (`:data`) module changes are expected — trend data is derived client-side from the existing `GetHealthHistoryUseCase` output. The glucose category thresholds already encoded in `NhgGlucoseCategory` are unchanged by this proposal (see `design.md` for the DVN cross-check and one flagged discrepancy).
- No new dependencies, no ADR changes anticipated (custom Canvas charts).
- `README.md` roadmap/feature description update.
- Sequencing: `add-dark-theme` and `add-localization` should land first (or at least be implemented alongside), since this change's new composables are written to depend on both.

This proposal intentionally stops at design/specification. Implementation tasks will be drafted once the visual design in `design.md` is confirmed.
