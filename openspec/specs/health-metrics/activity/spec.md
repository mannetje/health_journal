# activity Specification

## Purpose
Records physical activity sessions for a profile, entered manually in the app or imported in bulk. Each session captures start time, end time and total distance. Distance is stored in meters and duration is derived.

## Requirements

### Requirement: Record activity session
The system SHALL allow recording a session with a start timestamp, an end timestamp and a distance in meters, owned by a profile. The end SHALL be strictly after the start. Distance SHALL be within 0 to 1,000,000 meters inclusive.

#### Scenario: Valid session recorded
- **WHEN** a profile id, a start, an end after the start and a distance of 5000 m are provided
- **THEN** the session is persisted and its identifier returned

#### Scenario: End before or equal to start rejected
- **WHEN** the end is at or before the start
- **THEN** the session is rejected with a validation error and nothing is stored

#### Scenario: Distance out of range rejected
- **WHEN** the distance is negative or above 1,000,000 m
- **THEN** the session is rejected

### Requirement: Session duration
The duration in seconds SHALL be derived as the whole seconds between start and end. It is not stored independently of the timestamps.

#### Scenario: Duration computed
- **WHEN** a session runs from 08:00:00 to 08:30:00
- **THEN** its duration is 1800 seconds

### Requirement: Manual logging
In the logging screen the user enters a duration in minutes and a distance. The duration SHALL be greater than 0 and the distance SHALL be 0 or more; otherwise a localized validation error is shown and nothing is stored. The end timestamp SHALL be the current time and the start SHALL be the current time minus the duration (minutes x 60, truncated to whole seconds). Distance is entered in kilometers or miles according to the unit setting (see units-presentation) and stored in meters. Without an active profile, recording is unavailable.

#### Scenario: Manual session
- **WHEN** 30 minutes and 5 km are entered
- **THEN** a session ending now, starting 30 minutes earlier, with 5000 m is stored

### Requirement: Retrieve activity history
Sessions for a profile SHALL be returned ordered by start time, most recent first, each including start, end, distance and duration.

#### Scenario: History order
- **WHEN** sessions exist
- **THEN** they are listed newest start first

### Requirement: Editing a session
Editing (see entry-management) allows changing the duration and the distance. The start time is kept; the new end is start plus the entered duration (minutes x 60, rounded to whole seconds). The duration SHALL be greater than 0 and the distance 0 or more. Activity entries are not range-filtered by the trend range.

#### Scenario: Edit duration
- **WHEN** a session starting 08:00 is edited to 45 minutes
- **THEN** its end becomes 08:45 and its start stays 08:00

### Requirement: Persistence contract
Sessions SHALL be stored in table "activities": id, profile id, start timestamp (epoch ms), end timestamp (epoch ms), distance in meters (floating point). Ordering is by start descending.

#### Scenario: Round trip
- **WHEN** a session is stored and read back
- **THEN** start, end and distance are equal
