## 0. Decisions
- [x] 0.1 Pinned year and month rows above the list, in sync with scrolling (requested 2026-10-19)
- [x] 0.1b The navigation applies to every filter and entry type, including All and Activity (requested 2026-10-19)
- [x] 0.2 Months without entries are dimmed and not selectable (proposed)
- [x] 0.3 Month headers show the entry count only, no figures such as weight change (proposed)

## 1. App logic
- [x] 1.1 `HistoryTimeline`: group shown rows by year and month in the device zone (all five metric types, activity by start time), first list index per group, position → period and period → index; JVM tests: grouping order, year boundary (31 Dec 23:30 and 1 Jan 00:30), time-zone grouping equals row text, empty list, one month, chart item offset, newest period while the chart is on screen

## 2. App UI
- [x] 2.1 Year row and month row above the list; tap jumps; chips scroll to follow the selection; selected state by fill and check icon; dimmed months not clickable; hidden when fewer than 2 months
- [x] 2.2 Month headers with the entry count as list items
- [x] 2.3 Scroll and selection stay in sync: scroll → selection → chips, tap → animated scroll → selection; no second selected state
- [ ] 2.4 English and Dutch strings (month header, count, "no entries", content descriptions); layout check at font scale 1.3 and 2.0 in both languages, light and dark

## 3. Docs
- [x] 3.1 README (History), CHANGELOG (Unreleased), `docs/dev/design.md` (screen map, layout rules), `docs/dev/testing.md`, the specs index and `scripts/check-docs.sh`
- [x] 3.2 Screenshots of the History tab with the rows (light and dark, sample data)
- [ ] 3.3 Archive the change and merge the delta into `openspec/specs/entry-management`
- [x] 3.4 Commits carry no attribution trailer
