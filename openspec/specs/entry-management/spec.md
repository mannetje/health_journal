# entry-management Specification

## Purpose
Defines how recorded entries (weight, blood pressure, glucose, waist circumference, activity) are listed, edited and deleted from the History screen.

## Requirements

### Requirement: History list
The History screen SHALL show the entries of the active profile as one merged list of all five entry types, sorted newest first (activity by start time). When a single-metric filter is selected (weight, blood pressure, glucose or waist circumference) the list SHALL be limited to that type and to the selected trend range (see health-trends). The activity filter and the ALL filter show every entry of the type(s) without range restriction.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a glucose at 11:00 exist
- **THEN** the glucose entry is listed first

### Requirement: Update semantics
Updating an entry SHALL take the entry id, the owning profile and the new values (plus timestamp and the optional comment). The values SHALL be re-validated with the same rules as recording, and derived values (BMI for weight, category for blood pressure and glucose) SHALL be recomputed. The comment SHALL follow the rules of `entry-comments`; an empty comment removes the stored one. The entry keeps its id and profile. Updating an unknown id SHALL return "not found" and create nothing. An invalid update SHALL be rejected and leave the stored entry unchanged.

#### Scenario: Update recomputes BMI
- **WHEN** a weight entry is edited from 75 kg to 80 kg for a 180 cm profile
- **THEN** the same entry now has 80 kg and BMI 24.7

#### Scenario: Unknown id
- **WHEN** an update targets an id that does not exist
- **THEN** the result is "not found" and no entry is created

#### Scenario: Invalid update
- **WHEN** an update carries 800 kg
- **THEN** it is rejected and the stored entry is unchanged

#### Scenario: Comment edited and removed
- **WHEN** an entry with the comment "old" is edited to "new", and later saved with an empty comment
- **THEN** the entry shows "new" after the first edit and no comment after the second

### Requirement: Edit dialog rules
Each entry can be edited in a dialog that pre-fills the current values in the user's display units, including an editable comment field with the counter from `entry-comments`. Validation in the dialog:
- weight: 1 to 700 kg, else an "invalid value" message
- blood pressure: systolic 40 to 300, diastolic 20 to 200, systolic > diastolic, optional pulse 30 to 250
- waist circumference: 40 to 200 cm
- glucose: 0.5 to 55.0 mmol/L (after conversion from the display unit), else an "invalid value" message
- activity: duration greater than 0 minutes, distance 0 or more; start time is not editable
- comment: at most 200 characters, never a reason to block saving otherwise
Weight, blood pressure, glucose and waist circumference expose an editable date and time; activity does not.
If the update fails or the entry is gone, the dialog SHALL stay open and show an error ("Entry not found" when missing). On success the dialog closes and history and trend data refresh.

#### Scenario: Failed update keeps dialog
- **WHEN** the domain rejects the edited value
- **THEN** the dialog stays open showing the error

#### Scenario: Comment pre-filled
- **WHEN** an entry with the comment "new scale" is opened for editing
- **THEN** the comment field shows "new scale"

### Requirement: Untouched fields keep stored values
Each edit field is pre-filled with the stored value formatted for display: weight 2 decimals, glucose 1 decimal (mmol/L) or 0 decimals (mg/dL), distance 2 decimals, duration in minutes (seconds / 60) at 2 decimals; trailing zeros are stripped and the region's decimal separator is used. If a field's text equals its initial text when saved, the original stored value SHALL be saved, so display rounding never changes stored data. The comment is never reformatted for display.

#### Scenario: Round trip without change
- **WHEN** a glucose of 5.55 mmol/L is opened (shown as 5.6) and saved without editing
- **THEN** 5.55 mmol/L remains stored

#### Scenario: Comment round trip
- **WHEN** an entry with a comment is opened and saved without editing
- **THEN** the comment is unchanged

### Requirement: Delete
Deleting an entry SHALL require confirmation in a dialog and is permanent (no undo). Deleting an unknown id returns "not found" (shown as "Entry not found") and changes nothing; only the targeted entry is removed.

#### Scenario: Confirm delete
- **WHEN** the user confirms deletion of one weight entry
- **THEN** only that entry is removed and the list refreshes

#### Scenario: Cancel delete
- **WHEN** the user cancels the confirmation
- **THEN** nothing is removed

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

### Requirement: Migraine entries in History
The History list SHALL include migraine entries as a sixth entry type in the merged list, sorted by start time, and the single-metric filter SHALL offer Migraine. Editing SHALL re-validate with the recording rules (end not before start, intensity 0 to 10, intensity after 2 hours 0 to 10 and only with something taken, free text length) and keep the entry id and profile. Deleting SHALL use the same confirmation as other entries. The row of an ongoing migraine SHALL offer an "it ended" action. Details are in the `migraine` capability.

#### Scenario: Merged list with migraine
- **WHEN** a glucose at 09:00 and a migraine starting at 10:00 exist
- **THEN** the migraine is listed first

#### Scenario: Filter
- **WHEN** the Migraine filter is selected
- **THEN** only migraines are listed

#### Scenario: Edit rejects invalid end
- **WHEN** an edit sets the end time before the start time
- **THEN** it is refused and the stored entry is unchanged
