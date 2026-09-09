## Purpose

Records systolic and diastolic blood pressure readings in mmHg for a Profile and classifies them against Dutch NHG (Nederlands Huisartsen Genootschap) guideline thresholds.

## ADDED Requirements

### Requirement: Record blood pressure measurement
The system SHALL allow recording a blood pressure reading consisting of a systolic and diastolic value in mmHg, associated with a Profile and a timestamp.

#### Scenario: Valid reading recorded
- **WHEN** a client provides a Profile identifier, a systolic value between 40 and 300 mmHg, a diastolic value between 20 and 200 mmHg, and a timestamp
- **THEN** the system SHALL persist the reading and return its identifier

#### Scenario: Systolic below diastolic rejected
- **WHEN** a client provides a systolic value that is less than or equal to the diastolic value
- **THEN** the system SHALL reject the reading with a validation error

#### Scenario: Value out of physiological range rejected
- **WHEN** either value falls outside the accepted physiological range
- **THEN** the system SHALL reject the reading with a validation error

### Requirement: Classify blood pressure against NHG thresholds
The system SHALL classify a blood pressure reading against NHG guideline categories.

#### Scenario: Classified as optimal
- **WHEN** systolic is below 120 AND diastolic is below 80
- **THEN** the system SHALL classify the reading as OPTIMAL

#### Scenario: Classified as normal
- **WHEN** systolic is between 120 and 129 (inclusive) AND diastolic is below 80, OR systolic is below 130 AND diastolic is between 80 and 84 (inclusive)
- **THEN** the system SHALL classify the reading as NORMAL

#### Scenario: Classified as high normal
- **WHEN** systolic is between 130 and 139 (inclusive) OR diastolic is between 85 and 89 (inclusive)
- **THEN** the system SHALL classify the reading as HIGH_NORMAL

#### Scenario: Classified as hypertension grade 1
- **WHEN** systolic is between 140 and 159 (inclusive) OR diastolic is between 90 and 99 (inclusive)
- **THEN** the system SHALL classify the reading as HYPERTENSION_GRADE_1

#### Scenario: Classified as hypertension grade 2
- **WHEN** systolic is between 160 and 179 (inclusive) OR diastolic is between 100 and 109 (inclusive)
- **THEN** the system SHALL classify the reading as HYPERTENSION_GRADE_2

#### Scenario: Classified as hypertension grade 3
- **WHEN** systolic is 180 or above OR diastolic is 110 or above
- **THEN** the system SHALL classify the reading as HYPERTENSION_GRADE_3

### Requirement: Retrieve blood pressure history
The system SHALL allow retrieving all blood pressure readings for a Profile in reverse chronological order.

#### Scenario: History returned for known profile
- **WHEN** a client requests blood pressure history for a Profile with readings
- **THEN** the system SHALL return all readings ordered from most recent to oldest, each including classification
