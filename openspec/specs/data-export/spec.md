# data-export Specification

## Purpose
Enables export of a Profile's measurement history for a given metric type as a CSV file, and import of measurements from a CSV file into the application.

## Requirements

### Requirement: Export metric history as CSV
The system SHALL allow exporting all measurements of a given metric type for a Profile as a UTF-8 encoded CSV file.

#### Scenario: Weight history exported
- **WHEN** a client requests a CSV export for the weight metric of a Profile
- **THEN** the system SHALL produce a CSV with headers: `timestamp,weight_kg,bmi` and one row per measurement in chronological order

#### Scenario: Blood pressure history exported
- **WHEN** a client requests a CSV export for the blood pressure metric
- **THEN** the system SHALL produce a CSV with headers: `timestamp,systolic_mmhg,diastolic_mmhg,classification`

#### Scenario: Glucose history exported
- **WHEN** a client requests a CSV export for the glucose metric
- **THEN** the system SHALL produce a CSV with headers: `timestamp,glucose_mmol_l,context,classification`

#### Scenario: Activity history exported
- **WHEN** a client requests a CSV export for the activity metric
- **THEN** the system SHALL produce a CSV with headers: `start_timestamp,end_timestamp,distance_m,duration_s`

#### Scenario: Empty history produces header-only CSV
- **WHEN** a Profile has no measurements for the requested metric
- **THEN** the system SHALL produce a CSV file containing only the header row

### Requirement: Import measurements from CSV
The system SHALL allow importing measurements into a Profile from a UTF-8 encoded CSV file whose format matches the export schema for that metric.

#### Scenario: Valid CSV imported
- **WHEN** a client provides a valid CSV file matching the expected schema for a metric
- **THEN** the system SHALL persist each row as a measurement linked to the specified Profile and return a count of rows imported

#### Scenario: Malformed row skipped with error report
- **WHEN** a CSV file contains one or more rows with invalid data (missing fields, unparseable values, out-of-range values)
- **THEN** the system SHALL skip those rows, import the valid rows, and return an error report listing skipped row numbers and reasons

#### Scenario: Duplicate timestamp not prevented
- **WHEN** a CSV file contains a row whose timestamp already exists for that Profile and metric
- **THEN** the system SHALL import it as an additional measurement (duplicates are allowed)

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
