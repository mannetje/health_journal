## ADDED Requirements

### Requirement: Migraine entries in History
The History list SHALL include migraine entries as a sixth entry type in the merged list, sorted by start time, and the single-metric filter SHALL offer Migraine. Editing SHALL re-validate with the recording rules (end not before start, intensity 0 to 10, free text length) and keep the entry id and profile. Deleting SHALL use the same confirmation as other entries. The row of an ongoing migraine SHALL offer an "it ended" action. Details are in the `migraine` capability.

#### Scenario: Merged list with migraine
- **WHEN** a glucose at 09:00 and a migraine starting at 10:00 exist
- **THEN** the migraine is listed first

#### Scenario: Filter
- **WHEN** the Migraine filter is selected
- **THEN** only migraines are listed

#### Scenario: Edit rejects invalid end
- **WHEN** an edit sets the end time before the start time
- **THEN** it is refused and the stored entry is unchanged
