## ADDED Requirements

### Requirement: Migraines in the backup document
Each profile in the backup document SHALL hold a `migraines` list with every migraine of the profile (start, woke-up-with-it flag, end or null, intensity or null, areas, pain characters, type, symptoms, triggers, took-something, medication text, second dose, `intensityAfter2Hours` or null, relief methods, personal trigger tag ids, personal relief tag ids, the two "helped me" ticks, activities, comment) and a `migraineTags` list (id, kind trigger or relief, label, archived flag), using the same stable codes as the CSV export. `schemaVersion` SHALL stay 1 because readers ignore unknown properties, and a file without `migraines` SHALL restore with no migraines. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors and the recovery page) SHALL describe the list. Restoring into an app whose database predates migraines SHALL not occur, because the database is migrated on open.

#### Scenario: Everything is in the file
- **WHEN** a profile with two migraines, one ongoing, is backed up and restored into an empty app
- **THEN** both migraines are present and identical, including the ongoing one

#### Scenario: Older file
- **WHEN** a backup made before this change is restored
- **THEN** it restores as before and the profile has no migraines

#### Scenario: Older reader
- **WHEN** a reader that does not know migraines opens a newer file
- **THEN** it ignores the list and imports what it knows

#### Scenario: Published format
- **WHEN** the documentation is checked
- **THEN** the JSON schema and the Python recovery script both include `migraines` and the test vectors contain one migraine
