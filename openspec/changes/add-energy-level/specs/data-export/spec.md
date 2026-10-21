## ADDED Requirements

### Requirement: Export and import energy level
Export SHALL write a file with the header `timestamp,energy_0_10,comment`, one row per entry of the profile sorted ascending by timestamp, with text cells written through `CsvQuoting`. Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with a non-integer value or a value outside 0 to 10. The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** an entry with a comment containing a comma and a quote is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Empty history
- **WHEN** the profile has no entries
- **THEN** the file contains only the header row

#### Scenario: Invalid row
- **WHEN** a row has the value 11 or 4.5
- **THEN** that row is rejected with a reason and other rows are imported
