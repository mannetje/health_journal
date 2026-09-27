## ADDED Requirements

### Requirement: Import weight from Libra CSV
The system SHALL support importing weight measurements from UTF-8 encoded CSV files exported by the Libra Android application (`net.cachapa.libra`).

#### Scenario: Valid Libra CSV in kilograms imported
- **WHEN** a client imports a Libra CSV file with `#Units: kg` (or default kilograms) and semicolon-delimited rows containing valid ISO dates and weights
- **THEN** the system SHALL parse each data row, persist the weight measurement associated with the active Profile, and return the count of successfully imported rows

#### Scenario: Libra CSV with imperial pounds converted to kilograms
- **WHEN** a client imports a Libra CSV file specifying `#Units: lbs`
- **THEN** the system SHALL convert each weight entry from pounds to kilograms using factor 1 lb = 0.45359237 kg rounded to two decimal places and persist the weight measurement

#### Scenario: Metadata comments and optional columns ignored
- **WHEN** a Libra CSV contains header comments (such as `#Version:`, `#Units:`, or `#date;weight;...`) and optional trailing columns (weight trend, body fat, muscle mass, notes)
- **THEN** the system SHALL safely ignore non-essential metadata and optional columns while extracting date and weight

#### Scenario: Malformed Libra row skipped with error report
- **WHEN** a Libra CSV row contains an unparseable timestamp, non-numeric weight, or weight outside the physiological range of 1.0 kg to 700.0 kg
- **THEN** the system SHALL skip the malformed row, continue importing remaining rows, and include the line number and error reason in the skipped rows report
