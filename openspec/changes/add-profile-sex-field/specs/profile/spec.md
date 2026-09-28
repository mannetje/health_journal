## MODIFIED Requirements

### Requirement: Create profile
The system SHALL allow creating a new Profile with a full name, date-of-birth, optional height in centimetres, and optional sex (male/female).

#### Scenario: Valid profile creation
- **WHEN** a client provides a non-empty name, a valid date-of-birth (not in the future, not more than 130 years ago), an optional height between 50 and 300 cm, and an optional sex
- **THEN** the system SHALL persist the Profile and return its generated identifier

#### Scenario: Missing name rejected
- **WHEN** a client provides a blank or empty name
- **THEN** the system SHALL reject the request with a validation error

#### Scenario: Future date-of-birth rejected
- **WHEN** a client provides a date-of-birth that is today or in the future
- **THEN** the system SHALL reject the request with a validation error

#### Scenario: Sex not provided
- **WHEN** a client creates or updates a Profile without providing a sex value
- **THEN** the system SHALL persist the Profile with sex unset (not specified), without rejecting the request

### Requirement: Read profile
The system SHALL allow retrieving a previously created Profile by its identifier.

#### Scenario: Existing profile returned
- **WHEN** a client requests a Profile by a known identifier
- **THEN** the system SHALL return the Profile including name, date-of-birth, height (if set), and sex (if set)

#### Scenario: Unknown identifier
- **WHEN** a client requests a Profile by an identifier that does not exist
- **THEN** the system SHALL return a not-found result (no error thrown)

## ADDED Requirements

### Requirement: Sex is not used in health-metric calculations
The system SHALL NOT use a Profile's sex value in BMI, blood pressure, or glucose classification, since none of the app's current NHG-derived formulas or thresholds are sex-dependent.

#### Scenario: BMI calculated regardless of sex
- **WHEN** BMI is calculated for a Profile, whether sex is set to male, female, or not specified
- **THEN** the system SHALL compute the same BMI value and NHG category for the same weight and height
