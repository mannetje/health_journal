## ADDED Requirements

### Requirement: Energy entries in the backup document
Each profile in the backup document SHALL hold an `energyEntries` list with every entry (timestamp, level, comment). `schemaVersion` SHALL stay 1: a file without the list restores with no energy entries and readers that do not know it ignore it. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two entries is backed up and restored into an empty app
- **THEN** both are present and identical, including the comment

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no energy entries

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `energyEntries` and the test vectors contain one entry
