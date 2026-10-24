## ADDED Requirements

### Requirement: Record a mood entry
The system SHALL allow recording a mood entry for a Profile at a given timestamp (default now), with a mood step as a whole number from 1 to 5, an optional stress level as a whole number from 1 to 5, optional feeling words and the optional comment. A Profile MAY have any number of entries on the same day and at the same minute. This metric is optional: a Profile with no mood entries SHALL function identically to today.

#### Scenario: Valid entry
- **WHEN** step 4 is chosen and the entry is saved
- **THEN** it is stored with the current date and time and appears in History

#### Scenario: Several in one day
- **WHEN** two entries are saved on the same day
- **THEN** both are stored and both appear in History

#### Scenario: Out of range
- **WHEN** a step outside 1 to 5 reaches the domain layer (for example through an import)
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Stress out of range
- **WHEN** a stress level outside 1 to 5 reaches the domain layer
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Step required
- **WHEN** the form is saved with no step chosen
- **THEN** it is refused with a plain message that a step is needed and nothing is stored

### Requirement: Five-step scale with neutral labels
The mood scale SHALL be five steps shown in one row, numbered 1 to 5, each with a text label: 1 "Very unpleasant", 2 "Unpleasant", 3 "Neutral", 4 "Pleasant", 5 "Very pleasant". The scale describes how the moment feels to the person. The data model SHALL store only the whole number; labels belong to the presentation layer. No step SHALL be pre-selected, no step SHALL be coloured to suggest good or bad, and the words good, bad, better, worse, happy, depressed and anxious SHALL NOT be used as step labels. The step SHALL NOT be shown as a face or emoji.

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no step is selected

#### Scenario: Same style for all steps
- **WHEN** the five steps are shown
- **THEN** they have the same style and differ only by number and label

#### Scenario: Screen reader
- **WHEN** a step is focused with a screen reader
- **THEN** it is announced with its number, its label and whether it is selected

### Requirement: Optional stress level
The form SHALL offer an optional second row of five steps numbered 1 to 5, asked as "How much tension or pressure do you feel right now?", with the labels 1 "Not at all", 2 "A little", 3 "Somewhat", 4 "Quite a lot", 5 "Very much". The data model SHALL store only the whole number or nothing. No step SHALL be pre-selected or coloured, the row SHALL have the same style for all steps, and the stress level SHALL NOT be used to derive, change or correlate with the mood step, the feeling words or any figure. The words stressed, burnout, overloaded, high stress and low stress SHALL NOT be used as labels or captions. A chosen step MAY be cleared by tapping it again before saving.

#### Scenario: Left empty
- **WHEN** an entry is saved with a mood step and no stress level
- **THEN** it is stored with no stress level and shown without one

#### Scenario: Stress chosen
- **WHEN** mood step 3 and stress level 4 are saved
- **THEN** both numbers are stored, shown on the History row as plain text and restored in the edit dialog

#### Scenario: Cleared
- **WHEN** a stress step is chosen and tapped again before saving
- **THEN** it is deselected and the entry is saved with no stress level

### Requirement: Feeling words
The form SHALL offer an optional, fixed set of ten feeling words as chips: calm, content, cheerful, energetic, tired, tense, restless, irritated, sad, worried. The person MAY select none, one, two or three; a fourth selection is refused with a plain note about the limit. The words are notes by the person: the system SHALL NOT pre-select, suggest, count towards any score, derive a step from them, or change any figure or presentation because of them. Stored words SHALL be stable codes CALM, CONTENT, CHEERFUL, ENERGETIC, TIRED, TENSE, RESTLESS, IRRITATED, SAD, WORRIED.

#### Scenario: No words
- **WHEN** an entry is saved with no words
- **THEN** it is stored and shown without words

#### Scenario: Words chosen
- **WHEN** an entry is saved with "calm" and "tired"
- **THEN** both are stored, shown on the History row as plain text and restored in the edit dialog

#### Scenario: Limit
- **WHEN** a fourth word is tapped
- **THEN** the selection does not change and a note states that up to three can be chosen

#### Scenario: Words do not change figures
- **WHEN** two entries with the same step have different words
- **THEN** every figure and chart is identical to that with no words

### Requirement: No assessment of entries
The system SHALL NOT label, classify, rank, score, colour as good or bad, or comment on an entry or a set of entries. There SHALL be no questionnaire, screening, risk or severity score (such as PHQ-9 or GAD-7), no threshold, band, limit line, streak, goal, alert, reminder, trend verdict, prediction or advice, and no statement about depression, anxiety or any condition, in either language. The system SHALL NOT react to a chosen step, stress level or word with a message, prompt or change of screen. Figures SHALL be plain numbers.

#### Scenario: Any step
- **WHEN** entries of 5, 3 and 1 are recorded
- **THEN** all appear in the same style, without label, colour or message that differs by value

#### Scenario: Highest stress repeated
- **WHEN** many entries with stress level 5 are saved over weeks
- **THEN** the app shows them like any others, with no warning, colour, prompt or advice

#### Scenario: Run of the same step
- **WHEN** the last fourteen entries are all step 1
- **THEN** the app shows them like any others and raises no prompt or advice

#### Scenario: Wording check
- **WHEN** the mood strings are scanned in English and Dutch
- **THEN** none contains an assessment, advice, condition or screening word

### Requirement: Mood card and form
The Log tab SHALL show a Mood card in the style of the other metric cards, with the date and time (default now), the five steps, the optional stress row, the optional feeling words and the comment field with the counter from `entry-comments`. Saving an entry SHALL need at most the step and one tap on Save. After saving, the card SHALL clear and keep no draft of the step, stress level or words.

#### Scenario: Five-second entry
- **WHEN** the person taps a step and Save
- **THEN** the entry is stored with the current time and the card clears

#### Scenario: Save with comment
- **WHEN** step 3, the word "tired" and the comment "long day" are saved
- **THEN** History shows all three

#### Scenario: Backdated entry
- **WHEN** the date and time are changed to yesterday evening
- **THEN** the entry is stored with that timestamp and sorts there in History

### Requirement: Retrieve mood history
The system SHALL allow retrieving all mood entries of a Profile in reverse chronological order, and the entries in a date range in ascending order.

#### Scenario: History returned
- **WHEN** a Profile has three entries
- **THEN** they are returned newest first

#### Scenario: Equal timestamps
- **WHEN** two entries have the same timestamp
- **THEN** both are returned, in the order they were recorded

### Requirement: Mood relates to Mindfulness only by sitting side by side
The Mood log SHALL NOT link to, launch, suggest or be launched from the Mindfulness area, and Mindfulness SHALL NOT read or write mood entries. The Profile notice SHALL describe them as separate: Mindfulness records nothing, the Mood log records only what is typed.

#### Scenario: No link
- **WHEN** a mood entry is saved
- **THEN** no screen, message or button points to a breathing exercise or meditation

#### Scenario: Mindfulness stays unrecorded
- **WHEN** a breathing exercise is finished
- **THEN** no mood entry is created or changed

### Requirement: Mood text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. English SHALL use "Mood" for the metric and the step labels above, and "Stress" for the stress row with the question and labels above. Dutch SHALL use "Stemming" for the metric, "Zeer onaangenaam", "Onaangenaam", "Neutraal", "Aangenaam" and "Zeer aangenaam" for the steps, "Stress" for the stress row with the question "Hoeveel spanning of druk voel je nu?" and the labels "Helemaal niet", "Een beetje", "Redelijk", "Best veel" and "Heel veel", and "Rustig", "Tevreden", "Vrolijk", "Energiek", "Moe", "Gespannen", "Onrustig", "Prikkelbaar", "Verdrietig" and "Bezorgd" for the feeling words.

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card is titled "Stemming" and the steps and words read as above
