# Add History Timeline Navigation

## Why
The History list grows with every entry. After a year it is a long scroll, and the only way to reach an older entry is to keep scrolling. People want to jump to a year or a month, and to see where they are while they scroll.

## What Changes
- **Year row and month row** above the History list: a horizontally scrolling row of years and one of months of the selected year, like a date picker that is always visible.
- **Tap to jump:** tapping a year or a month scrolls the list to the newest entry of that period.
- **Always in sync:** while the list scrolls (by finger, fling or jump) the selected year and month follow the top of the list, and the chip rows scroll so the selection stays visible. Tapping a chip and scrolling are two ways to move the same position, never two separate states.
- **Month headers** in the list ("August 2025") with the number of entries of that month, so the position is also readable inside the list.
- **Only periods with entries** can be selected. Months without entries of the selected year are shown dimmed and cannot be tapped.
- **All entry types:** the navigation is on every History view (All, Weight, Blood pressure, Glucose, Waist, Activity). Activity entries are placed by start time.
- **Scroll as before:** the list keeps its newest-first order, the filters and the trend range work as today. The navigation follows whatever list is shown.

## Capabilities
- **Modified Capability:** `entry-management` (History list navigation)

## Impact
- **Database:** none.
- **Affected code:** `app` only: a small pure `HistoryTimeline` (groups the shown entries by year and month and maps a list position to a period and back), the navigation rows and month headers in `HistoryScreen`, strings in English and Dutch. No `domain` or `data` change.
- **Dependencies:** none.
- **Docs:** README (History), CHANGELOG, `docs/dev/design.md` (screen map and layout rules), `docs/dev/testing.md` if the test list changes, the `entry-management` spec. No ADR: it is a tactical UI addition within ADR 0010 (responsive layout).

## Non-goals
- No search and no free date picker (a later proposal if wanted).
- No per-month totals or changes (for example weight difference): figures would read as a verdict and need their own design.
- No change to the trend charts or to the order of entries.
