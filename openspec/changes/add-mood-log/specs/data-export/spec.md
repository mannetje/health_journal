## ADDED Requirements

### Requirement: Export and import mood entries
Export SHALL write a file with the header `timestamp,mood_level,stress_level,feelings,comment`, one row per entry of the profile sorted ascending by timestamp (equal timestamps in recorded order); `mood_level` is a whole number 1 to 5; `stress_level` is a whole number 1 to 5 or empty; `feelings` holds the stable codes CALM, CONTENT, CHEERFUL, ENERGETIC, TIRED, TENSE, RESTLESS, IRRITATED, SAD, WORRIED joined by `|`, is empty when none, and is never translated; text cells use `CsvQuoting.cell`. Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with a level or stress level outside 1 to 5 or not a whole number, an unknown feeling code, more than three codes, a repeated code or an unreadable timestamp; a rejected row imports nothing. The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** an entry with two words and a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported entry equals the original

#### Scenario: Empty history
- **WHEN** the profile has no mood entries
- **THEN** the file contains only the header row

#### Scenario: Unknown code
- **WHEN** a row has the feeling code `ANGRY_X`
- **THEN** that row is rejected with a reason and other rows are imported

#### Scenario: Level out of range
- **WHEN** a row has `mood_level` 6 or 2.5
- **THEN** that row is rejected with a reason

#### Scenario: Stress out of range
- **WHEN** a row has `stress_level` 0 or 7
- **THEN** that row is rejected with a reason

#### Scenario: No stress level
- **WHEN** a row has an empty `stress_level`
- **THEN** it is imported with no stress level
