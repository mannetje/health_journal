## 0. Decisions
- [x] 0.1 A list/graph toggle for filters that have a chart (requested 2026-10-19)
- [x] 0.2 Weight statistics like the example: lowest and highest, change table with kg/week and kg/%, monthly and yearly change bars (requested 2026-10-19)
- [x] 0.3 Toggle as an icon in the title row; default is the list; remembered per filter (proposed)
- [x] 0.4 Neutral wording and one neutral bar colour; a period the history does not reach shows a dash (proposed)
- [x] 0.5 Statistics use all weight entries, not the range selector (proposed)

## 1. Statistics
- [x] 1.1 `WeightStatistics` with tests: ties, a period longer than the history, month and year boundaries, a single entry, several entries on one day

## 2. App
- [x] 2.1 Toggle icon and per-filter state; graph view without the list; list view without the chart; timeline offset 0
- [x] 2.2 Weight graph sections: lowest/highest cards, change table with kg/% toggle
- [x] 2.3 `BarTrendChart`, the monthly chart with year chips and the yearly chart
- [x] 2.4 English and Dutch strings and content descriptions; layout check at font scale 1.3 and 2.0, light and dark

## 3. Docs
- [x] 3.1 README, CHANGELOG, `docs/dev/design.md`, `docs/dev/testing.md`
- [x] 3.2 Screenshots of the graph view and the weight statistics (light and dark)
- [x] 3.3 Archive the change and merge the delta into `openspec/specs/health-trends`
