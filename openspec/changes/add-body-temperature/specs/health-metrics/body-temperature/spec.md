## ADDED Requirements

### Requirement: Record body temperature
The system SHALL allow recording a body temperature reading for a Profile at a given timestamp, as degrees Celsius with one decimal from 30.0 to 45.0, with an optional measurement site and the optional comment. The value SHALL be stored as an integer number of tenths of a degree. This metric is optional: a Profile with no readings SHALL function identically to today.

#### Scenario: Valid reading
- **WHEN** a reading of 36.8 is recorded
- **THEN** it is stored as 368 tenths and appears in History as 36.8 °C

#### Scenario: Comma accepted
- **WHEN** "37,2" is typed
- **THEN** it is read as 37.2

#### Scenario: Out of range
- **WHEN** a value below 30.0 or above 45.0 is entered
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Too many decimals
- **WHEN** 36.85 is entered
- **THEN** it is refused rather than rounded

### Requirement: Measurement site
The measurement site SHALL be an optional choice from: ear, mouth, armpit, forehead, rectal. It is a note by the person. The system SHALL NOT pre-select it, derive it from other data, interpret it, correct or convert a value because of it, or change any figure or presentation because of it.

#### Scenario: No site
- **WHEN** a reading is saved without a site
- **THEN** it is stored and shown without one

#### Scenario: Site used
- **WHEN** a reading is saved with site "ear"
- **THEN** it is stored, shown on the History row as plain text and restored in the edit dialog

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no site is selected

### Requirement: No assessment of readings
The system SHALL NOT label, classify, colour, rank or comment on a reading or a set of readings: there SHALL be no reference range, band, limit line, threshold, alert, trend verdict or advice, and no words such as fever, koorts, low, high, normal, good or bad in either language. Figures SHALL be plain numbers.

#### Scenario: Any value
- **WHEN** readings of 36.4, 38.9 and 35.2 are recorded
- **THEN** all appear in the same style with no label or colour difference

#### Scenario: Wording check
- **WHEN** the temperature strings are scanned in English and Dutch
- **THEN** none contains an assessment word, "fever" or "koorts", or a number presented as a limit

### Requirement: Body temperature card and form
The Log tab SHALL show a Body temperature card in the same style as the other metric cards, with the date and time (default now), the value, the optional site and the comment field with the counter from `entry-comments`. The card SHALL show no tip, warning or advice text.

#### Scenario: Save with comment
- **WHEN** a reading of 37.1 with the comment "after lunch" is saved
- **THEN** the card clears and History shows both

### Requirement: Retrieve body temperature history
The system SHALL allow retrieving all readings of a Profile in reverse chronological order.

#### Scenario: History returned
- **WHEN** a Profile has three readings
- **THEN** they are returned newest first

### Requirement: Body temperature text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Lichaamstemperatuur" for the metric and "Oor", "Mond", "Oksel", "Voorhoofd" and "Rectaal" for the sites; English SHALL use "Body temperature", "Ear", "Mouth", "Armpit", "Forehead" and "Rectal".

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card is titled "Lichaamstemperatuur" and the sites read as above
