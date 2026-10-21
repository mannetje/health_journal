## ADDED Requirements

### Requirement: Body temperature entries in History
The History list SHALL include body temperature readings as a further entry type in the merged list, sorted by timestamp, and the single-metric filter SHALL offer Temperature. Each can be edited in a dialog with an editable date and time, the value (30.0 to 45.0, one decimal), the optional site and the comment, validated like recording; deleting uses the same confirmation as other entries. A failed update keeps the dialog open.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a temperature reading at 11:00 exist
- **THEN** the temperature reading is listed first

#### Scenario: Edit rejects invalid value
- **WHEN** an edit sets the value to 50.0
- **THEN** an "invalid value" message is shown and the stored reading is unchanged

#### Scenario: Timeline navigation
- **WHEN** readings span two months and the Temperature filter is on
- **THEN** the year and month rows appear as for other filters
