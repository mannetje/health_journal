## ADDED Requirements

### Requirement: Fluid intake in the backup document
Each profile in the backup document SHALL hold a `fluidEntries` list with every entry (timestamp, ml, drink, comment or null) and `fluidMinMl` and `fluidMaxMl` values (whole numbers or null) holding the person's own minimum and maximum. `schemaVersion` SHALL stay 1: a file without them restores with no entries and no limits, and readers that do not know them ignore them. The drink portions are preferences and are not part of the list. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list.

#### Scenario: Everything is in the file
- **WHEN** a profile with two entries, a minimum of 1400 and a maximum of 2000 is backed up and restored into an empty app
- **THEN** both entries are present and identical, including the comment, and the minimum is 1400 and the maximum 2000

#### Scenario: Only one number set
- **WHEN** a profile with a maximum of 1500 and no minimum is backed up
- **THEN** `fluidMaxMl` is 1500, `fluidMinMl` is null and restoring leaves the minimum empty

#### Scenario: No numbers set
- **WHEN** a profile without limits is backed up
- **THEN** both are null and restoring leaves both empty

#### Scenario: Drink in the file
- **WHEN** an entry of 125 ml of Coffee is backed up and restored
- **THEN** the restored entry has the drink Coffee, and a file entry without a drink restores as Other

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no fluid entries and no limits

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script include `fluidEntries`, `fluidMinMl` and `fluidMaxMl` and the test vectors contain one entry
