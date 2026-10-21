## ADDED Requirements

### Requirement: Mood entries in History
The History list SHALL include mood entries as a further entry type in the merged list, sorted by timestamp, and the single-metric filter SHALL offer Mood. A row SHALL show the time, the step number with its label, the feeling words as plain text and the comment, in the same style for every step. Each entry can be edited in a dialog with an editable date and time, the five steps, the feeling words and the comment, validated like recording; deleting uses the same confirmation as other entries. A failed update keeps the dialog open.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a mood entry at 11:00 exist
- **THEN** the mood entry is listed first

#### Scenario: Same style
- **WHEN** rows with step 1 and step 5 are listed
- **THEN** they differ only by number, label and text, not by colour or icon

#### Scenario: Edit step and words
- **WHEN** an entry is edited from step 2 to step 3 and "tense" is removed
- **THEN** the stored entry has step 3 and no "tense"

#### Scenario: Delete
- **WHEN** an entry is deleted and the confirmation accepted
- **THEN** it is gone from History and the graph

#### Scenario: Timeline navigation
- **WHEN** entries span two months and the Mood filter is on
- **THEN** the year and month rows appear as for other filters
