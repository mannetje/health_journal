## ADDED Requirements

### Requirement: Export and import daily activity
Export SHALL write a file with the header `date,steps,active_minutes,activity,comment`, one row per entry of the profile sorted ascending by date; `date` is an ISO local date (`2026-10-21`); `steps`, `active_minutes` and `activity` are empty when absent; `activity` holds the stable code WALKING, CYCLING, SWIMMING, SPORT, HOUSEWORK_GARDEN or OTHER and is never translated; text cells use the existing `CsvQuoting`. Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with neither steps nor minutes, a value that is not a whole number or is outside 0 to 100000 (steps) or 0 to 1440 (minutes), an unknown activity code, a malformed or future date, or a date that already exists for the profile or earlier in the file. The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** an entry with steps, minutes, a type and a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Empty history
- **WHEN** the profile has no activity entries
- **THEN** the file contains only the header row

#### Scenario: Duplicate date
- **WHEN** two rows have the date 2026-10-21
- **THEN** the second row is rejected with a reason and other rows are imported

#### Scenario: Unknown code
- **WHEN** a row has the activity code `RUNNING_X`
- **THEN** that row is rejected with a reason and other rows are imported
