# data-export Specification

## Purpose
Defines the CSV contract for exporting a profile's measurement history and importing measurements (including the Libra weight format). The CSV format is a stable interchange contract; a port to another platform SHALL read and write identical files.

## Requirements

### Requirement: CSV conventions
Exported files SHALL be UTF-8. Every row, including the header, SHALL end with a line feed. The separator is a comma and the decimal separator is always a point, independent of region. All values are metric canonical units regardless of display settings. Timestamps SHALL be ISO-8601 in UTC with a trailing Z (for example 2026-09-20T08:00:00Z); fractional seconds appear only when non-zero.

#### Scenario: Locale independence
- **WHEN** the region uses a decimal comma and weight is exported
- **THEN** the weight in the file still uses a decimal point

### Requirement: Export metric history
Export SHALL write the header row followed by one row per measurement of the profile, sorted ascending by timestamp (activity by start timestamp). Headers and columns:
- weight: `timestamp,weight_kg,bmi` (weight as plain decimal; bmi empty when absent)
- blood pressure: `timestamp,systolic_mmhg,diastolic_mmhg,classification` (classification is the category name)
- glucose: `timestamp,glucose_mmol_l,context,classification` (context FASTING or POSTPRANDIAL; classification is the category name)
- activity: `start_timestamp,end_timestamp,distance_m,duration_s` (distance in meters as decimal such as 5000.0; duration in whole seconds)

The UI SHALL offer export for weight, blood pressure and glucose. Activity export is supported by the contract but has no UI entry point.

#### Scenario: Weight exported
- **WHEN** two weights are exported
- **THEN** the file has the header `timestamp,weight_kg,bmi` and two rows, oldest first

#### Scenario: Empty history
- **WHEN** the profile has no measurements of the metric
- **THEN** the file contains only the header row

### Requirement: Import measurements
Import SHALL take a metric type name and the file content and return an import result: the number of rows imported and the list of skipped rows (line number, reason). The metric type name is lowercased with "-" replaced by "_"; accepted names are `weight`, `blood_pressure` (also `bloodpressure`, `bp`), `glucose`, `activity`, and `libra` / `libra_weight`. Any other name skips every row with reason "Unsupported metric type: X". Blank content imports nothing and skips nothing. Blank lines are ignored, but reported line numbers are the physical line numbers of the file (blank lines count), so they match what a text editor shows. A file with only a header imports 0 rows. Each imported row SHALL receive a new identifier and be linked to the active profile. Import with no active profile SHALL do nothing. Skip reasons are technical English text and are not localized.

#### Scenario: Valid file
- **WHEN** a weight file with two valid rows is imported
- **THEN** the result is 2 imported and no skipped rows

#### Scenario: Unknown metric
- **WHEN** the metric name is "steps"
- **THEN** all rows are skipped with "Unsupported metric type: steps"

### Requirement: Standard row rules
Cells are split on commas and trimmed. Timestamps SHALL be parsed as an ISO-8601 instant, otherwise as epoch milliseconds. Rows failing a rule are skipped and the rest imported; a row with too few columns is skipped with "Expected at least N columns (...), got M"; any other failure is skipped with the error message or "Invalid data format".
- weight: at least 2 columns; weight must be within 1.0 to 700.0 kg; BMI is always computed from the profile height (absent when the profile has no height); a bmi column in the file is ignored
- blood pressure: at least 3 columns; values are whole numbers subject to the blood-pressure ranges; the category is always computed; an optional 4th column is ignored
- glucose: at least 3 columns; value in mmol/L within range; context name case-insensitive; the category is always computed; an optional 4th column is ignored
- activity: at least 3 columns (start, end, distance in meters); subject to activity rules

BMI and categories are derived data: they SHALL be recomputed on import and values in the file are never trusted.

#### Scenario: File values ignored
- **WHEN** a weight row says bmi 99.9 for 81.0 kg and the profile height is 180 cm
- **THEN** the stored BMI is 25.0

#### Scenario: Malformed row skipped
- **WHEN** a row has an unparseable timestamp or an out-of-range value
- **THEN** it is skipped with its line number and reason and the valid rows are imported

#### Scenario: Duplicates allowed
- **WHEN** a row has the same timestamp as an existing entry
- **THEN** it is imported as an additional entry

### Requirement: Import weight from Libra CSV
When the metric is `libra`/`libra_weight`, or the metric is `weight` and the content looks like Libra (any line starting with `#Version:`, `#Units:`, `#date;` or `date;`), the Libra rules apply. Fields are semicolon-separated, cells trimmed and surrounding quotes removed. Lines starting with `#` are skipped; `#date;`/`#date,` marks the header, and a plain `date;`/`date,` header line is skipped. Unit is read from `#Units:`: `kg` means kilograms and `lbs` means pounds (case-insensitive); an absent unit means kilograms. Any other unit SHALL skip every data row with the reason "Unsupported Libra unit: X (expected kg or lbs)" and import nothing, rather than guessing a conversion. Pounds convert with 1 lb = 0.45359237 kg rounded to 2 decimals (half up). The weight is in the second column; other columns are ignored. A decimal comma is accepted. Timestamps may be a full ISO instant (with Z or offset), a local date-time without offset (interpreted as UTC) or a date only (start of day UTC). BMI is computed from the profile height. Line numbers are physical file lines, including comment lines. When a Libra import is explicitly chosen, UI errors use a Libra-specific message.

#### Scenario: Pounds converted
- **WHEN** a Libra file has `#Units: lbs` and a row weight of 165.0
- **THEN** 74.84 kg is stored

#### Scenario: Blank weight
- **WHEN** a row has a blank weight
- **THEN** it is skipped with reason "Weight value is blank"

#### Scenario: Unknown unit
- **WHEN** a Libra file has `#Units: st` and two data rows
- **THEN** both rows are skipped with an unsupported-unit reason and nothing is imported

#### Scenario: Out of range after conversion
- **WHEN** the converted weight is outside 1.0 to 700.0 kg
- **THEN** the row is skipped with a message stating the weight is outside the physiological range [1.0, 700.0] kg

#### Scenario: Metadata ignored
- **WHEN** the file contains `#Version:` and `#Units:` comments and extra columns
- **THEN** they are ignored while date and weight are read

### Requirement: Import feedback in the UI
After an import the UI SHALL show the number of imported rows and the number of skipped rows. Activity import has no UI entry point.

#### Scenario: Summary shown
- **WHEN** 3 rows import and 1 is skipped
- **THEN** the UI reports 3 imported and 1 skipped
