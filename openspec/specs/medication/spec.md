# medication Specification

## Purpose
Describes how a person records the medications they take for a profile, plans when to take them, logs what happened at each planned time, and sees the day in a pillbox. It records and shows; it never advises.

## Requirements

### Requirement: Manage medications
The system SHALL allow creating, editing, archiving and deleting medications for a Profile. A medication SHALL have a name of 1 to 80 characters and a dose per intake greater than 0 and at most 1000 in a chosen dose unit, and MAY have a strength, a form, a comment and an appearance. The comment SHALL follow the rules of an entry comment (single line, at most 200 characters, blank means none).

#### Scenario: Medication created with custom dosage
- **WHEN** the user saves a medication named "Medication A" with strength 500 mg and a dose of 1 tablet
- **THEN** the system SHALL persist it and show it in the medication list

#### Scenario: Liquid, spray and injection forms
- **WHEN** the user saves a medication with form liquid and dose 5 ml, or form spray and dose 2 puffs, or form injection and dose 20 units
- **THEN** the system SHALL persist each with its own dose unit and show the dose in that unit, and SHALL NOT convert between units

#### Scenario: Strength has its own units
- **WHEN** the user enters a strength
- **THEN** the unit SHALL be chosen from mg, microgram, g, international units, or one of those per ml, and SHALL be independent of the dose unit

#### Scenario: Actual amount differs from the planned dose
- **WHEN** the user marks an intake as taken and enters an amount different from the planned dose
- **THEN** the system SHALL store the actual amount with the intake and SHALL NOT comment on it

#### Scenario: Doses are never calculated
- **WHEN** the user logs or schedules any medication
- **THEN** the system SHALL NOT calculate, suggest, round or adjust a dose, and SHALL NOT use glucose, blood pressure, weight or food entries to derive one

#### Scenario: Invalid input rejected
- **WHEN** the name is blank or longer than 80 characters, or the dose is 0 or negative
- **THEN** the system SHALL reject the save and show a localized validation message

#### Scenario: Comment follows the entry comment rules
- **WHEN** the user enters a comment of 201 characters or with line breaks
- **THEN** the system SHALL limit it to 200 characters and replace line breaks, as it does for entry comments

#### Scenario: Archiving keeps history
- **WHEN** the user archives a medication
- **THEN** the system SHALL stop planning intakes for it from that day on, and SHALL keep its intake log

#### Scenario: Deleting requires confirmation
- **WHEN** the user deletes a medication
- **THEN** the system SHALL ask for confirmation, and on confirmation SHALL remove the medication, its schedule versions and its intake log

### Requirement: Schedule intakes
The system SHALL support recurring schedules (one to eight times a day, on selected weekdays or every N days, with a start date and an optional end date) and as-needed medications with no schedule. A schedule SHALL be kept as versions, each with the date from which it applies, so the planned intakes of a date come from the version in force on that date.

#### Scenario: Planned intakes derived from the schedule
- **WHEN** a medication is scheduled at 08:00 and 20:00 every day
- **THEN** the pillbox for any date within the schedule SHALL show two planned intakes, one at each time

#### Scenario: Weekly or multi-day interval
- **WHEN** the user schedules a medication every 7 days or every 3 days
- **THEN** the pillbox SHALL show a planned intake on those days only

#### Scenario: Editing a schedule applies from a date
- **WHEN** the user changes a schedule from 08:00 to 09:00 and chooses to apply it from today
- **THEN** dates before today SHALL keep 08:00 and dates from today SHALL use 09:00, and past recorded intakes SHALL remain unchanged

#### Scenario: Default date for an edit
- **WHEN** the user edits a schedule and does not choose a date
- **THEN** the change SHALL apply from today

#### Scenario: Recorded intake no longer planned
- **WHEN** an intake outcome exists for a planned time that the schedule version in force no longer produces
- **THEN** the system SHALL keep and show the outcome in the day view, SHALL NOT delete it, and SHALL NOT count it as a planned intake

#### Scenario: Planned times are wall-clock
- **WHEN** the device time zone changes
- **THEN** planned times SHALL remain at the same local clock time

### Requirement: Record intake outcomes
The system SHALL let the user mark a planned intake as Taken (with the actual time) or Skipped, or leave it Pending, and SHALL let the user log an as-needed dose with a timestamp. An intake MAY carry an actual amount and a comment that follows the entry comment rules.

#### Scenario: Marked taken
- **WHEN** the user marks the 08:00 intake as taken at 08:12
- **THEN** the system SHALL store status TAKEN and the actual time, and the pillbox SHALL show it as taken

#### Scenario: Pending past the grace period becomes missed
- **WHEN** a planned intake has no outcome and the 2-hour grace period has passed
- **THEN** the system SHALL show it as missed

#### Scenario: Several as-needed doses
- **WHEN** the user logs two as-needed doses of the same medication on the same day
- **THEN** the system SHALL store both, each with its own timestamp, and show both

#### Scenario: Correcting a past intake
- **WHEN** the user changes the outcome of a past intake
- **THEN** the system SHALL update it and show the new outcome

### Requirement: Pillbox view
The system SHALL show a day view of all planned intakes grouped by morning, afternoon, evening and night, with intakes at the same time shown together as one slot, and a week strip marking each day complete, partial, missed or empty. The user SHALL be able to mark a whole slot as taken in one action and still change each intake in it separately.

#### Scenario: Day grouped by time of day
- **WHEN** intakes are planned at 08:00, 13:00 and 21:00
- **THEN** they SHALL appear under morning, afternoon and evening respectively

#### Scenario: Medications at the same time share a slot
- **WHEN** three medications are planned at 07:30
- **THEN** the pillbox SHALL show one 07:30 slot listing all three, with an action to mark all as taken

#### Scenario: One medication in a slot handled separately
- **WHEN** the user marks one of the three medications in the 07:30 slot as skipped and the other two as taken
- **THEN** each intake SHALL keep its own outcome

#### Scenario: Appearance shown with name and dose
- **WHEN** a medication has an appearance
- **THEN** the pillbox and the medication list SHALL show its colour and shape icon together with the name and dose, so colour is never the only identifier

### Requirement: Pillbox is a separate screen
The system SHALL provide the pillbox as its own screen reached through a pill button in the top bar that is visible on every main tab, SHALL keep the bottom bar unchanged, and SHALL return to the tab the user came from. Within the pillbox screen the user SHALL reach the day view (Today) and the Medications list, and further views MAY be added there without adding bottom tabs.

#### Scenario: Open the pillbox
- **WHEN** the user taps the pill button on any main tab
- **THEN** the pillbox screen opens on Today and the bottom bar still shows only the existing tabs

#### Scenario: Leave the pillbox
- **WHEN** the user goes back from the pillbox screen
- **THEN** the app shows the tab that was open before

#### Scenario: Reach the medication list
- **WHEN** the user opens the Medications view within the pillbox screen
- **THEN** the list with its add button is shown, and an added medication appears in Today when it is planned for that day

### Requirement: Not medical advice
The system SHALL NOT provide dosing advice, interaction checks or drug information, and SHALL state in the app that it is a personal log and not a medical device. Its stated intended purpose SHALL be logging only, and it SHALL NOT claim to diagnose, predict, or support treatment decisions.

#### Scenario: Missed dose shows a status only
- **WHEN** an intake is shown as missed
- **THEN** the system SHALL show only the status "Missed" and the planned time, and SHALL NOT say or imply what to do (not to take it, not to skip it, not to double it, not to contact anyone)

#### Scenario: No advice about medication anywhere
- **WHEN** the app shows a medication, dose, schedule or intake status
- **THEN** it SHALL NOT include advice, instructions, warnings, tips, recommendations or encouragement about taking, stopping, changing or timing medication, and every word shown about a medication SHALL come from the user's own input or be a neutral status

#### Scenario: Information links are neutral references
- **WHEN** the app offers further information
- **THEN** it SHALL offer a single neutral "Sources" list on the information screen with links to apotheek.nl and Thuisarts (NHG), without paraphrasing or summarizing their content and without attaching a recommendation

#### Scenario: Disclaimer visible
- **WHEN** the user opens the pillbox screen for the first time
- **THEN** the system SHALL show the disclaimer and make it available again from the profile settings

### Requirement: Medication data belongs to a profile
Every medication, schedule version and intake SHALL belong to exactly one Profile. The pillbox screen SHALL show only the active profile's data.

#### Scenario: Switching profile
- **WHEN** the user switches the active profile
- **THEN** the pillbox and the medication list show the new profile's data

#### Scenario: Deleting a profile
- **WHEN** the user deletes a profile after confirmation
- **THEN** its medications, schedule versions and intakes are removed with it, and the confirmation names that they will be removed

### Requirement: Accessibility
Medication screens SHALL be operable with the platform's screen reader and large text. Icons and status chips SHALL have text descriptions that state the medication, dose, planned time and status. Controls SHALL have a touch target of at least 48 dp (or the platform equivalent), and icon colours SHALL keep sufficient contrast in light and dark themes.

#### Scenario: Screen reader announces an intake
- **WHEN** a screen reader focuses a pending intake
- **THEN** it reads the medication name, dose, planned time and status, and offers Taken and Skip as actions

#### Scenario: Pill button has a description
- **WHEN** a screen reader focuses the pill button in the top bar
- **THEN** it reads a text description of it in the app language

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
- **THEN** the app opens the pillbox on Today, where each intake can be marked Taken or Skipped separately

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
- **THEN** the pillbox SHALL still work and the system SHALL show a hint in Profile explaining that reminders are off

#### Scenario: Permission later granted in settings
- **WHEN** the user grants permission in the system settings and returns to the app
- **THEN** the hint disappears and reminders are armed

### Requirement: Reminder text is neutral
A reminder SHALL contain only the planned time and the user's own medication names and doses, or the generic text "Medication reminder" when details are hidden, with no instruction, advice, warning or encouragement wording.

#### Scenario: Neutral wording
- **WHEN** a reminder is shown with details
- **THEN** it lists the planned time and each medication name with its dose, and contains no wording such as "take now" or "do not miss"

### Requirement: Adherence tracking
The system SHALL compute per-medication and overall adherence for the last 7, 30 and 90 days as taken divided by due, rounded to a whole percent (half up), and the current streak of days with all planned intakes taken. Due SHALL use the schedule version in force on each date.

#### Scenario: Adherence calculated
- **WHEN** 28 intakes were due in the last 30 days and 26 were taken
- **THEN** adherence SHALL be 93%

#### Scenario: Skipped shown separately
- **WHEN** an intake was skipped
- **THEN** it SHALL count as due and not taken, and the report SHALL show skipped and missed counts separately

#### Scenario: Still inside the grace period
- **WHEN** a planned intake has no outcome and the 2-hour grace period has not passed
- **THEN** it SHALL NOT count as missed

#### Scenario: As-needed medication
- **WHEN** a medication is as-needed
- **THEN** the report SHALL show the number of doses and no percentage

#### Scenario: Not due before the start date
- **WHEN** a medication starts on 2026-10-10
- **THEN** earlier dates SHALL NOT count as due

#### Scenario: Nothing due
- **WHEN** no intake was due in the chosen range
- **THEN** the report SHALL show no percentage

#### Scenario: Schedule edited from today
- **WHEN** a schedule is changed from today
- **THEN** adherence for earlier days SHALL be unchanged

#### Scenario: Schedule corrected from an earlier date
- **WHEN** the user applies a schedule from an earlier date to correct a mistake
- **THEN** adherence for the days from that date SHALL be recalculated from the corrected schedule and recorded outcomes SHALL stay unchanged

#### Scenario: Outcome without a plan
- **WHEN** an intake outcome exists for a planned time that the schedule no longer produces
- **THEN** it SHALL NOT count as due and SHALL NOT change the percentage

#### Scenario: Correcting a past intake
- **WHEN** the user changes the outcome of a past intake
- **THEN** adherence SHALL be recalculated

### Requirement: Adherence view in the pillbox screen
The system SHALL show adherence as a view within the pillbox screen next to Today and Medications, without adding a bottom tab, with neutral wording only.

#### Scenario: Open the view
- **WHEN** the user opens the Adherence view
- **THEN** the system shows the overall and per-medication figures for the chosen range, the streak, skipped and missed counts and the list of missed intakes with medication and planned time

#### Scenario: No judgement
- **WHEN** the figures are shown
- **THEN** the system SHALL NOT label them good or bad, show targets, encouragement or advice, or use colours that imply either
