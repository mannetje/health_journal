## ADDED Requirements

### Requirement: Migraine entry
The system SHALL store a migraine entry for a profile with: a start time; an optional end time that is not before the start time; an optional intensity from 0 to 10; zero or more head areas; an optional type (with aura, without aura, other headache, not sure); zero or more symptoms; zero or more triggers; a took-something flag; an optional free text of what was taken (at most 100 characters); a took-a-second-dose flag; an optional daily-activities choice (could carry on, had to stop); and the optional comment. Duration and weekday SHALL be derived from the times and SHALL NOT be stored. The type is only the label the person picked; the system SHALL NOT choose, suggest or change it.

#### Scenario: Minimal entry
- **WHEN** a migraine is saved with only a start time
- **THEN** it is stored with no end time, no intensity and nothing else, and it appears in History

#### Scenario: End before start
- **WHEN** an end time before the start time is entered
- **THEN** saving is refused with a message in the app language and nothing is stored

#### Scenario: Intensity out of range
- **WHEN** an intensity of 11 is entered
- **THEN** it is refused; 0 and 10 are accepted

#### Scenario: Derived duration
- **WHEN** a migraine runs from 22:00 to 03:30 the next day
- **THEN** its duration is 5 hours 30 minutes and its weekday is that of the start

### Requirement: Ongoing migraine
A migraine without an end time SHALL be treated as ongoing. The History list SHALL show an "it ended" action for it that sets the end time (default now, editable, never before the start). An ongoing migraine SHALL count in the number of migraines and in days with migraine, and SHALL be left out of duration figures.

#### Scenario: End an ongoing migraine
- **WHEN** a migraine started at 08:00 is ended at 13:00
- **THEN** the same entry now has an end time of 13:00 and a duration of 5 hours

#### Scenario: Ongoing in the statistics
- **WHEN** the range holds one finished and one ongoing migraine
- **THEN** the number of migraines is 2 and the duration chart shows one migraine

### Requirement: Migraine card on the Log tab
The Log tab SHALL show a Migraine card after the existing metric cards, with a "Log a migraine" button (Dutch: "Migraine registreren") and the latest entry as plain text (date, intensity if given). The text SHALL NOT use the word "attack" in either language.

#### Scenario: Card with no entries
- **WHEN** the profile has no migraines
- **THEN** the card shows the button and no latest entry

#### Scenario: Latest entry
- **WHEN** the latest migraine started on 12 March with intensity 6
- **THEN** the card names that date and intensity 6 as plain text

### Requirement: Step-by-step logging
The button SHALL open a short step-by-step screen with these steps in order: start and end time; intensity; head area; type; symptoms; triggers; medication taken; daily activities; comment. The start time SHALL default to now. Only the start time is required: every later step SHALL be skippable and a Save action SHALL be available on every step. Back SHALL return one step and, from the first step, leave the screen without saving. Leaving after any input SHALL ask once whether to discard it.

#### Scenario: Quick save
- **WHEN** a person opens the screen and saves on the first step
- **THEN** a migraine with the start time as chosen is stored

#### Scenario: Skipping
- **WHEN** a person skips every step after the first and saves
- **THEN** only the start time is stored

#### Scenario: Leaving with input
- **WHEN** an intensity was chosen and Back is pressed on the first step
- **THEN** a confirmation asks whether to discard, and choosing keep returns to the step

### Requirement: Head areas, symptoms and triggers
Head area SHALL be a plain list: whole head, left side, right side, forehead, behind the eyes, temples, back of the head and neck, other. Symptoms SHALL be a checklist: nausea, aura, sensitive to light, sensitive to sound, sensitive to smell, dizziness, vertigo, movement symptoms such as numbness, other. Triggers SHALL be a checklist the person fills in only from their own notion: sleep, stress, food, weather, other. The system SHALL NOT pre-select, suggest or rank any of them, and SHALL NOT present a trigger as a cause.

#### Scenario: Several symptoms
- **WHEN** nausea and sensitivity to light are ticked
- **THEN** both are stored and shown on the entry in History

#### Scenario: Nothing pre-selected
- **WHEN** the symptoms step opens
- **THEN** no box is ticked

### Requirement: Medication taken is free text
The medication step SHALL offer a took-something tick, a free text field (at most 100 characters) shown only when the tick is on, and a took-a-second-dose tick. The text field SHALL offer no suggestions or auto-complete, and the app SHALL contain no medicine names. This step SHALL NOT be linked to the pillbox, SHALL NOT create or change medication data and SHALL NOT raise any reminder.

#### Scenario: Free text only
- **WHEN** the person types "my usual one" and ticks a second dose
- **THEN** exactly that text and the flag are stored

#### Scenario: Pillbox untouched
- **WHEN** a migraine with something taken is saved
- **THEN** no medication, schedule or intake log is created or changed

### Requirement: Daily activities choice
The daily-activities step SHALL offer two choices, "could carry on" and "had to stop", of which at most one is chosen, and SHALL be skippable. The choice SHALL be shown on the entry in History as plain text and SHALL NOT be counted, scored or interpreted anywhere.

#### Scenario: Choice stored
- **WHEN** "had to stop" is chosen
- **THEN** the entry stores it and History shows it as text

#### Scenario: Not counted
- **WHEN** the Migraine view is shown for a range with entries that have this choice
- **THEN** no tile, chart or caption uses it

### Requirement: Migraine in History
History SHALL list migraines with the other entry types in the merged timeline, newest first by start time, each row showing start, duration or "ongoing", intensity if given, and the comment if any. Tapping a row SHALL open an edit screen with the same steps and values; deleting SHALL follow the existing delete confirmation. The History metric filter SHALL include Migraine.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a migraine starting at 11:00 exist
- **THEN** the migraine is listed first

#### Scenario: Edit
- **WHEN** a migraine's intensity is changed from 6 to 4 and saved
- **THEN** the same entry now has intensity 4 and its other values are unchanged

#### Scenario: Delete
- **WHEN** a migraine is deleted and confirmed
- **THEN** it is gone from History, the graphs and the calendar

### Requirement: Migraine view in the graph section
The History graph section SHALL offer a Migraine view with a Graph and Calendar toggle and the range chips week, month, 3 months, 6 months and year (the same chips and range rules as other trends). All figures SHALL be computed by pure functions in the domain layer for the active profile and range. The Graph view SHALL show, in this order: summary tiles; a timeline chart; a duration chart; an intensity chart; a weekday chart; a weekday-by-time-of-day heat map; symptom counts; trigger counts. An empty range SHALL show a plain "No migraines in this period" text and no charts.

#### Scenario: Summary tiles
- **WHEN** the range holds 4 migraines on 3 different days, something taken on 2 of those days, and the range is 30 days
- **THEN** the tiles read 4 migraines, 3 days with migraine, 2 days with something taken and 27 migraine-free days

#### Scenario: Migraine across midnight
- **WHEN** a migraine starts at 23:00 and ends at 04:00 the next day
- **THEN** it counts once as a migraine, and as days with migraine only for its start day

#### Scenario: Empty range
- **WHEN** the range holds no migraines
- **THEN** the view shows the empty text and no charts

### Requirement: Timeline and duration charts
The timeline chart SHALL draw one bar per migraine on its start date, its height the intensity 0 to 10 (a migraine without intensity is drawn as a marker at the base, not as a value), with other-headache migraines in a second tone of the same hue and a legend. The duration chart SHALL count finished migraines in the hour ranges under 4, 4 to 12 and over 12 as plain bars with numbers; ongoing ones SHALL be counted in a note ("1 still ongoing") and not charted.

#### Scenario: Duration ranges
- **WHEN** durations of 2, 4, 12 and 13 hours exist
- **THEN** under 4 shows 1, 4 to 12 shows 2 and over 12 shows 1

#### Scenario: Missing intensity
- **WHEN** a migraine has no intensity
- **THEN** it appears in the timeline as a base marker and is left out of the intensity chart, with a note giving how many were left out

### Requirement: Intensity, weekday and time-of-day figures
The intensity chart SHALL count migraines in the number ranges 0 to 3, 4 to 6 and 7 to 10 and SHALL label them only with those numbers, never with words such as mild, moderate or severe. The weekday chart SHALL count migraines by start weekday starting on the profile locale's first day of the week. The heat map SHALL count migraines by start weekday and time of day (night 00:00 to 05:59, morning 06:00 to 11:59, afternoon 12:00 to 17:59, evening 18:00 to 23:59), with cell shade by count in a single hue and the count written in each non-empty cell. A caption SHALL state facts only, such as "4 migraines started on Monday".

#### Scenario: Range boundaries
- **WHEN** intensities 3, 4, 6 and 7 exist
- **THEN** 0 to 3 shows 1, 4 to 6 shows 2 and 7 to 10 shows 1

#### Scenario: Time-of-day boundary
- **WHEN** migraines start at 05:59 and 06:00
- **THEN** the first counts as night and the second as morning

#### Scenario: Factual caption
- **WHEN** Monday holds the highest weekday count of 4
- **THEN** the caption reads "4 migraines started on Monday" and does not say "mostly", give a percentage or suggest a reason

### Requirement: Migraine calendar
The Calendar toggle SHALL show a month grid, starting on the locale's first day of the week, with days that hold at least one migraine marked in a single hue and a count when there are two or more. Previous and next buttons SHALL move by month and not past the current month. Tapping a marked day SHALL open that day's migraines in History. A day SHALL be marked by the start time of the migraine; a day with something taken SHALL carry a second plain marker.

#### Scenario: Open a day
- **WHEN** a marked day with two migraines is tapped
- **THEN** History opens filtered to Migraine and that day, showing both

#### Scenario: Future months
- **WHEN** the current month is shown
- **THEN** the next button is disabled

### Requirement: Symptom and trigger counts
Two lists SHALL show how many migraines in the range had each symptom and each trigger, as a plain number per item, sorted by the count and then by the fixed order of the list. Items with a count of 0 SHALL be left out. The lists SHALL NOT interpret the counts.

#### Scenario: Counts
- **WHEN** nausea is on 3 migraines and sensitivity to light on 5
- **THEN** light is listed first with 5 and nausea second with 3

### Requirement: Neutral presentation
The Migraine log SHALL use one hue in light and dark theme, SHALL NOT use red, amber or green or any colour to rank intensity or counts, SHALL NOT use the word "attack" or "aanval" in text, descriptions or documents in either language, and SHALL contain no advice, warnings, predictions, "mostly" statements, percentage headlines or suggested causes. Every chart SHALL have a text description for screen readers giving the same numbers.

#### Scenario: Screen reader
- **WHEN** a screen reader focuses the weekday chart
- **THEN** it reads each weekday with its count

#### Scenario: Wording check
- **WHEN** the string resources are scanned in English and Dutch
- **THEN** none contains "attack" or "aanval"

### Requirement: Migraine text in English and Dutch
Every Migraine string SHALL exist in English and Dutch. Dutch SHALL use "migraine" and "migraine registreren", and dates, times and weekdays SHALL follow the app language.

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card button reads "Migraine registreren" and the tiles read "dagen met migraine" and "migraine-vrije dagen"
