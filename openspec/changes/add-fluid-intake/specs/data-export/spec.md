## ADDED Requirements

### Requirement: Export and import fluid intake
Export SHALL write a file with the header `timestamp,ml,comment`, one row per entry of the profile sorted ascending by timestamp (entries, not day totals); `comment` is empty when absent; text cells use the existing quoting (`CsvQuoting`). Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with a non-integer `ml`, a value outside 10 to 5000, or an unreadable timestamp. The quick-add sizes and the personal limit are settings of the person, not entries, and SHALL NOT be in the CSV (the limit travels in the encrypted backup). The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** an entry of 330 ml with a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Empty history
- **WHEN** the profile has no entries
- **THEN** the file contains only the header row

#### Scenario: Invalid row
- **WHEN** a row has `ml` of 0 or 12000
- **THEN** that row is rejected with a reason and other rows are imported
