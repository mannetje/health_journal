## Purpose

Records blood glucose measurements for a Profile. The primary unit is mmol/L (as required by Dutch NHG guidelines). Supports optional conversion from mg/dL. Classifies readings as fasting or postprandial and evaluates them against NHG reference ranges.

## ADDED Requirements

### Requirement: Record glucose measurement
The system SHALL allow recording a blood glucose measurement expressed in mmol/L, associated with a Profile, a context (fasting or postprandial), and a timestamp.

#### Scenario: Valid fasting measurement recorded
- **WHEN** a client provides a glucose value in mmol/L between 0.5 and 55.0, context FASTING, and a timestamp
- **THEN** the system SHALL persist the measurement and return its identifier

#### Scenario: Valid postprandial measurement recorded
- **WHEN** a client provides a glucose value in mmol/L between 0.5 and 55.0, context POSTPRANDIAL, and a timestamp
- **THEN** the system SHALL persist the measurement and return its identifier

#### Scenario: Value out of range rejected
- **WHEN** a client provides a glucose value outside 0.5–55.0 mmol/L
- **THEN** the system SHALL reject the measurement with a validation error

### Requirement: Convert mg/dL input to mmol/L
The system SHALL accept an input in mg/dL and store the value converted to mmol/L using the factor 1 mg/dL = 0.0555 mmol/L, rounded to two decimal places.

#### Scenario: mg/dL converted and stored as mmol/L
- **WHEN** a client provides a glucose value in mg/dL
- **THEN** the system SHALL convert it to mmol/L before persistence and expose only mmol/L externally

### Requirement: Classify glucose against NHG thresholds
The system SHALL classify a glucose reading against NHG reference ranges based on its measurement context.

#### Scenario: Fasting glucose classified as normal
- **WHEN** a fasting reading is between 3.5 and 6.0 mmol/L (inclusive)
- **THEN** the system SHALL classify it as NORMAL

#### Scenario: Fasting glucose classified as impaired fasting glucose
- **WHEN** a fasting reading is between 6.1 and 6.9 mmol/L (inclusive)
- **THEN** the system SHALL classify it as IMPAIRED_FASTING

#### Scenario: Fasting glucose classified as diabetic range
- **WHEN** a fasting reading is 7.0 mmol/L or above
- **THEN** the system SHALL classify it as DIABETES_RANGE

#### Scenario: Fasting glucose classified as hypoglycaemia
- **WHEN** a fasting reading is below 3.5 mmol/L
- **THEN** the system SHALL classify it as HYPOGLYCAEMIA

#### Scenario: Postprandial glucose classified as normal
- **WHEN** a postprandial reading (2h post-meal) is below 7.8 mmol/L
- **THEN** the system SHALL classify it as NORMAL

#### Scenario: Postprandial glucose classified as impaired
- **WHEN** a postprandial reading is between 7.8 and 11.0 mmol/L (inclusive)
- **THEN** the system SHALL classify it as IMPAIRED_GLUCOSE_TOLERANCE

#### Scenario: Postprandial glucose classified as diabetic range
- **WHEN** a postprandial reading is above 11.0 mmol/L
- **THEN** the system SHALL classify it as DIABETES_RANGE

### Requirement: Retrieve glucose history
The system SHALL allow retrieving all glucose measurements for a Profile in reverse chronological order.

#### Scenario: History returned for known profile
- **WHEN** a client requests glucose history for a Profile with measurements
- **THEN** the system SHALL return all measurements ordered from most recent to oldest, each including its context and classification
