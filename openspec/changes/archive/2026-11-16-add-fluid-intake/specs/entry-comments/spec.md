## ADDED Requirements

### Requirement: Comment on fluid intake entries
Fluid intake entries SHALL carry the same optional comment as every other entry, with the 200-character limit, normalisation, storage as typed, the field and counter in the custom entry form and the edit dialog, display in History, and inclusion in export and backup. Quick-add entries start without a comment.

#### Scenario: Comment shown in History
- **WHEN** a custom entry is saved with the comment "after the walk"
- **THEN** History shows the comment under the entry

#### Scenario: Comment added to a quick-add entry
- **WHEN** a quick-add entry is edited in History and a comment is saved
- **THEN** the comment is stored and shown

#### Scenario: Comment too long
- **WHEN** 201 characters are typed
- **THEN** the field behaves as for other entries
