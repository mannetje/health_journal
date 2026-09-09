## Purpose

Records GPS-tracked physical activity sessions for a Profile. Each session captures start time, end time, and total distance. Distance is stored in metres internally.

## ADDED Requirements

### Requirement: Record activity session
The system SHALL allow recording a physical activity session with a start timestamp, end timestamp, and total distance in metres, associated with a Profile.

#### Scenario: Valid session recorded
- **WHEN** a client provides a Profile identifier, a start timestamp, an end timestamp after the start, and a distance between 0 and 1,000,000 metres
- **THEN** the system SHALL persist the session and return its identifier

#### Scenario: End before start rejected
- **WHEN** the end timestamp is at or before the start timestamp
- **THEN** the system SHALL reject the session with a validation error

#### Scenario: Negative distance rejected
- **WHEN** the distance value is negative
- **THEN** the system SHALL reject the session with a validation error

### Requirement: Calculate session duration
The system SHALL derive the session duration in seconds from the start and end timestamps.

#### Scenario: Duration computed correctly
- **WHEN** a session has a valid start and end timestamp
- **THEN** the system SHALL expose the duration as the difference in whole seconds between end and start

### Requirement: Retrieve activity history
The system SHALL allow retrieving all activity sessions for a Profile in reverse chronological order.

#### Scenario: History returned for known profile
- **WHEN** a client requests activity history for a Profile with sessions
- **THEN** the system SHALL return all sessions ordered from most recent to oldest, each including start time, end time, distance, and duration
