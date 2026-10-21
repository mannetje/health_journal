## ADDED Requirements

### Requirement: Oxygen saturation entries in History
The History list SHALL include oxygen saturation readings as a further entry type in the merged list, sorted by timestamp, and the single-metric filter SHALL offer Oxygen. Each can be edited in a dialog with an editable date and time, the value (50 to 100), the optional pulse (30 to 250), the optional context and the comment, validated like recording; deleting uses the same confirmation as other entries. A failed update keeps the dialog open.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and an oxygen reading at 11:00 exist
- **THEN** the oxygen reading is listed first

#### Scenario: Edit rejects invalid value
- **WHEN** an edit sets the value to 101
- **THEN** an "invalid value" message is shown and the stored reading is unchanged

#### Scenario: Timeline navigation
- **WHEN** readings span two months and the Oxygen filter is on
- **THEN** the year and month rows appear as for other filters
