## MODIFIED Requirements

### Requirement: CSV conventions
Exported files SHALL be UTF-8. Every row, including the header, SHALL end with a line feed. The separator is a comma and the decimal separator is always a point, independent of region. All values are metric canonical units regardless of display settings. Timestamps SHALL be ISO-8601 in UTC with a trailing Z (for example 2026-09-20T08:00:00Z); fractional seconds appear only when non-zero. A cell that contains a comma, a double quote or a line break SHALL be wrapped in double quotes with inner double quotes doubled (RFC 4180), and import SHALL read such cells back to the original text.

#### Scenario: Locale independence
- **WHEN** the region uses a decimal comma and weight is exported
- **THEN** the weight in the file still uses a decimal point

#### Scenario: Comma in a comment
- **WHEN** a comment "dizzy, then fine" is exported
- **THEN** the cell is `"dizzy, then fine"` and import returns the same text

#### Scenario: Quote in a comment
- **WHEN** a comment `he said "ok"` is exported
- **THEN** the cell is `"he said ""ok"""` and import returns the original text

### Requirement: Export metric history
Export SHALL write the header row followed by one row per measurement of the profile, sorted ascending by timestamp (activity by start timestamp). Headers and columns:
- weight: `timestamp,weight_kg,bmi,comment` (weight as plain decimal; bmi empty when absent)
- blood pressure: `timestamp,systolic_mmhg,diastolic_mmhg,pulse_bpm,classification,comment` (pulse is empty when absent; classification is the band name: NORMAL, HIGH or SERIOUSLY_RAISED)
- glucose: `timestamp,glucose_mmol_l,context,classification,comment` (context FASTING or POSTPRANDIAL; classification is the category name, never translated)
- waist circumference: `timestamp,waist_cm,classification,comment` (classification is empty when the profile has no sex set)
- activity: `start_timestamp,end_timestamp,distance_m,duration_s,comment` (distance in meters as decimal such as 5000.0; duration in whole seconds)
The comment column is empty when an entry has no comment.

The UI SHALL offer export for weight, blood pressure, glucose and waist circumference and SHALL state that comments are included. Activity export is supported by the contract but has no UI entry point.

#### Scenario: Weight exported
- **WHEN** two weights are exported
- **THEN** the file has the header `timestamp,weight_kg,bmi,comment` and two rows, oldest first

#### Scenario: Empty history
- **WHEN** the profile has no measurements of the metric
- **THEN** the file contains only the header row

#### Scenario: Blood pressure band names
- **WHEN** a reading of 150/95 is exported
- **THEN** the classification column is HIGH

#### Scenario: Blood pressure file without pulse
- **WHEN** a blood pressure file with the older header `timestamp,systolic_mmhg,diastolic_mmhg,classification` is imported
- **THEN** the rows are accepted and their pulse is unset

#### Scenario: File from an earlier version
- **WHEN** a file with the classification HYPERTENSION_GRADE_1 is imported
- **THEN** the row is accepted and its band is computed from the values

#### Scenario: Comment round trip
- **WHEN** an entry with a comment is exported and the file is imported into an empty profile
- **THEN** the imported entry has the same comment

### Requirement: Standard row rules
Cells are split on commas (respecting double quotes) and trimmed. Timestamps SHALL be parsed as an ISO-8601 instant, otherwise as epoch milliseconds. Rows failing a rule are skipped and the rest imported; a row with too few columns is skipped with "Expected at least N columns (...), got M"; any other failure is skipped with the error message or "Invalid data format".
- weight: at least 2 columns; weight must be within 1.0 to 700.0 kg; BMI is always computed from the profile height (absent when the profile has no height); a bmi column in the file is ignored
- blood pressure: at least 3 columns; values are whole numbers subject to the blood-pressure ranges; the category is always computed; an optional 4th column is ignored
- glucose: at least 3 columns; value in mmol/L within range; context name case-insensitive; the category is always computed; an optional 4th column is ignored
- activity: at least 3 columns (start, end, distance in meters); subject to activity rules
- comment: when the header has a `comment` column, its cell is read, normalised by the `entry-comments` rules and attached to the entry; a header without the column means no comments; a comment over 200 characters skips the row with the reason "Comment longer than 200 characters"

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

#### Scenario: Older file without comment column
- **WHEN** a weight file with the header `timestamp,weight_kg,bmi` is imported
- **THEN** the rows are imported with no comment

#### Scenario: Comment too long
- **WHEN** a row has a 201-character comment
- **THEN** it is skipped with the reason "Comment longer than 200 characters" and its line number

### Requirement: Import weight from Libra CSV
When the metric is `libra`/`libra_weight`, or the metric is `weight` and the content looks like Libra (any line starting with `#Version:`, `#Units:`, `#date;` or `date;`), the Libra rules apply. Fields are semicolon-separated, cells trimmed and surrounding quotes removed. Lines starting with `#` are skipped; `#date;`/`#date,` marks the header, and a plain `date;`/`date,` header line is skipped. Unit is read from `#Units:`: `kg` means kilograms and `lbs` means pounds (case-insensitive); an absent unit means kilograms. Any other unit SHALL skip every data row with the reason "Unsupported Libra unit: X (expected kg or lbs)" and import nothing, rather than guessing a conversion. Pounds convert with 1 lb = 0.45359237 kg rounded to 2 decimals (half up). The weight is in the second column and the `comments` column, when present, becomes the entry comment (normalised by the `entry-comments` rules and clipped to its first 200 characters when longer, never skipping the row); other columns are ignored. A decimal comma is accepted. Timestamps may be a full ISO instant (with Z or offset), a local date-time without offset (interpreted as UTC) or a date only (start of day UTC). BMI is computed from the profile height. Line numbers are physical file lines, including comment lines. When a Libra import is explicitly chosen, UI errors use a Libra-specific message.

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
- **THEN** they are ignored while date, weight and comments are read

#### Scenario: Libra comment imported
- **WHEN** a Libra row has the comments cell "test comment"
- **THEN** the imported weight has the comment "test comment"

#### Scenario: Libra comment too long is clipped
- **WHEN** a Libra row has a comments cell longer than 200 characters
- **THEN** the row is imported with the comment cut to its first 200 characters (trailing whitespace removed) and is not skipped
