## ADDED Requirements

### Requirement: Record a night of sleep
The system SHALL allow recording a sleep entry for a Profile with a bed time and a wake time (both date and time), an optional quality as a whole number from 1 to 5, an optional number of times woken as a whole number from 0 to 30, and the optional comment. The wake time MUST be after the bed time and at most 24 hours later. This metric is optional: a Profile with no sleep entries SHALL function identically to today.

#### Scenario: Valid entry
- **WHEN** bed time yesterday 23:15 and wake time today 06:45 are saved
- **THEN** the entry is stored and appears in History

#### Scenario: Wake before bed
- **WHEN** a wake time equal to or before the bed time is entered
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Longer than 24 hours
- **WHEN** the wake time is more than 24 hours after the bed time
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Quality or times woken out of range
- **WHEN** a quality of 6 or a number of times woken of 31 is entered
- **THEN** it is refused; leaving either empty is accepted

### Requirement: Default times
The form SHALL default the bed time to 23:00 on the previous calendar day and the wake time to 07:00 today, on the device clock and time zone, and both SHALL be editable with the date and time pickers of the other metrics. Nothing SHALL be derived from earlier entries or from other data, and no quality or number of times woken SHALL be pre-selected.

#### Scenario: Form opens
- **WHEN** the form opens on the 21st at any time of day
- **THEN** the bed time reads the 20th at 23:00 and the wake time reads the 21st at 07:00

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no quality is selected and the number of times woken is empty

### Requirement: Duration is computed and never judged
The system SHALL compute the duration as the time between the bed time and the wake time (as instants, so a night with a daylight-saving change shows its real length), show it as hours and minutes in the form, History and graph, and SHALL NOT store it. The system SHALL NOT label, classify, colour, rank or comment on a duration, a quality or a number of times woken: there SHALL be no target, band, goal line, threshold, score, alert or advice, and no words such as enough, too little, good or bad in either language.

#### Scenario: Duration shown
- **WHEN** an entry has bed time 23:15 and wake time 06:45 the next day
- **THEN** the duration reads 7 h 30 min

#### Scenario: Daylight-saving night
- **WHEN** an entry spans the night the clocks go forward by one hour, from 23:00 to 07:00 local time
- **THEN** the duration reads 7 h 0 min

#### Scenario: Any duration
- **WHEN** entries of 4 h, 8 h and 11 h are recorded
- **THEN** all appear in the same style with no label or colour difference

#### Scenario: Wording check
- **WHEN** the sleep strings are scanned in English and Dutch
- **THEN** none contains an assessment word or a number presented as a target

### Requirement: Night belongs to the day of waking
The system SHALL assign each entry to the calendar day (device time zone) of its wake time, for sorting, grouping in History, the range selection and the graph. An entry whose wake time lies in the range counts in the range even if the bed time lies before it.

#### Scenario: Over midnight
- **WHEN** an entry has bed time the 20th at 23:15 and wake time the 21st at 06:45
- **THEN** it is listed and graphed on the 21st

### Requirement: Sleep card and form
The Log tab SHALL show a Sleep card in the same style as the other metric cards, with the bed time and wake time pickers, the computed duration as text, the optional quality (five choices from 1 to 5, none selected), the optional number of times woken and the comment field with the counter from `entry-comments`. Saving SHALL be possible with the defaults untouched.

#### Scenario: Quick save
- **WHEN** the card opens and Save is tapped without changes
- **THEN** a night from yesterday 23:00 to today 07:00 is stored with no quality and no times woken

#### Scenario: Save with comment
- **WHEN** an entry is saved with the comment "woke up for the neighbour's dog"
- **THEN** the card resets and History shows the comment

### Requirement: Retrieve sleep history
The system SHALL allow retrieving all sleep entries of a Profile in reverse chronological order of wake time.

#### Scenario: History returned
- **WHEN** a Profile has three entries
- **THEN** they are returned newest first by wake time

### Requirement: Sleep text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Slaap" for the metric, "Naar bed" for the bed time, "Wakker geworden" for the wake time, "Kwaliteit" for the quality and "Aantal keer wakker" for the number of times woken.

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card is titled "Slaap" and the fields read as above
