## ADDED Requirements

### Requirement: Comment on migraine entries
Migraine entries SHALL carry the same optional comment as every other entry, with the same 200-character limit, normalisation, storage as typed, form field, History display, export and backup behaviour.

#### Scenario: Comment saved
- **WHEN** a migraine is saved with the comment "after the flight"
- **THEN** History shows that comment on the entry

#### Scenario: Comment too long
- **WHEN** a comment of 201 characters is entered for a migraine
- **THEN** it is limited or refused exactly as for other entries
