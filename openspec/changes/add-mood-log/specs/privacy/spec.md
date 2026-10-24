## ADDED Requirements

### Requirement: Mood data is protected like all entries
Mood entries, including steps, stress levels, feeling words and comments, SHALL be stored only in the encrypted database, SHALL NOT be written to any log, notification, preference file, crash text or the Android backup, and SHALL leave the device only through the user's own export or encrypted backup. No notification, widget or reminder SHALL show a mood value or word. Mood screens follow the secure-screen setting.

#### Scenario: No log output
- **WHEN** an entry is saved, edited or deleted in a release build
- **THEN** nothing about it appears in the device log

#### Scenario: Stored encrypted
- **WHEN** the database file is read without the key
- **THEN** the mood table content is not readable

#### Scenario: Secure screen
- **WHEN** the secure screen setting is on and the Mood card or graph is open
- **THEN** screenshots and the recent-apps preview are blocked as elsewhere

#### Scenario: No notifications
- **WHEN** an entry is saved
- **THEN** no notification or widget text is created from it
