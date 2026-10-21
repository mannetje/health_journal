## ADDED Requirements

### Requirement: Comment on mood entries
Mood entries SHALL carry the same optional comment as every other entry, with the 200-character limit, normalisation, storage as typed, the field and counter in the form and the edit dialog, display in History, and inclusion in export and backup. The app SHALL NOT analyse, score or classify the comment text.

#### Scenario: Comment shown in History
- **WHEN** an entry is saved with the comment "long day, quiet evening"
- **THEN** History shows the comment under the entry

#### Scenario: Comment too long
- **WHEN** 201 characters are typed
- **THEN** the field behaves as for other entries

#### Scenario: No analysis
- **WHEN** a comment contains words about feeling unwell
- **THEN** the app treats it as plain text and shows no message or change
