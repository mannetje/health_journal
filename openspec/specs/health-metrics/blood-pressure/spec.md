# blood-pressure Specification

## Purpose
Defines blood pressure logging and the NHG classification. Values are in mmHg.

## Requirements

### Requirement: Reading ranges
A reading SHALL have systolic in the inclusive range 40 to 300 and diastolic in the inclusive range 20 to 200 (whole numbers, mmHg), and systolic SHALL be strictly greater than diastolic. Violations SHALL be rejected.

#### Scenario: Systolic not above diastolic
- **WHEN** 80/80 is recorded
- **THEN** it is rejected

#### Scenario: Range boundaries
- **WHEN** systolic is 39 or 301, or diastolic is 19 or 201
- **THEN** it is rejected
- **WHEN** 300/200 or 40/20 is recorded
- **THEN** it is accepted

### Requirement: NHG classification
The band SHALL be evaluated top-down, the first matching rule wins:
1. SERIOUSLY_RAISED when systolic >= 180 or diastolic >= 110
2. HIGH when systolic >= 140 or diastolic >= 90
3. NORMAL otherwise

The higher of the systolic and diastolic bands therefore wins. The limits 140 and 180 follow NHG. The shown names and ranges are defined in `range-labels`.

#### Scenario: Examples
- **WHEN** readings are 115/75, 125/78, 135/80, 128/88, 145/85, 150/100, 165/90, 185/95 and 120/112
- **THEN** bands are NORMAL, NORMAL, NORMAL, NORMAL, HIGH, HIGH, HIGH, SERIOUSLY_RAISED and SERIOUSLY_RAISED

### Requirement: Stored category
The band SHALL be computed and stored with the entry when it is recorded and recomputed when the entry is updated, using the names NORMAL, HIGH and SERIOUSLY_RAISED. Rows stored with the earlier names SHALL be read as follows and need no rewrite: OPTIMAL, NORMAL and HIGH_NORMAL as NORMAL; HYPERTENSION_GRADE_1 and HYPERTENSION_GRADE_2 as HIGH; HYPERTENSION_GRADE_3 as SERIOUSLY_RAISED. Recording uses the current time as timestamp.

#### Scenario: Update reclassifies
- **WHEN** an entry of 118/75 is edited to 150/95
- **THEN** its stored category becomes HIGH

#### Scenario: Legacy row
- **WHEN** a row stored with HYPERTENSION_GRADE_2 is read
- **THEN** its category is HIGH and the stored row is unchanged

### Requirement: Live preview and validation feedback
The UI SHALL show a category preview only when systolic is 40 to 300, diastolic is 20 to 200 and systolic > diastolic. Blank, non-numeric or invalid values SHALL show localized validation errors for systolic and diastolic and store nothing.

#### Scenario: Preview hidden for invalid pair
- **WHEN** 90/95 is typed
- **THEN** no preview is shown

### Requirement: Persistence contract
Readings SHALL be stored in table "blood_pressures": id, profile id, timestamp (epoch ms), systolic, diastolic, category (enum name as text).

#### Scenario: Round trip
- **WHEN** a reading is stored and read back
- **THEN** all fields including the category name are equal
