## ADDED Requirements

### Requirement: Record daily activity
The system SHALL allow recording one activity entry per calendar day for a Profile, with a date (default today, not in the future), an optional number of steps (whole number from 0 to 100000), an optional number of active minutes (whole number from 0 to 1440), an optional activity type and the optional comment. At least one of steps and active minutes SHALL be present. The numbers are typed by the person; the system SHALL NOT read any sensor, count steps, or import data from another app. This metric is optional: a Profile with no activity entries SHALL function identically to today.

#### Scenario: Steps only
- **WHEN** an entry with 6500 steps and no minutes is saved
- **THEN** it is stored and appears in History

#### Scenario: Minutes only
- **WHEN** an entry with 45 active minutes and no steps is saved
- **THEN** it is stored and appears in History

#### Scenario: Neither number
- **WHEN** both steps and minutes are empty
- **THEN** saving is refused with a message in the app language and nothing is stored

#### Scenario: Out of range or fraction
- **WHEN** steps of 100001, minutes of 1441, or a fraction such as 45.5 are entered
- **THEN** it is refused and nothing is stored

#### Scenario: Future date
- **WHEN** a date after today is chosen
- **THEN** saving is refused

### Requirement: One entry per day
The system SHALL keep at most one activity entry per Profile and date. WHEN the form date matches an existing entry, THEN the form SHALL load that entry and Save SHALL update it. The system SHALL NOT add the entries of a day together.

#### Scenario: Second save the same day
- **WHEN** an entry for today with 4000 steps exists and the person opens the form for today, changes steps to 7000 and saves
- **THEN** one entry for today exists with 7000 steps

#### Scenario: Different day
- **WHEN** the date is changed to yesterday and no entry exists for it
- **THEN** the form is empty and Save creates a new entry

### Requirement: Activity type
The activity type SHALL be an optional choice from: walking, cycling, swimming, sport, housework or garden, other, stored as a stable code. It is a note by the person. The system SHALL NOT pre-select it, derive it from the numbers or interpret it. Free text belongs in the comment.

#### Scenario: No type
- **WHEN** an entry is saved without a type
- **THEN** it is stored and shown without one

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no type is selected

### Requirement: No target or assessment
The system SHALL NOT set, suggest or display a target, goal, norm or reference for activity: there SHALL be no goal line, progress ring, percentage of a goal, streak, badge, colour threshold, reminder, advice and no words such as enough, too little, active enough or on track in either language. No national guideline value (minutes per week or steps per day) SHALL appear in the app. Figures SHALL be plain numbers.

#### Scenario: Any value
- **WHEN** entries of 12000, 3000 and 500 steps are recorded
- **THEN** all appear in the same style with no label or colour difference

#### Scenario: Wording check
- **WHEN** the activity strings are scanned in English and Dutch
- **THEN** none contains an assessment word or a number presented as a goal

### Requirement: Daily activity card and form
The Log tab SHALL show a Daily activity card in the same style as the other metric cards, with the date (default today), steps, active minutes, the optional activity type and the comment field with the counter from `entry-comments`. Number fields SHALL accept digits only.

#### Scenario: Save with comment
- **WHEN** an entry of 8000 steps with the comment "long walk with the dog" is saved
- **THEN** the card clears and History shows both

### Requirement: Retrieve daily activity history
The system SHALL allow retrieving all activity entries of a Profile in reverse chronological order of date.

#### Scenario: History returned
- **WHEN** a Profile has three entries
- **THEN** they are returned newest date first

### Requirement: Daily activity text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Dagelijkse beweging" for the metric, "Stappen" and "Actieve minuten" for the numbers, and "Wandelen", "Fietsen", "Zwemmen", "Sport", "Huishouden of tuin" and "Anders" for the types.

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card is titled "Dagelijkse beweging" and the fields and types read as above
