# Design: History Timeline Navigation

## Context
`HistoryScreen` builds one merged list of rows, sorted newest first, inside a `LazyColumn`. Under a single-metric filter a trend chart is the first item of that list. The list can hold thousands of entries.

## One source of truth: the position in the list
The selected year and month are not stored. They are derived from the list position:
- `HistoryTimeline.of(rows)` groups the shown rows by (year, month) in the device time zone and keeps, per group, the index of its first row in the list (including the month header item and the chart item offset).
- **Scrolling → selection:** `LazyListState.firstVisibleItemIndex` is read with `snapshotFlow` + `distinctUntilChanged`; the group that contains that index is the selected period. If the chart is still at the top (no entry visible yet) the newest period is selected.
- **Tap → scroll:** a tap calls `animateScrollToItem(index of the first item of that group)`. While that animation runs, the selection follows the same derivation, so it moves smoothly through the periods in between and ends on the tapped one. There is no second "selected" variable that could disagree with the list.
- **Year tap:** jumps to the newest month of that year that has entries.
- **Selection → chips:** when the selected period changes, the year row and the month row scroll the selected chip into view (`animateScrollToItem` on their own `LazyListState`), so the chips follow a fling.

## Layout
```
filter chips
range source note
[ 2023 | 2024 | 2025 | 2026 ]      year row, scrolls sideways
[ Jun | Jul | Aug | Sep | ... ]    month row of the selected year
list                               month headers + entries
```
- The two rows sit above the list, outside the `LazyColumn`, so they stay visible while it scrolls. They are pinned on purpose: a sticky header inside the list would scroll away with the chart item.
- Months of the selected year in calendar order (Jan to Dec). A month without entries is dimmed, has a "no entries" state description and is not clickable. Selected is shown by fill and by a check icon, not by colour alone, like the other segmented controls.
- Chips are at least 48 dp tall. The rows scroll horizontally so words never wrap, at font scale 2.0 too.
- Month names come from `java.time` with the app locale (English and Dutch follow the language setting).
- The month header item shows "August 2025" and the count ("12 entries"). It is a normal list item, so the filters, edit and delete work unchanged.
- With fewer than 2 distinct months the rows are hidden: there is nothing to navigate.

## Interaction with filters and the trend range
`HistoryTimeline` is built from the rows that are shown, after the filter and the date range are applied. Changing a filter or the range rebuilds the groups and scrolls to the top (as the list does today); the selection is then the newest period.

## Performance
Grouping is one pass over the shown rows when they change (remembered on the row list). Scroll tracking reads one index and only recomposes the chips when the period changes.

## Alternatives considered
- **A stored selected year and month in the ViewModel:** rejected, it can drift from the list and needs sync code in both directions.
- **A sticky header only (no chips):** rejected, it shows the position but gives no jump.
- **A date picker dialog:** rejected, slower than two rows that are always visible, and no sync while scrolling.
- **A fast-scroll thumb:** possible later; does not give year and month jumps.

## Risks
- A fling can move the selection quickly and cause many chip scrolls. Mitigation: `distinctUntilChanged` on the period and `scrollToItem` (no animation) while the list itself is flinging, animation only when idle.
- Vertical space at large font scales. Mitigation: the rows are compact single lines and scroll sideways; checked at 1.3 and 2.0.
- Timestamps near midnight and time-zone changes group by the device zone at the moment of display. Mitigation: grouping uses the same `formatLocal` zone as the row text, so header and row always agree.
