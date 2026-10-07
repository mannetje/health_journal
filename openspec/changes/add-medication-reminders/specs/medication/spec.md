## ADDED Requirements

### Requirement: Grouped reminders with actions
The system SHALL post one local notification per slot (all planned intakes of the active profile at the same local time) at that time, with Taken all and Snooze actions, and SHALL keep reminders working after restart, app update, time change and schedule edits. A reminder SHALL depend only on the user-defined schedule, never on a measured value.

#### Scenario: Several medications, one notification
- **WHEN** three medications are planned at 07:30
- **THEN** the system SHALL post one notification at 07:30 for all three, not three

#### Scenario: Taken all from the notification
- **WHEN** the user taps Taken all on a reminder
- **THEN** the system SHALL record every still-pending intake of the slot as taken without opening the app and SHALL dismiss the notification

#### Scenario: Existing outcomes are not overwritten
- **WHEN** one intake of the slot was already marked skipped and the user taps Taken all
- **THEN** that intake SHALL stay skipped and the others SHALL be recorded as taken

#### Scenario: Slot rebuilt when it fires
- **WHEN** a medication of the slot was archived or marked taken after the alarm was set
- **THEN** the notification SHALL list only the intakes that are still pending, and no notification SHALL be posted when none is pending

#### Scenario: Open the slot
- **WHEN** the user taps the notification
- **THEN** the app opens the pillbox on Today at that slot, where each intake can be marked Taken or Skipped separately

#### Scenario: Snooze
- **WHEN** the user taps Snooze and the snooze length in Profile is 30 minutes
- **THEN** the system SHALL show the reminder again after 30 minutes without opening the app and SHALL keep the original planned time

#### Scenario: Snooze length is a setting
- **WHEN** the user opens the reminder settings
- **THEN** the user can choose 10, 30 or 60 minutes as the snooze length, with 10 minutes as the default

#### Scenario: Notification follows the pillbox
- **WHEN** the user records an outcome for every medication of a slot in the pillbox while its notification is shown
- **THEN** that notification SHALL be removed, and with outcomes for only some of them it SHALL list only the rest

#### Scenario: Reminders survive reboot
- **WHEN** the device restarts
- **THEN** the system SHALL re-arm the next reminder for the active profile

#### Scenario: Reminder follows a schedule edit
- **WHEN** the user changes a time or archives a medication
- **THEN** the next reminder SHALL be re-armed from the changed schedule

#### Scenario: Reminders belong to the active profile
- **WHEN** the user switches the active profile
- **THEN** reminders of the previous profile are cancelled and the new profile's next reminder is armed

### Requirement: Reminder permission flow
The system SHALL ask for notification permission only when the user first saves a schedule, with a short explanation of why before the system prompt, and SHALL keep the pillbox fully usable if it is denied.

#### Scenario: First schedule
- **WHEN** the user saves their first scheduled medication and permission has not been decided
- **THEN** the system explains why reminders need permission and then shows the system prompt

#### Scenario: Permission denied
- **WHEN** notification permission is denied
- **THEN** the pillbox SHALL still work and the system SHALL show a banner explaining that reminders are off

#### Scenario: Permission later granted in settings
- **WHEN** the user grants permission in the system settings and returns to the app
- **THEN** the banner disappears and reminders are armed

### Requirement: Reminder text is neutral
A reminder SHALL contain only the planned time and the user's own medication names and doses, or the generic text "Medication reminder" when details are hidden, with no instruction, advice, warning or encouragement wording.

#### Scenario: Neutral wording
- **WHEN** a reminder is shown with details
- **THEN** it lists the planned time and each medication name with its dose, and contains no wording such as "take now" or "do not miss"
