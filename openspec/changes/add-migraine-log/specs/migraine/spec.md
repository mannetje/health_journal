## ADDED Requirements

### Requirement: Migraine entry
The system SHALL store a migraine entry for a profile with: a start time; a woke-up-with-it flag; an optional end time that is not before the start time; an optional intensity from 0 to 10; zero or more head areas; zero or more pain characters (throbbing or pounding, pressing or tight, piercing); an optional type (with aura, without aura, recognized headache, other headache, not sure); zero or more symptoms; zero or more triggers; a took-something flag; an optional free text of what was taken (at most 100 characters); a took-a-second-dose flag; an optional intensity after 2 hours from 0 to 10, allowed only when the took-something flag is set; zero or more relief methods (sleep or rest, dark and quiet room, cold pack, heat, massage, coffee or tea, other); zero or more personal triggers and personal relief methods chosen from the person's own tags; an optional daily-activities choice (could carry on, had to stop); and the optional comment. Duration and weekday SHALL be derived from the times and SHALL NOT be stored. The type is only the label the person picked; the system SHALL NOT choose, suggest or change it. The woke-up-with-it flag marks the start time as an estimate; the system SHALL store the start time as entered and SHALL NOT change it because of the flag.

#### Scenario: Minimal entry
- **WHEN** a migraine is saved with only a start time
- **THEN** it is stored with no end time, no intensity and nothing else, and it appears in History

#### Scenario: End before start
- **WHEN** an end time before the start time is entered
- **THEN** saving is refused with a message in the app language and nothing is stored

#### Scenario: Intensity out of range
- **WHEN** an intensity of 11 is entered
- **THEN** it is refused; 0 and 10 are accepted

#### Scenario: Intensity after 2 hours out of range
- **WHEN** an intensity after 2 hours of 11 is entered
- **THEN** it is refused; 0 and 10 are accepted

#### Scenario: Intensity after 2 hours without something taken
- **WHEN** an entry has an intensity after 2 hours but the took-something flag is not set
- **THEN** saving is refused and nothing is stored

#### Scenario: Woke up with migraine
- **WHEN** a migraine is saved with the woke-up-with-it switch on and a start time of 07:10
- **THEN** 07:10 is stored as entered and treated as an estimate, and History shows "Started during sleep" on its row

#### Scenario: Woke-up flag off by default
- **WHEN** the first step opens
- **THEN** the switch is off

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
The button SHALL open a short step-by-step screen with these steps in order: start and end time with a woke-up-with-it switch; intensity; head area and pain character; type; symptoms; triggers; medication taken; what I did (relief methods and daily activities); comment. The start time SHALL default to now. Only the start time is required: every later step SHALL be skippable and a Save action SHALL be available on every step. Back SHALL return one step and, from the first step, leave the screen without saving. Leaving after any input SHALL ask once whether to discard it.

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
Head area SHALL be a plain list: whole head, left side, right side, forehead, behind the eyes, temples, back of the head and neck, other. Symptoms SHALL be a checklist: nausea, aura, sensitive to light, sensitive to sound, sensitive to smell, dizziness, vertigo, movement symptoms such as numbness, worse with physical activity, brain fog or trouble thinking, extreme fatigue, other. Triggers SHALL be a checklist the person fills in only from their own notion: sleep, stress, food, weather, menstruation, other. The type list SHALL offer, as the person's own label, with aura, without aura, recognized (familiar) headache, other headache and not sure. The system SHALL NOT pre-select, suggest or rank any of them, and SHALL NOT present a trigger as a cause.

#### Scenario: Several symptoms
- **WHEN** nausea and sensitivity to light are ticked
- **THEN** both are stored and shown on the entry in History

#### Scenario: New symptoms and trigger
- **WHEN** "brain fog or trouble thinking" and "extreme fatigue" are ticked and the trigger "menstruation" is ticked
- **THEN** all three are stored and shown on the entry in History as plain text

#### Scenario: Recognized headache label
- **WHEN** the type "recognized headache" is chosen
- **THEN** it is stored as the person's label and the system does not change it or draw any conclusion from it

#### Scenario: Nothing pre-selected
- **WHEN** the symptoms step opens
- **THEN** no box is ticked

### Requirement: Medication taken is free text
The medication step SHALL offer a took-something tick, a free text field (at most 100 characters) shown only when the tick is on, a took-a-second-dose tick and, only when the took-something tick is on, an optional "intensity after 2 hours" picker from 0 to 10. Turning the took-something tick off SHALL clear the intensity after 2 hours from the draft. The value SHALL be stored and shown on the entry as a plain number next to the first intensity, both as entered. The app SHALL NOT compute a difference, average, score, trend or verdict from them and SHALL NOT present the result as whether the medication worked. What the person writes themselves, for example in the comment that something helped, is their own subjective note and SHALL be stored and shown exactly as typed. The text field SHALL offer no suggestions or auto-complete, and the app SHALL contain no medicine names. This step SHALL NOT be linked to the pillbox, SHALL NOT create or change medication data and SHALL NOT raise any reminder.

#### Scenario: Free text only
- **WHEN** the person types "my usual one" and ticks a second dose
- **THEN** exactly that text and the flag are stored

#### Scenario: Intensity after 2 hours logged
- **WHEN** something taken is ticked and an intensity after 2 hours of 3 is chosen
- **THEN** 3 is stored with the entry and History shows it as a plain number

#### Scenario: Picker hidden without the tick
- **WHEN** the took-something tick is off
- **THEN** no intensity after 2 hours picker is shown, and turning the tick off after choosing a value clears that value

#### Scenario: Not interpreted
- **WHEN** an entry has intensity 7 and intensity after 2 hours 2
- **THEN** the app generates no text, tile, chart or caption that says improved, helped or effective, and shows no computed difference between the two numbers; both numbers are shown as entered

#### Scenario: The person's own note
- **WHEN** the person writes "this helped" in the comment
- **THEN** the comment is stored and shown unchanged, and the app does not react to the words

#### Scenario: Pillbox untouched
- **WHEN** a migraine with something taken is saved
- **THEN** no medication, schedule or intake log is created or changed

### Requirement: Daily activities choice
The daily-activities choice, shown on the what-I-did step, SHALL offer two choices, "could carry on" and "had to stop", of which at most one is chosen, and SHALL be skippable. The choice SHALL be shown on the entry in History as plain text and SHALL NOT be counted, scored or interpreted anywhere.

#### Scenario: Choice stored
- **WHEN** "had to stop" is chosen
- **THEN** the entry stores it and History shows it as text

#### Scenario: Not counted
- **WHEN** the Migraine view is shown for a range with entries that have this choice
- **THEN** no tile, chart or caption uses it

### Requirement: Pain character and relief methods
The where-and-what-kind step SHALL offer, after the head area list, a pain character checklist: throbbing or pounding, pressing or tight, piercing. The what-I-did step SHALL offer a relief methods checklist of what the person did besides taking something: sleep or rest, dark and quiet room, cold pack, heat, massage, coffee or tea, other. Both are multi-choice, start with nothing ticked, are skippable and are stored and shown on the entry as plain text. The system SHALL NOT pre-select, suggest or rank any item, SHALL NOT itself call a relief method effective or helpful (the person's own words in the comment are untouched), and the relief list SHALL contain no medicine or supplement names.

#### Scenario: Several pain characters
- **WHEN** throbbing or pounding and piercing are ticked
- **THEN** both are stored and shown on the entry in History

#### Scenario: Relief methods stored as done
- **WHEN** "dark and quiet room" and "sleep or rest" are ticked
- **THEN** both are stored and shown as what the person did, with no app-generated wording that says they worked

### Requirement: Optional "This helped me" note
The medication step SHALL offer an optional "This helped me" tick when something was taken, and the what-I-did step SHALL offer one when a relief method is chosen. Both SHALL be off by default and SHALL be stored as the person's own note, shown on the entry as set. The app SHALL NOT count, average, rank, chart or compare the ticks, SHALL NOT combine them with the intensity after 2 hours, and SHALL NOT suggest, pre-set or prompt for them. Turning off the field they belong to SHALL clear the tick.

#### Scenario: Person ticks this helped me
- **WHEN** the person ticks "This helped me" on the medication step and saves
- **THEN** the entry stores it and shows it as the person's note, and no tile, chart or caption reacts to it

#### Scenario: Tick is optional
- **WHEN** the person saves without touching the tick
- **THEN** it is off and nothing about it is shown

#### Scenario: Tick needs its field
- **WHEN** the took-something tick is turned off after "This helped me" was set
- **THEN** the tick is cleared

### Requirement: Personal tags for triggers and relief methods
The triggers step and the what-I-did step SHALL each end their list with an "Add my own" chip. Choosing it SHALL open a one-line text field; saving SHALL add a personal tag of that kind to the profile and select it on the entry being logged. Personal tags SHALL appear as chips after the fixed items of their list on every later entry, in the order they were added, and SHALL be selectable like the fixed items. A label SHALL be 1 to 30 characters after trimming, SHALL NOT contain `|`, and SHALL be unique within its kind and profile ignoring case; entering a duplicate SHALL select the existing tag instead of creating another. A profile SHALL hold at most 30 active tags per kind. The text field SHALL offer no suggestions, auto-complete or autofill, and the app SHALL NOT suggest, pre-select, rank or interpret a personal tag. Only triggers and relief methods SHALL have personal tags; symptoms, pain characters, head areas and type stay fixed.

A person SHALL be able to rename a personal tag and to remove it from their list, by long-press on the chip or by an accessible custom action. A rename SHALL change the label on every entry that uses the tag. Removing SHALL only stop the tag from being offered on new entries: entries that carry it SHALL keep, show and count it. Personal tags SHALL belong to one profile, SHALL be stored only in the encrypted database and SHALL NOT be shared between profiles.

#### Scenario: Add my own trigger
- **WHEN** the person taps "Add my own" on the triggers step, types "skipped lunch" and saves
- **THEN** a trigger tag "skipped lunch" is added to the profile and ticked on this entry, and it is offered as a chip on later entries

#### Scenario: Duplicate label
- **WHEN** "Skipped Lunch" is entered and "skipped lunch" already exists as a trigger tag
- **THEN** no second tag is created and the existing one is selected

#### Scenario: Invalid label
- **WHEN** an empty label, a label of 31 characters or a label containing `|` is entered
- **THEN** it is refused with a message in the app language and no tag is created

#### Scenario: Same word in both kinds
- **WHEN** "rest" is added as a trigger tag and later as a relief tag
- **THEN** two separate tags exist, one per kind

#### Scenario: Rename
- **WHEN** a tag used on three entries is renamed
- **THEN** all three entries show the new label

#### Scenario: Remove from my list
- **WHEN** a personal tag used on two entries is removed from the list
- **THEN** it is no longer offered on new entries, and both old entries still show it and it still counts in the range figures

#### Scenario: Not interpreted
- **WHEN** a personal tag is typed, such as "this helped"
- **THEN** it is shown and counted as typed, and no text, chart or caption reacts to its words

### Requirement: Migraine in History
History SHALL list migraines with the other entry types in the merged timeline, newest first by start time, each row showing start, the text "Started during sleep" when the woke-up-with-it flag is set, duration or "ongoing", intensity if given, the intensity after 2 hours as a plain number if given, and the comment if any. Tapping a row SHALL open an edit screen with the same steps and values; deleting SHALL follow the existing delete confirmation. The History metric filter SHALL include Migraine.

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
The History graph section SHALL offer a Migraine view with a Graph and Calendar toggle and the range chips week, month, 3 months, 6 months and year (the same chips and range rules as other trends). All figures SHALL be computed by pure functions in the domain layer for the active profile and range. The Graph view SHALL show, in this order: summary tiles; a timeline chart; a duration chart; an intensity chart; a weekday chart; a weekday-by-time-of-day heat map; symptom counts; trigger counts; pain character counts; relief method counts. An empty range SHALL show a plain "No migraines in this period" text and no charts.

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
The intensity chart SHALL count migraines in the number ranges 0 to 3, 4 to 6 and 7 to 10 and SHALL label them only with those numbers, never with words such as mild, moderate or severe. The weekday chart SHALL count migraines by start weekday starting on the profile locale's first day of the week. The heat map SHALL count migraines by start weekday and time of day (night 00:00 to 05:59, morning 06:00 to 11:59, afternoon 12:00 to 17:59, evening 18:00 to 23:59), with cell shade by count in a single hue and the count written in each non-empty cell. Migraines with the woke-up-with-it flag SHALL be left out of the heat map and counted in a note ("1 started during sleep"), because their clock time is an estimate; they still count in every other figure. A caption SHALL state facts only, such as "4 migraines started on Monday".

#### Scenario: Range boundaries
- **WHEN** intensities 3, 4, 6 and 7 exist
- **THEN** 0 to 3 shows 1, 4 to 6 shows 2 and 7 to 10 shows 1

#### Scenario: Time-of-day boundary
- **WHEN** migraines start at 05:59 and 06:00
- **THEN** the first counts as night and the second as morning

#### Scenario: Started during sleep in the heat map
- **WHEN** the range holds 3 migraines and one has the woke-up-with-it flag
- **THEN** the heat map shows 2, a note says "1 started during sleep", and the number of migraines is still 3

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

### Requirement: Symptom, trigger, pain character and relief counts
Four lists SHALL show how many migraines in the range had each symptom, each trigger, each pain character and each relief method, as a plain number per item, sorted by the count and then by the fixed order of the list, with personal tags after the fixed items in the order they were added and labelled as typed. Items with a count of 0 SHALL be left out. The lists SHALL NOT interpret the counts, so the relief list never says which method worked or helped most.

#### Scenario: Counts
- **WHEN** nausea is on 3 migraines and sensitivity to light on 5
- **THEN** light is listed first with 5 and nausea second with 3

### Requirement: Neutral presentation
The Migraine log SHALL use one hue in light and dark theme, SHALL NOT use red, amber or green or any colour to rank intensity or counts, SHALL NOT use the word "attack" or "aanval" in text, descriptions or documents in either language, and SHALL contain no advice, warnings, predictions, "mostly" statements, percentage headlines or suggested causes, and no app-generated wording that evaluates a medication or relief method (worked, helped, effective, improved); the person's own text is never altered, filtered or interpreted. Every chart SHALL have a text description for screen readers giving the same numbers.

#### Scenario: Screen reader
- **WHEN** a screen reader focuses the weekday chart
- **THEN** it reads each weekday with its count

#### Scenario: Wording check
- **WHEN** the string resources are scanned in English and Dutch
- **THEN** none contains "attack" or "aanval"

### Requirement: Migraine text in English and Dutch
Every Migraine string SHALL exist in English and Dutch. Dutch SHALL use "migraine" and "migraine registreren", and dates, times and weekdays SHALL follow the app language. The "Add my own" chip and its dialog and messages SHALL be translated; personal tags are the person's own text and SHALL be shown as typed in both languages.

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card button reads "Migraine registreren" and the tiles read "dagen met migraine" and "migraine-vrije dagen"
