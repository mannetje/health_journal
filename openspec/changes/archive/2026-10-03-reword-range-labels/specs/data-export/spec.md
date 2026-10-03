## MODIFIED Requirements

### Requirement: Export metric history
Export SHALL write the header row followed by one row per measurement of the profile, sorted ascending by timestamp (activity by start timestamp). Headers and columns:
- weight: `timestamp,weight_kg,bmi` (weight as plain decimal; bmi empty when absent)
- blood pressure: `timestamp,systolic_mmhg,diastolic_mmhg,classification` (classification is the band name: NORMAL, HIGH or SERIOUSLY_RAISED)
- glucose: `timestamp,glucose_mmol_l,context,classification` (context FASTING or POSTPRANDIAL; classification is the category name, never translated)
- activity: `start_timestamp,end_timestamp,distance_m,duration_s` (distance in meters as decimal such as 5000.0; duration in whole seconds)

The UI SHALL offer export for weight, blood pressure and glucose. Activity export is supported by the contract but has no UI entry point.

#### Scenario: Weight exported
- **WHEN** two weights are exported
- **THEN** the file has the header `timestamp,weight_kg,bmi` and two rows, oldest first

#### Scenario: Empty history
- **WHEN** the profile has no measurements of the metric
- **THEN** the file contains only the header row

#### Scenario: Blood pressure band names
- **WHEN** a reading of 150/95 is exported
- **THEN** the classification column is HIGH

#### Scenario: File from an earlier version
- **WHEN** a file with the classification HYPERTENSION_GRADE_1 is imported
- **THEN** the row is accepted and its band is computed from the values
