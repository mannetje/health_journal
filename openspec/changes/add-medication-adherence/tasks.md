## 0. Decisions
- [x] 0.1 Skipped counts as due and not taken, shown separately (approved)
- [x] 0.2 Adherence is a third view in the pillbox screen, no bottom tab (approved 2026-10-06)
- [x] 0.3 The trend-chart overlay is deferred (approved 2026-10-06)

## 1. Domain
- [ ] 1.1 `AdherenceCalculator` with tests: rounding (half up, 26 of 28 is 93), skipped, missed versus inside grace, as-needed, start and end dates, archive date, schedule version in force, an edit from today leaving earlier days unchanged, an edit from an earlier date changing them, orphan outcomes, due 0 shows no percentage, streak across days with nothing planned
- [ ] 1.2 `GetAdherence` use case for 7, 30 and 90 days

## 2. App
- [ ] 2.1 Adherence view in the pillbox screen: overall and per-medication rows, streak, skipped and missed counts, missed list; neutral wording
- [ ] 2.2 English and Dutch strings, `UiText`; layout check at 1.3x and 2.0x; ViewModel tests

## 3. Docs
- [ ] 3.1 README, CHANGELOG (Unreleased), affected specs, the specs index and `scripts/check-docs.sh`
- [ ] 3.2 Archive the change and merge the delta into `openspec/specs/medication`
- [ ] 3.3 Commits carry no attribution trailer
