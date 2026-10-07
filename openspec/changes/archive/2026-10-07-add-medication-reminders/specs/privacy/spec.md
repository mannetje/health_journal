## ADDED Requirements

### Requirement: Reminder details hidden on a locked device
The system SHALL hide medication names and doses in notifications shown on a locked device by default, and SHALL offer a setting to show them.

#### Scenario: Lock screen details hidden by default
- **WHEN** a reminder is shown on a locked device with the default setting
- **THEN** it SHALL show only "Medication reminder" and not the medication names or doses

#### Scenario: Details shown by choice
- **WHEN** the user turns on showing details on the lock screen
- **THEN** reminders on a locked device list the medication names and doses
