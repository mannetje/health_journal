## ADDED Requirements

### Requirement: Daily activity entries in History
The History list SHALL include daily activity entries as a further entry type in the merged list, positioned at the start (00:00, local time) of their date, and the single-metric filter SHALL offer Activity. Each can be edited in a dialog with an editable date, steps, active minutes, the optional activity type and the comment, validated like recording; deleting uses the same confirmation as other entries. A failed update keeps the dialog open. Changing the date to one that already has an entry SHALL be refused with a message and leave both entries unchanged.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and an activity entry for the same day exist
- **THEN** the weight is listed first and the activity entry after it

#### Scenario: Edit rejects invalid value
- **WHEN** an edit clears both steps and minutes
- **THEN** a "enter steps or minutes" message is shown and the stored entry is unchanged

#### Scenario: Edit to an occupied date
- **WHEN** an edit moves an entry to a date that already has one
- **THEN** a message is shown and neither entry changes

#### Scenario: Timeline navigation
- **WHEN** entries span two months and the Activity filter is on
- **THEN** the year and month rows appear as for other filters
