## MODIFIED Requirements

### Requirement: Local storage with Room
The app SHALL persist data offline in a SQLite database through Room, with the database at schema version 5. Each step SHALL be a real migration that keeps all rows: 1 to 2 adds the optional sex column to the profile table, 2 to 3 adds the waist circumference table, 3 to 4 adds the optional pulse column to blood pressure, and 4 to 5 adds the optional comment column to the weight, blood pressure, glucose, waist circumference and activity tables. Storage SHALL always be metric.

#### Scenario: Upgrade keeps data
- **WHEN** a device with a version 1, 2, 3 or 4 database installs a build that has version 5
- **THEN** all profiles and entries SHALL still be present, the sex of each profile, the pulse of each earlier blood pressure and the comment of every earlier entry SHALL be unset, and the waist circumference history SHALL be empty when upgrading from version 1 or 2 (ADR 0004)
