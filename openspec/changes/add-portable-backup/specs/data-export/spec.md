## ADDED Requirements

### Requirement: CSV export is plain text and says so
The UI that exports CSV files SHALL state, before the file is written, that the file is not encrypted and that anyone who can open it can read it. The warning SHALL exist in English and Dutch. The CSV export SHALL NOT be described as a backup; the encrypted backup file is described in the `data-backup` spec.

#### Scenario: Warning before export
- **WHEN** the user starts a CSV export
- **THEN** the warning that the file is not encrypted is shown before the destination is chosen

#### Scenario: Both languages
- **WHEN** the language is Dutch
- **THEN** the warning is shown in Dutch
