## ADDED Requirements

### Requirement: Export and import sleep entries
Export SHALL write a file with the header `bed_time,wake_time,quality,times_woken,comment`, one row per entry of the profile sorted ascending by wake time; `bed_time` and `wake_time` are ISO-8601 UTC timestamps; `quality` and `times_woken` are integers and empty when absent; text cells use the existing quoting (`CsvQuoting`). Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with an unparsable time, a wake time not after the bed time or more than 24 hours after it, a quality outside 1 to 5, a non-integer quality, a number of times woken outside 0 to 30 or a non-integer one. The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** an entry with quality, times woken and a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Empty history
- **WHEN** the profile has no sleep entries
- **THEN** the file contains only the header row

#### Scenario: Bad row
- **WHEN** a row has a quality of 7
- **THEN** that row is rejected with a reason and other rows are imported
