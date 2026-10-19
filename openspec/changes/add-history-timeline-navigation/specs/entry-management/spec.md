## ADDED Requirements

### Requirement: Timeline navigation
The History screen SHALL show, for every filter (All, Weight, Blood pressure, Glucose, Waist, Activity), a year row and a month row above the entry list when the shown entries span at least two distinct months. The year row SHALL list the years that have shown entries; the month row SHALL list the twelve months of the selected year, in calendar order, with months that have no shown entries dimmed and not selectable. Month names SHALL follow the app language. Each chip SHALL be at least 48 dp tall, the rows SHALL scroll horizontally without wrapping text, and the selected year and month SHALL be marked by more than colour.

The selected year and month SHALL always be the period of the entry at the top of the list, in the device time zone. They SHALL update continuously while the list scrolls by drag, fling or jump, and the rows SHALL scroll so the selected chips stay visible. Selecting a month SHALL scroll the list to the newest entry of that month; selecting a year SHALL scroll to the newest entry of that year. The list order, filters and trend range SHALL be unchanged, and the navigation SHALL describe the list as shown after the filter and range are applied.

#### Scenario: Jump to a month
- **WHEN** entries exist in June 2025 and August 2025 and the user taps "Jun" in 2025
- **THEN** the list scrolls to the newest entry of June 2025 and "Jun" is shown as selected

#### Scenario: Scrolling updates the selection
- **WHEN** the user scrolls the list until the top entry is from March 2024
- **THEN** the year row shows 2024 selected and the month row shows March selected, both scrolled into view

#### Scenario: Year jump
- **WHEN** the user taps 2023 and the newest entry of 2023 is in November
- **THEN** the list scrolls to that entry and November 2023 is selected

#### Scenario: Month without entries
- **WHEN** the selected year has no entry in April
- **THEN** "Apr" is dimmed and tapping it does nothing

#### Scenario: Every entry type
- **WHEN** the filter is All, Weight, Blood pressure, Glucose, Waist or Activity
- **THEN** the same year and month rows and month headers are available, built from the entries of that view (activity by start time)

#### Scenario: Filter changes the periods
- **WHEN** the weight filter and a 3-month trend range are selected
- **THEN** only years and months that have weight entries within that range are selectable

#### Scenario: Too little to navigate
- **WHEN** all shown entries are from one month
- **THEN** the year and month rows are not shown

#### Scenario: Year boundary
- **WHEN** an entry at 23:30 on 31 December and one at 00:30 on 1 January exist in the device time zone
- **THEN** they belong to different years and different month headers

### Requirement: Month headers
The History list SHALL show a header before the first entry of each month with the month name, the year and the number of shown entries of that month. The header SHALL be part of the scrolling list and SHALL NOT show figures derived from the entries (for example a change in weight).

#### Scenario: Header with count
- **WHEN** 12 entries from August 2025 are shown
- **THEN** a header "August 2025" with the count 12 precedes the first of them
