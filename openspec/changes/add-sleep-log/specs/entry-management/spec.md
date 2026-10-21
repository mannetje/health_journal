## ADDED Requirements

### Requirement: Sleep entries in History
The History list SHALL include sleep entries as a further entry type in the merged list, sorted by wake time, and the single-metric filter SHALL offer Sleep. Each can be edited in a dialog with the editable bed time and wake time, the optional quality (1 to 5), the optional number of times woken (0 to 30) and the comment, validated like recording; the row SHALL show the duration as plain text. Deleting uses the same confirmation as other entries. A failed update keeps the dialog open.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a sleep entry waking at 11:00 exist
- **THEN** the sleep entry is listed first

#### Scenario: Edit rejects invalid times
- **WHEN** an edit sets the wake time before the bed time
- **THEN** an "invalid time" message is shown and the stored entry is unchanged

#### Scenario: Timeline navigation
- **WHEN** entries span two months and the Sleep filter is on
- **THEN** the year and month rows appear as for other filters
