# data-export Specification

## Purpose
Defines the CSV contract for exporting a profile's measurement history and importing measurements (including the Libra weight format). The CSV format is a stable interchange contract; a port to another platform SHALL read and write identical files.

## Requirements

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

### Requirement: Import measurements
Import SHALL take a metric type name and the file content and return an import result: the number of rows imported and the list of skipped rows (line number, reason). The metric type name is lowercased with "-" replaced by "_"; accepted names are `weight`, `blood_pressure` (also `bloodpressure`, `bp`), `glucose`, `activity`, and `libra` / `libra_weight`. Any other name skips every row with reason "Unsupported metric type: X". Blank content imports nothing and skips nothing. Blank lines are ignored, but reported line numbers are the physical line numbers of the file (blank lines count), so they match what a text editor shows. A file with only a header imports 0 rows. Each imported row SHALL receive a new identifier and be linked to the active profile. Import with no active profile SHALL do nothing. Skip reasons are technical English text and are not localized.

#### Scenario: Valid file
- **WHEN** a weight file with two valid rows is imported
- **THEN** the result is 2 imported and no skipped rows

#### Scenario: Unknown metric
- **WHEN** the metric name is "steps"
- **THEN** all rows are skipped with "Unsupported metric type: steps"

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

### Requirement: Import feedback in the UI
After an import the UI SHALL show the number of imported rows and the number of skipped rows. Activity import has no UI entry point.

#### Scenario: Summary shown
- **WHEN** 3 rows import and 1 is skipped
- **THEN** the UI reports 3 imported and 1 skipped

### Requirement: Export medication data
Export SHALL write three CSV files for a profile, following the conventions of this spec (UTF-8, comma separator, decimal point, line feed endings, ISO-8601 UTC instants). Doses and strengths are exported exactly as entered, with their unit, and are never converted. Planned times are wall-clock values and SHALL NOT carry a time zone. Comments follow the existing comment column rules (quoting, 200 characters).
- medications: `ref,name,form,strength_amount,strength_unit,dose_amount,dose_unit,as_needed,colour,shape,comment,archived`
- medication schedule (one row per daily time of each schedule version, and one row with an empty `local_time` for an as-needed version): `ref,effective_from,local_time,days_of_week,interval_days,start_date,end_date` (`days_of_week` is a pipe-separated list of MON to SUN, empty when an interval is used; `interval_days` is a whole number, empty when weekdays are used)
- intakes: `ref,planned_local_datetime,status,actual_timestamp,actual_amount,comment` (status TAKEN, SKIPPED or AS_NEEDED; `planned_local_datetime` is empty for as-needed doses, whose `actual_timestamp` is required; pending and missed are never stored, because they are derived)

`form`, `strength_unit`, `dose_unit`, `colour` and `shape` are the language-independent enum names, never translated labels. `ref` is a per-export identifier used only to link rows across the three files.

#### Scenario: Language independence
- **WHEN** the app language is Dutch and a medication with form liquid and dose unit ml is exported
- **THEN** the file contains `LIQUID` and `ML`, not the Dutch labels

#### Scenario: Doses unchanged
- **WHEN** a dose of 2.5 ml is exported with a Dutch region
- **THEN** the file contains `2.5` and `ML`

#### Scenario: Schedule versions exported
- **WHEN** a schedule was changed from 08:00 to 09:00 effective from a date
- **THEN** the schedule file contains a row set for each version with its `effective_from`

#### Scenario: Derived status not exported
- **WHEN** a planned intake has no outcome
- **THEN** it does not appear in the intakes file

### Requirement: Import medication data
Import SHALL accept the three files, link rows through `ref` within the batch, assign new identifiers, and attach everything to the active profile. Invalid rows are skipped with their physical line number and a reason, and the rest are imported. A schedule or intake row whose `ref` does not exist in the batch is skipped. Medications SHALL be validated with the same rules as manual entry, and a comment over 200 characters SHALL skip its row like the standard CSV import does. A medication that matches one already in the profile (same name, form, strength and dose) SHALL NOT be created again, and its rows link to the existing one. A planned intake with the same medication and planned time as an existing one, and an as-needed dose with the same medication and timestamp as an existing one, SHALL be ignored without an error, so importing the same files twice does not duplicate the medications or the log. A medication with no valid schedule SHALL be skipped. Import with no active profile SHALL do nothing.

#### Scenario: Round trip
- **WHEN** a profile is exported and the files are imported into an empty profile
- **THEN** the medications, schedule versions and intake log are identical

#### Scenario: Import twice
- **WHEN** the same intakes file is imported twice
- **THEN** the second import adds no medications or intakes and reports no skipped rows, for planned and as-needed doses alike

#### Scenario: Unknown enum value
- **WHEN** a row has a form or unit that is not in the contract
- **THEN** the row is skipped with a reason and nothing is guessed

#### Scenario: Orphan row
- **WHEN** an intake row refers to a `ref` that is not in the medications file
- **THEN** it is skipped with its line number and the name of the file it came from

#### Scenario: Over-long comment
- **WHEN** a row has a comment of 201 characters
- **THEN** the row is skipped with a reason
