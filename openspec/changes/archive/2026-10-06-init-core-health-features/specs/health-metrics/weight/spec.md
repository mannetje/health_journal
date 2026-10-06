## Purpose

Records body weight measurements over time for a Profile. Supports BMI calculation when the Profile has a height set. Evaluates weight status against WHO/NHG BMI categories.

## ADDED Requirements

### Requirement: Record weight measurement
The system SHALL allow recording a body weight measurement in kilograms for a Profile at a given timestamp.

#### Scenario: Valid weight recorded
- **WHEN** a client provides a Profile identifier, a weight between 1.0 kg and 700.0 kg, and a measurement timestamp
- **THEN** the system SHALL persist the measurement and return its identifier

#### Scenario: Weight out of range rejected
- **WHEN** a client provides a weight value less than 1.0 kg or greater than 700.0 kg
- **THEN** the system SHALL reject the request with a validation error

### Requirement: Calculate BMI
The system SHALL calculate BMI (Body Mass Index) from a weight measurement when the Profile has a height recorded.

#### Scenario: BMI calculated with height available
- **WHEN** a weight measurement is recorded and the Profile has a height set
- **THEN** the system SHALL return the BMI value rounded to one decimal place using the formula: weight(kg) / (height(m))²

#### Scenario: BMI unavailable without height
- **WHEN** a weight measurement is recorded and the Profile has no height set
- **THEN** the system SHALL return a null/absent BMI value, not an error

### Requirement: Classify BMI against NHG categories
The system SHALL classify a calculated BMI value against NHG/WHO standard categories.

#### Scenario: BMI classified as underweight
- **WHEN** BMI is below 18.5
- **THEN** the system SHALL classify it as UNDERWEIGHT

#### Scenario: BMI classified as normal
- **WHEN** BMI is between 18.5 (inclusive) and 25.0 (exclusive)
- **THEN** the system SHALL classify it as NORMAL

#### Scenario: BMI classified as overweight
- **WHEN** BMI is between 25.0 (inclusive) and 30.0 (exclusive)
- **THEN** the system SHALL classify it as OVERWEIGHT

#### Scenario: BMI classified as obese
- **WHEN** BMI is 30.0 or above
- **THEN** the system SHALL classify it as OBESE

### Requirement: Retrieve weight history
The system SHALL allow retrieving all weight measurements for a Profile in reverse chronological order.

#### Scenario: History returned for known profile
- **WHEN** a client requests weight history for a Profile with measurements
- **THEN** the system SHALL return all measurements ordered from most recent to oldest
