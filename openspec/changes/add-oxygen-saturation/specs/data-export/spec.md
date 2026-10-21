## ADDED Requirements

### Requirement: Export and import oxygen saturation
Export SHALL write a file with the header `timestamp,spo2_percent,pulse_bpm,context,comment`, one row per reading of the profile sorted ascending by timestamp; `pulse_bpm` and `context` are empty when absent; `context` holds the stable code AT_REST, AFTER_ACTIVITY, ASLEEP_OR_JUST_WOKE or OTHER and is never translated; text cells use the existing quoting. Import SHALL accept the same header, report accepted and rejected rows like the other imports, and reject a row with a value outside 50 to 100, a non-integer value, a pulse outside 30 to 250 or an unknown context code. The UI SHALL offer export and import, state that comments are included and repeat the existing warning that a CSV file is not encrypted.

#### Scenario: Round trip
- **WHEN** a reading with pulse, context and a comment containing a comma is exported and imported into an empty profile
- **THEN** the imported reading equals the original

#### Scenario: Empty history
- **WHEN** the profile has no readings
- **THEN** the file contains only the header row

#### Scenario: Unknown context
- **WHEN** a row has the context code `UNKNOWN_X`
- **THEN** that row is rejected with a reason and other rows are imported
