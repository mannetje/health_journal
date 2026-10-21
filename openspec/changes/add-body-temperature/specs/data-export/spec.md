## ADDED Requirements

### Requirement: Export and import body temperature
Export SHALL write a file with the header `timestamp,temperature_celsius,site,comment`, one row per reading of the profile sorted ascending by timestamp; `temperature_celsius` has one decimal and a point as separator regardless of region; `site` is empty when absent or holds the stable code EAR, MOUTH, ARMPIT, FOREHEAD or RECTAL and is never translated; text cells use `CsvQuoting`. Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with a value outside 30.0 to 45.0, a value with more than one decimal, a non-number, or an unknown site code. The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** a reading with a site and a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported reading equals the original

#### Scenario: Empty history
- **WHEN** the profile has no readings
- **THEN** the file contains only the header row

#### Scenario: Bad rows
- **WHEN** a row has the value 46.0 and another has the site code `ANKLE`
- **THEN** both rows are rejected with a reason and other rows are imported
