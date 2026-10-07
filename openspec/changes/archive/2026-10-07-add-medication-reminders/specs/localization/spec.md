## ADDED Requirements

### Requirement: Notifications use the app language
Notification titles, bodies and action labels for medication reminders SHALL be in the in-app language, not the device language.

#### Scenario: English device, Dutch app
- **WHEN** a reminder is posted while the app language is Dutch and the device language is English
- **THEN** the notification title, body, and the Taken all and Snooze actions are in Dutch

#### Scenario: Both languages complete
- **WHEN** the project is tested
- **THEN** a test SHALL fail if a reminder string key exists in one language and not in the other
