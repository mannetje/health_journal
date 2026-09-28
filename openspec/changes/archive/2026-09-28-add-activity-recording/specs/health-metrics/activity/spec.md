# Spec Delta

## MODIFIED Requirements

### Requirement: Record activity session
The system SHALL allow recording a physical activity session with a start timestamp, end timestamp, and total distance in metres, associated with a Profile, both through bulk data import and through manual entry in the application's own UI.

#### Scenario: Valid session recorded
- **WHEN** a client provides a Profile identifier, a start timestamp, an end timestamp after the start, and a distance between 0 and 1,000,000 metres
- **THEN** the system SHALL persist the session and return its identifier

#### Scenario: End before start rejected
- **WHEN** the end timestamp is at or before the start timestamp
- **THEN** the system SHALL reject the session with a validation error

#### Scenario: Negative distance rejected
- **WHEN** the distance value is negative
- **THEN** the system SHALL reject the session with a validation error

#### Scenario: Manual session entry via app
- **WHEN** a user with an active Profile enters a duration and a distance for an activity in the logging screen and confirms
- **THEN** the system SHALL derive a start timestamp of "now minus duration" and an end timestamp of "now", persist the session for the active Profile, and confirm the entry was recorded
