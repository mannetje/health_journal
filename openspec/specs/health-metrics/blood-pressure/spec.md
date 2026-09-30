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
The category SHALL be evaluated top-down, the first matching rule wins:
1. GRADE_3 when systolic >= 180 or diastolic >= 110
2. GRADE_2 when systolic >= 160 or diastolic >= 100
3. GRADE_1 when systolic >= 140 or diastolic >= 90
4. HIGH_NORMAL when systolic is 130 to 139 or diastolic is 85 to 89
5. NORMAL when (systolic 120 to 129 and diastolic < 80) or (systolic < 130 and diastolic 80 to 84)
6. OPTIMAL when systolic < 120 and diastolic < 80

The higher of the systolic and diastolic categories therefore wins.

#### Scenario: Examples
- **WHEN** readings are 115/75, 125/78, 118/82, 135/80, 128/88, 145/85, 150/100, 165/90, 185/95 and 120/112
- **THEN** categories are OPTIMAL, NORMAL, NORMAL, HIGH_NORMAL, HIGH_NORMAL, GRADE_1, GRADE_2, GRADE_2, GRADE_3 and GRADE_3

### Requirement: Stored category
The category SHALL be computed and stored with the entry when it is recorded and recomputed when the entry is updated. Recording uses the current time as timestamp.

#### Scenario: Update reclassifies
- **WHEN** an entry of 118/75 is edited to 150/95
- **THEN** its stored category becomes GRADE_1

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
