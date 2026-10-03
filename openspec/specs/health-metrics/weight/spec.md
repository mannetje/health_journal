# weight Specification

## Purpose
Defines weight logging, BMI calculation and the NHG BMI categories. Weight is always stored in kilograms.

## Requirements

### Requirement: Weight value range
A weight SHALL be a decimal number of kilograms in the inclusive range 1.0 to 700.0. Values outside the range SHALL be rejected. Weights entered in pounds are converted to kilograms first (see units-presentation) and stored rounded to 2 decimals (half up).

#### Scenario: Boundaries
- **WHEN** weight is 0.9 or 700.1 kg
- **THEN** it is rejected
- **WHEN** weight is 1.0 or 700.0 kg
- **THEN** it is accepted

### Requirement: Weight entry
A weight entry SHALL consist of an identifier (UUID v4 or v7), the owning profile, a timestamp (instant), the weight in kg and an optional BMI. Recording SHALL use the current time as timestamp.

#### Scenario: Record weight
- **WHEN** 75 kg is recorded for the active profile
- **THEN** an entry with the current timestamp and 75 kg is stored and appears in history

### Requirement: BMI calculation
BMI SHALL be weight (kg) divided by the square of height in meters. Height in meters is centimeters divided by 100 computed to 4 decimals (half up). The BMI result SHALL be rounded to 1 decimal (half up). When the profile has no height, or the profile cannot be found, BMI SHALL be absent. BMI SHALL be computed and stored on the entry when it is recorded and recomputed when the entry is updated; it SHALL NOT be recomputed when the profile height later changes.

#### Scenario: BMI with height
- **WHEN** 75 kg is recorded for a profile of 180 cm
- **THEN** the stored BMI is 23.1

#### Scenario: No height
- **WHEN** a weight is recorded for a profile without height
- **THEN** BMI is absent

### Requirement: NHG BMI categories
The BMI category SHALL be UNDERWEIGHT below 18.5; NORMAL from 18.5 up to but excluding 25.0; OVERWEIGHT from 25.0 up to but excluding 30.0; OBESE from 30.0 upward. The shown label is the class name followed by its BMI range, as defined in `range-labels`, and never describes the class as a condition.

#### Scenario: Boundaries
- **WHEN** BMI is 18.4, 18.5, 24.9, 25.0, 29.9 or 30.0
- **THEN** the categories are UNDERWEIGHT, NORMAL, NORMAL, OVERWEIGHT, OVERWEIGHT and OBESE respectively

#### Scenario: Label with range
- **WHEN** a BMI of 31.2 is shown in English
- **THEN** the label is "Obese · BMI 30 and above"

### Requirement: Live BMI preview
While logging, the UI SHALL show a BMI preview only when the entered weight is within 1 to 700 kg, an active profile exists and that profile has a height. Otherwise no preview is shown.

#### Scenario: No preview without height
- **WHEN** the profile has no height
- **THEN** no BMI preview is shown

### Requirement: Weight logging validation feedback
A blank, non-numeric or out-of-range weight SHALL show a localized validation error and SHALL NOT be stored. Without an active profile the record action is disabled and the UI asks to create a profile first.

#### Scenario: Invalid input
- **WHEN** the user records "abc"
- **THEN** a weight validation error is shown and nothing is stored

### Requirement: Persistence contract
Weights SHALL be stored in table "weights": id, profile id, timestamp (epoch milliseconds), weight in kg (floating point) and BMI (nullable floating point). History ordering is by timestamp descending.

#### Scenario: Round trip
- **WHEN** an entry is stored and read back
- **THEN** id, profile, timestamp, weight and BMI are equal to those stored
