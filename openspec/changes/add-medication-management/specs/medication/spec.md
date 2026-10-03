## ADDED Requirements

### Requirement: Manage medications
The system SHALL allow creating, editing, archiving and deleting medications for a Profile. A medication SHALL have a name of 1 to 80 characters and a dose per intake greater than 0 and at most 1000 in a chosen unit, and MAY have a strength, a form, a note and an appearance.

#### Scenario: Medication created with custom dosage
- **WHEN** the user saves a medication named "Medication A" with strength 500 mg and a dose of 1 tablet
- **THEN** the system SHALL persist it and show it in the medication list

#### Scenario: Liquid, spray and injection forms
- **WHEN** the user saves a medication with form liquid and dose 5 ml, or form spray and dose 2 puffs, or form injection and dose 20 units
- **THEN** the system SHALL persist each with its own dose unit and show the dose in that unit, and SHALL NOT convert between units

#### Scenario: Weekly or multi-day interval
- **WHEN** the user schedules a medication every 7 days or every 3 days
- **THEN** the pillbox SHALL show a planned intake on those days only

#### Scenario: Actual amount differs from the planned dose
- **WHEN** the user marks an intake as taken and enters an amount different from the planned dose
- **THEN** the system SHALL store the actual amount with the intake, SHALL NOT comment on it, and SHALL count the intake as taken for adherence

#### Scenario: Doses are never calculated
- **WHEN** the user logs or schedules any medication
- **THEN** the system SHALL NOT calculate, suggest, round or adjust a dose, and SHALL NOT use glucose, blood pressure, weight or food entries to derive one

#### Scenario: Invalid input rejected
- **WHEN** the name is blank or longer than 80 characters, or the dose is 0 or negative
- **THEN** the system SHALL reject the save and show a localized validation message

#### Scenario: Archiving keeps history
- **WHEN** the user archives a medication
- **THEN** the system SHALL stop generating planned intakes and reminders for it, and SHALL keep its intake log and adherence history

#### Scenario: Deleting requires confirmation
- **WHEN** the user deletes a medication
- **THEN** the system SHALL ask for confirmation, and on confirmation SHALL remove the medication, its schedule and its intake log

### Requirement: Schedule intakes
The system SHALL support recurring schedules (one to eight times a day, on selected weekdays or every N days, with a start date and an optional end date) and as-needed medications with no schedule.

#### Scenario: Planned intakes derived from the schedule
- **WHEN** a medication is scheduled at 08:00 and 20:00 every day
- **THEN** the pillbox for any date within the schedule SHALL show two planned intakes, one at each time

#### Scenario: Editing a schedule does not change history
- **WHEN** the user changes a schedule from 08:00 to 09:00
- **THEN** past recorded intakes SHALL remain unchanged and only dates from the edit onward SHALL use the new time

#### Scenario: Planned times are wall-clock
- **WHEN** the device time zone changes
- **THEN** planned times SHALL remain at the same local clock time

### Requirement: Record intake outcomes
The system SHALL let the user mark a planned intake as Taken (with the actual time) or Skipped, or leave it Pending, and SHALL let the user log an as-needed dose with a timestamp.

#### Scenario: Marked taken
- **WHEN** the user marks the 08:00 intake as taken at 08:12
- **THEN** the system SHALL store status TAKEN and the actual time, and the pillbox SHALL show it as taken

#### Scenario: Pending past the grace period becomes missed
- **WHEN** a planned intake has no outcome and the 2-hour grace period has passed
- **THEN** the system SHALL show it as missed and count it as due and not taken

#### Scenario: Correcting a past intake
- **WHEN** the user changes the outcome of a past intake
- **THEN** the system SHALL update it and recalculate adherence

### Requirement: Pillbox view
The system SHALL show a day view of all planned intakes grouped by morning, afternoon, evening and night, and a week strip marking each day complete, partial, missed or empty.

#### Scenario: Day grouped by time of day
- **WHEN** intakes are planned at 08:00, 13:00 and 21:00
- **THEN** they SHALL appear under morning, afternoon and evening respectively

#### Scenario: Appearance shown with name and dose
- **WHEN** a medication has an appearance
- **THEN** the pillbox, notifications and history SHALL show its colour and shape icon together with the name and dose, so colour is never the only identifier

### Requirement: Reminders with actions
The system SHALL post a local notification at each planned time with Taken and Snooze actions, and SHALL keep reminders working after restart, app update, time change and schedule edits.

#### Scenario: Mark taken from the notification
- **WHEN** the user taps Taken on a reminder
- **THEN** the system SHALL record the intake as taken without opening the app and SHALL dismiss the notification

#### Scenario: Snooze
- **WHEN** the user taps Snooze and picks 30 minutes
- **THEN** the system SHALL show the reminder again after 30 minutes and SHALL keep the original planned time for adherence

#### Scenario: Reminders survive reboot
- **WHEN** the device restarts
- **THEN** the system SHALL re-arm the next reminder for every active medication

#### Scenario: Notification permission denied
- **WHEN** notification permission is denied
- **THEN** the pillbox SHALL still work and the system SHALL show a banner explaining that reminders are off

#### Scenario: Lock screen details hidden by default
- **WHEN** a reminder is shown on a locked device with the default setting
- **THEN** it SHALL show only "Medication reminder" and not the medication name or dose

### Requirement: Adherence tracking
The system SHALL compute per-medication and overall adherence for the last 7, 30 and 90 days as taken divided by due, rounded to a whole percent (half up), and the current streak of days with all intakes taken.

#### Scenario: Adherence calculated
- **WHEN** 28 intakes were due in the last 30 days and 26 were taken
- **THEN** adherence SHALL be 93%

#### Scenario: Skipped shown separately
- **WHEN** an intake was skipped
- **THEN** it SHALL count as due and not taken, and the report SHALL show skipped and missed counts separately

#### Scenario: As-needed medication
- **WHEN** a medication is as-needed
- **THEN** the report SHALL show the number of doses and no percentage

#### Scenario: Not due before the start date
- **WHEN** a medication starts on 2026-10-10
- **THEN** earlier dates SHALL NOT count as due

### Requirement: Not medical advice
The system SHALL NOT provide dosing advice, interaction checks or drug information, and SHALL state in the app that it is a personal log and not a medical device. Its stated intended purpose SHALL be logging and reminding only, and it SHALL NOT claim to diagnose, predict, or support treatment decisions.

#### Scenario: Missed dose shows a status only
- **WHEN** an intake is shown as missed
- **THEN** the system SHALL show only the status "Missed" and the planned time, and SHALL NOT say or imply what to do (not to take it, not to skip it, not to double it, not to contact anyone)

#### Scenario: No advice about medication anywhere
- **WHEN** the app shows a medication, dose, schedule, reminder, notification or adherence figure
- **THEN** it SHALL NOT include advice, instructions, warnings, tips, recommendations or encouragement about taking, stopping, changing or timing medication, and every word shown about a medication SHALL come from the user's own input or be a neutral status

#### Scenario: Information links are neutral references
- **WHEN** the app offers further information
- **THEN** it SHALL offer a single neutral "Sources" list on the information screen with links to apotheek.nl and Thuisarts (NHG), without paraphrasing or summarizing their content and without attaching a recommendation

#### Scenario: Disclaimer visible
- **WHEN** the user opens the Medication tab for the first time
- **THEN** the system SHALL show the disclaimer and make it available again from settings

### Requirement: Medication tab placement
The system SHALL provide a fourth bottom tab named Medication (Dutch: Medicatie) with a pill icon, placed second after Log. It SHALL contain the sub-views Today (pillbox), Medications and Adherence. Medication settings and the notice SHALL be in Profile, and tapping a reminder notification SHALL open Today.

#### Scenario: Tab order
- **WHEN** the app is opened
- **THEN** the bottom bar shows Log, Medication, History, Profile in that order

#### Scenario: Notification opens Today
- **WHEN** the user taps a reminder notification
- **THEN** the app opens the Medication tab on Today

### Requirement: Medication data belongs to a profile
Every medication, schedule and intake SHALL belong to exactly one Profile. The Medication tab SHALL show only the active profile's data, and reminders SHALL be armed for the active profile only.

#### Scenario: Switching profile
- **WHEN** the user switches the active profile
- **THEN** the pillbox, medication list and adherence show the new profile's data and reminders of the previous profile are cancelled

#### Scenario: Deleting a profile
- **WHEN** the user deletes a profile after confirmation
- **THEN** its medications, schedules and intakes are removed with it, and the confirmation names that they will be removed

### Requirement: Accessibility
Medication screens SHALL be operable with the platform's screen reader and large text. Icons and status chips SHALL have text descriptions that state the medication, dose, planned time and status. Controls SHALL have a touch target of at least 48 dp (or the platform equivalent), and icon colours SHALL keep sufficient contrast in light and dark themes.

#### Scenario: Screen reader announces an intake
- **WHEN** a screen reader focuses a pending intake
- **THEN** it reads the medication name, dose, planned time and status, and offers Taken and Skip as actions

### Requirement: Reminder permission flow
The system SHALL ask for notification permission only when the user first saves a schedule, with a short explanation of why before the system prompt, and SHALL keep the pillbox fully usable if it is denied.

#### Scenario: First schedule
- **WHEN** the user saves their first scheduled medication and permission has not been decided
- **THEN** the system explains why reminders need permission and then shows the system prompt

#### Scenario: Permission later granted in settings
- **WHEN** the user grants permission in the system settings and returns to the app
- **THEN** the banner disappears and reminders are armed
