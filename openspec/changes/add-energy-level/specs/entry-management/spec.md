## ADDED Requirements

### Requirement: Energy entries in History
The History list SHALL include energy entries as a further entry type in the merged list, sorted by timestamp, and the single-metric filter SHALL offer Energy. Each can be edited in a dialog with an editable date and time, the level (0 to 10, using the shared picker) and the comment, validated like recording; deleting uses the same confirmation as other entries. A failed update keeps the dialog open.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and an energy entry at 11:00 exist
- **THEN** the energy entry is listed first

#### Scenario: Edit rejects invalid value
- **WHEN** an edit would set the level outside 0 to 10
- **THEN** an "invalid value" message is shown and the stored entry is unchanged

#### Scenario: Timeline navigation
- **WHEN** entries span two months and the Energy filter is on
- **THEN** the year and month rows appear as for other filters
