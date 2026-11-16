## ADDED Requirements

### Requirement: Export and import fluid intake
Export SHALL write a file with the header `timestamp,ml,drink,comment`, with `drink` one of water, coffee, tea, milk, juice, soup, other; one row per entry of the profile sorted ascending by timestamp (entries, not day totals); `comment` is empty when absent; text cells use the existing quoting (`CsvQuoting`). Import SHALL accept the same header (a file without the `drink` column, and an unknown or empty drink, are read as other), report accepted and rejected rows like the other imports, and reject a row with a non-integer `ml`, a value outside 10 to 5000, or an unreadable timestamp. The drink portions and the personal minimum and maximum are settings of the person, not entries, and SHALL NOT be in the CSV (the limits travel in the encrypted backup). The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** an entry of 330 ml of tea with a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Empty history
- **WHEN** the profile has no entries
- **THEN** the file contains only the header row

#### Scenario: Invalid row
- **WHEN** a row has `ml` of 0 or 12000
- **THEN** that row is rejected with a reason and other rows are imported

#### Scenario: File without a drink column
- **WHEN** a file with the header `timestamp,ml,comment` is imported
- **THEN** the rows are accepted and stored with the drink Other
