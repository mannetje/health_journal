## Purpose

Represents a person using the application. A Profile holds identifying information (name, date-of-birth, optional height) and serves as the Aggregate Root that owns all health log entries. Every measurement MUST be associated with exactly one Profile.

## ADDED Requirements

### Requirement: Create profile
The system SHALL allow creating a new Profile with a full name, date-of-birth, and optional height in centimetres.

#### Scenario: Valid profile creation
- **WHEN** a client provides a non-empty name, a valid date-of-birth (not in the future, not more than 130 years ago), and an optional height between 50 and 300 cm
- **THEN** the system SHALL persist the Profile and return its generated identifier

#### Scenario: Missing name rejected
- **WHEN** a client provides a blank or empty name
- **THEN** the system SHALL reject the request with a validation error

#### Scenario: Future date-of-birth rejected
- **WHEN** a client provides a date-of-birth that is today or in the future
- **THEN** the system SHALL reject the request with a validation error

### Requirement: Read profile
The system SHALL allow retrieving a previously created Profile by its identifier.

#### Scenario: Existing profile returned
- **WHEN** a client requests a Profile by a known identifier
- **THEN** the system SHALL return the Profile including name, date-of-birth, and height (if set)

#### Scenario: Unknown identifier
- **WHEN** a client requests a Profile by an identifier that does not exist
- **THEN** the system SHALL return a not-found result (no error thrown)
