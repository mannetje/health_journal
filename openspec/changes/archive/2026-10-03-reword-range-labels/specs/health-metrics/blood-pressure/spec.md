## MODIFIED Requirements

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
