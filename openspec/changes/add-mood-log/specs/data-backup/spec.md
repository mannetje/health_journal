## ADDED Requirements

### Requirement: Mood entries in the backup document
Each profile in the backup document SHALL hold a `moodEntries` list with every entry (timestamp, level as a whole number, stressLevel as a whole number or absent, feelings as a list of the stable codes used in the CSV export, comment). `schemaVersion` SHALL stay 1: a file without the list restores with no mood entries and readers that do not know it ignore it; an item without `stressLevel` restores with no stress level. An unknown feeling code in a file SHALL be ignored for that code and the entry kept. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two mood entries is backed up and restored into an empty app
- **THEN** both are present and identical, including stress level, words and comment

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no mood entries

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `moodEntries` and the test vectors contain one entry
