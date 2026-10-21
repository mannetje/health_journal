## ADDED Requirements

### Requirement: Comment on body temperature readings
Body temperature readings SHALL carry the same optional comment as every other entry, with the 200-character limit, normalisation, storage as typed, the field and counter in the form and the edit dialog, display in History, and inclusion in export and backup.

#### Scenario: Comment shown in History
- **WHEN** a reading is saved with the comment "after lunch"
- **THEN** History shows the comment under the reading

#### Scenario: Comment too long
- **WHEN** 201 characters are typed
- **THEN** the field behaves as for other entries
