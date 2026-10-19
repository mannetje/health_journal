## 0. Decisions
- [x] 0.1 Vertical line and value on touch, only entries selectable (requested 2026-10-19)
- [x] 0.2 Tap to select, second tap to compare, same entry again to clear (proposed, to be approved)
- [x] 0.3 Neutral wording; all text in English and Dutch

## 1. Logic
- [x] 1.1 `PointSelection` with tests: nearest entry in time, ties take the newest, limit and average series excluded, difference in value, percent and time between two entries, selection cycle (first, second, third, same again)

## 2. App
- [x] 2.1 `ChartSeries` flag for selectable series; reference lines and averages not selectable
- [x] 2.2 Marker with vertical guideline and label in `LineTrendChart`; pan and zoom still work
- [x] 2.3 Second selection shows both lines and the difference
- [x] 2.4 English and Dutch strings; spoken description for TalkBack
- [x] 2.5 Check on the emulator: weight, blood pressure, glucose, waist; English, Dutch, mg/dL, dark mode

## 3. Docs
- [x] 3.1 README, CHANGELOG, `docs/dev/graphs.md`, `docs/dev/testing.md`
- [x] 3.2 Archive the change and merge the delta into `openspec/specs/health-trends`
