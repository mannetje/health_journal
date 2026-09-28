## ADDED Requirements

### Requirement: Record waist circumference measurement
The system SHALL allow recording a waist circumference measurement in centimetres for a Profile at a given timestamp. This metric is optional — a Profile with no waist circumference measurements SHALL function identically to today, with no other feature affected.

#### Scenario: Valid waist circumference recorded
- **WHEN** a client provides a Profile identifier, a waist circumference between 40 and 200 cm, and a measurement timestamp
- **THEN** the system SHALL persist the measurement and return its identifier

#### Scenario: Waist circumference out of range rejected
- **WHEN** a client provides a waist circumference value less than 40 cm or greater than 200 cm
- **THEN** the system SHALL reject the request with a validation error

### Requirement: Classify waist circumference against sex-specific NHG/Voedingscentrum categories
The system SHALL classify a waist circumference measurement against sex-specific healthy-range categories (Healthy, Increased Risk, High Risk) sourced from Voedingscentrum guidance, when the Profile's sex is set.

#### Scenario: Classified for a female Profile
- **WHEN** a waist circumference measurement is recorded for a Profile with sex set to female
- **THEN** the system SHALL classify it as HEALTHY below 80 cm, INCREASED_RISK from 80 cm up to 88 cm, and HIGH_RISK at 88 cm or above

#### Scenario: Classified for a male Profile
- **WHEN** a waist circumference measurement is recorded for a Profile with sex set to male
- **THEN** the system SHALL classify it as HEALTHY below 94 cm, INCREASED_RISK from 94 cm up to 102 cm, and HIGH_RISK at 102 cm or above

#### Scenario: Unclassified without sex
- **WHEN** a waist circumference measurement is recorded for a Profile with no sex set
- **THEN** the system SHALL return a null/absent category, not an error, and SHALL still persist the measurement

### Requirement: Retrieve waist circumference history
The system SHALL allow retrieving all waist circumference measurements for a Profile in reverse chronological order.

#### Scenario: History returned for known profile
- **WHEN** a client requests waist circumference history for a Profile with measurements
- **THEN** the system SHALL return all measurements ordered from most recent to oldest
