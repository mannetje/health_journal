# glucose Specification

## Purpose
Defines blood glucose logging, unit conversion and the NHG classification. The canonical (stored) unit is mmol/L; users may enter and view mg/dL (see units-presentation).

## Requirements

### Requirement: Canonical value and range
A glucose level SHALL be stored in mmol/L, rounded to 2 decimals (half up), and SHALL be within the inclusive range 0.5 to 55.0. Values outside are rejected.

#### Scenario: Boundaries
- **WHEN** a level is 0.49 or 55.01 mmol/L
- **THEN** it is rejected
- **WHEN** a level is 0.5 or 55.0 mmol/L
- **THEN** it is accepted

### Requirement: mg/dL conversion
mg/dL input SHALL be converted with mmol/L = mg/dL x 0.0555, rounded to 2 decimals (half up). Display conversion from mmol/L to mg/dL SHALL divide by 0.0555. Recording requires either an mmol/L or an mg/dL value; updating an entry takes mmol/L.

#### Scenario: Conversion on record
- **WHEN** 100 mg/dL is recorded
- **THEN** 5.55 mmol/L is stored

#### Scenario: Neither value supplied
- **WHEN** a record is attempted without any value
- **THEN** it is rejected

### Requirement: Measurement context
Each entry SHALL have a context: FASTING or POSTPRANDIAL. Labels are localized (see localization). The context is stored as its enum name.

#### Scenario: Context stored
- **WHEN** a postprandial reading is saved
- **THEN** the context POSTPRANDIAL is stored

### Requirement: NHG classification
The classifier SHALL use these bands.
FASTING: HYPOGLYCAEMIA below 3.9; NORMAL from 3.9 to 6.0 inclusive; IMPAIRED_FASTING above 6.0 up to 6.9 inclusive; DIABETES_RANGE above 6.9.
POSTPRANDIAL: HYPOGLYCAEMIA below 3.9; NORMAL from 3.9 up to but excluding 7.8; IMPAIRED_GLUCOSE_TOLERANCE from 7.8 to 11.0 inclusive; DIABETES_RANGE above 11.0.
The category is computed on the stored mmol/L value, stored with the entry, and recomputed on update. The category names are internal identifiers that are stored and exported but never shown to the user. The shown names are Low, Normal, Slightly raised and High blood glucose, with the ranges defined in `range-labels`.

#### Scenario: Fasting boundaries
- **WHEN** fasting levels are 3.8, 3.9, 6.0, 6.1, 6.9 and 7.0
- **THEN** categories are HYPOGLYCAEMIA, NORMAL, NORMAL, IMPAIRED_FASTING, IMPAIRED_FASTING and DIABETES_RANGE

#### Scenario: Postprandial boundaries
- **WHEN** postprandial levels are 7.7, 7.8, 11.0 and 11.1
- **THEN** categories are NORMAL, IMPAIRED_GLUCOSE_TOLERANCE, IMPAIRED_GLUCOSE_TOLERANCE and DIABETES_RANGE

#### Scenario: Internal names not shown
- **WHEN** an entry with category DIABETES_RANGE is displayed
- **THEN** the screen shows "High blood glucose" with its range and never the category name

### Requirement: Live preview and validation
The UI SHALL show a category preview only when the entered value is greater than 0 and the resulting level is within range. Invalid input SHALL show a localized validation error and store nothing.

#### Scenario: Out-of-range preview
- **WHEN** 0.1 mmol/L is typed
- **THEN** no preview is shown

### Requirement: Persistence contract
Readings SHALL be stored in table "glucoses": id, profile id, timestamp (epoch ms), level in mmol/L (floating point), context (enum name), category (enum name). Storage is always mmol/L regardless of display unit.

#### Scenario: Round trip
- **WHEN** a reading is stored and read back
- **THEN** level, context and category are equal
