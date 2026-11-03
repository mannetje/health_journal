## ADDED Requirements

### Requirement: Record oxygen saturation
The system SHALL allow recording an oxygen saturation reading for a Profile at a given timestamp, as a whole percent from 50 to 100, with an optional pulse from 30 to 250 beats per minute, an optional context, an optional measurement site, an optional "oxygen given" tick and the optional comment. This metric is optional: a Profile with no readings SHALL function identically to today.

#### Scenario: Valid reading
- **WHEN** a reading of 97 percent with a pulse of 68 is recorded
- **THEN** it is stored and appears in History

#### Scenario: Out of range
- **WHEN** a value below 50 or above 100, or a fraction such as 97.5, is entered
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Pulse out of range
- **WHEN** a pulse of 20 is entered
- **THEN** it is refused; leaving the pulse empty is accepted

### Requirement: Context of the reading
The context SHALL be an optional choice from: at rest, after activity, asleep or just woke, other. It is a note by the person. The system SHALL NOT pre-select it, derive it from the time of day or interpret it.

#### Scenario: No context
- **WHEN** a reading is saved without a context
- **THEN** it is stored without one and shown without one

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no context is selected

### Requirement: Measurement site and oxygen given
The measurement site SHALL be an optional choice from: finger, toe, earlobe. The "oxygen given" tick SHALL be optional, off by default and mean only that the person ticked it. Both are notes by the person: the system SHALL NOT pre-select the site, derive either from other data, interpret them, or change any figure or presentation because of them.

#### Scenario: Neither used
- **WHEN** a reading is saved without a site and without the tick
- **THEN** it is stored and shown without them

#### Scenario: Both used
- **WHEN** a reading is saved with site "finger" and the tick on
- **THEN** both are stored, shown on the History row as plain text and restored in the edit dialog

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no site is selected and the tick is off

### Requirement: No assessment of readings
The system SHALL NOT label, classify, colour, rank or comment on a reading or a set of readings: there SHALL be no reference range, band, limit line, threshold, alert, trend verdict or advice, and no words such as low, normal, good or bad in either language. Figures SHALL be plain numbers.

#### Scenario: Any value
- **WHEN** readings of 99, 93 and 88 are recorded
- **THEN** all appear in the same style with no label or colour difference

#### Scenario: Wording check
- **WHEN** the oxygen strings are scanned in English and Dutch
- **THEN** none contains an assessment word or a number presented as a limit

### Requirement: Oxygen saturation card and form
The Log tab SHALL show an Oxygen saturation card in the same style as the other metric cards, with the date and time (default now), the value, the optional pulse, the optional context, the optional measurement site, the optional "oxygen given" tick and the comment field with the counter from `entry-comments`. The form SHALL show once a plain tip "Many people sit still for about 10 minutes first" with a dismiss action that hides it for good; the tip SHALL NOT be styled as a warning or tied to a value.

#### Scenario: Save with comment
- **WHEN** a reading of 96 with the comment "after the stairs" is saved
- **THEN** the card clears and History shows both

#### Scenario: Tip dismissed
- **WHEN** the tip is dismissed
- **THEN** it does not return on the next opening of the form

### Requirement: Retrieve oxygen saturation history
The system SHALL allow retrieving all readings of a Profile in reverse chronological order.

#### Scenario: History returned
- **WHEN** a Profile has three readings
- **THEN** they are returned newest first

### Requirement: Oxygen saturation text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Zuurstofsaturatie" for the metric and "Rust", "Na inspanning", "Slapend of net wakker" and "Anders" for the contexts, "Vinger", "Teen" and "Oorlel" for the sites and "Zuurstof toegediend" for the tick.

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card is titled "Zuurstofsaturatie" and the contexts, sites and tick read as above
