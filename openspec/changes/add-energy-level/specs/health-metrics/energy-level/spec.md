## ADDED Requirements

### Requirement: Record energy level
The system SHALL allow recording an energy level for a Profile at a given timestamp (default now) as a whole number from 0 to 10, with the optional comment. A Profile MAY have any number of entries on the same day, including at the same minute. This metric is optional: a Profile with no entries SHALL function identically to today.

#### Scenario: Valid entry
- **WHEN** a level of 6 is recorded
- **THEN** it is stored and appears in History

#### Scenario: Bounds
- **WHEN** 0 or 10 is recorded
- **THEN** both are accepted

#### Scenario: Out of range
- **WHEN** -1, 11 or a fraction such as 6.5 is entered
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Several in one day
- **WHEN** entries are saved at 08:00, 13:00 and 20:00 on one day
- **THEN** all three are stored and listed

### Requirement: Scale direction and wording
The scale SHALL read 0 as "no energy" and 10 as "full of energy" (higher means more energy). The picker SHALL show the numbers 0 to 10 and the two end texts only; it SHALL NOT name any other number, use a colour gradient, or select a value by default. The value SHALL be presented as the person's own rating and SHALL NOT be called a score of, or compared with, any validated questionnaire (such as CIS, FSS or Borg). The picker SHALL be the shared 0-10 picker component (also used by Migraine intensity), with end texts, labels and accessibility text passed as parameters.

#### Scenario: Nothing pre-selected
- **WHEN** the form opens
- **THEN** no number is selected and Save is unavailable until one is chosen

#### Scenario: End texts only
- **WHEN** the picker is shown
- **THEN** only 0 and 10 have words next to them and all numbers share one style

### Requirement: No assessment or screening
The system SHALL NOT label, classify, colour, rank or comment on an entry or a set of entries: there SHALL be no reference range, band, limit line, threshold, alert, reminder, trend verdict, cause, advice or screening, and no words such as low, normal, good, bad, tired, fatigue, exhausted, burn-out or syndrome (or their Dutch equivalents) in the app text of this metric. Figures SHALL be plain numbers.

#### Scenario: Any value
- **WHEN** entries of 9, 5 and 1 are recorded
- **THEN** all appear in the same style with no label or colour difference

#### Scenario: Wording check
- **WHEN** the energy strings are scanned in English and Dutch
- **THEN** none contains an assessment word, a fatigue-syndrome term or a number presented as a limit

### Requirement: Energy card and form
The Log tab SHALL show an Energy card in the same style as the other metric cards with the date and time (default now), the 0-10 picker and the comment field with the counter from `entry-comments`.

#### Scenario: Save with comment
- **WHEN** a level of 4 with the comment "after lunch" is saved
- **THEN** the card clears and History shows both

### Requirement: Retrieve energy history
The system SHALL allow retrieving all entries of a Profile in reverse chronological order.

#### Scenario: History returned
- **WHEN** a Profile has three entries
- **THEN** they are returned newest first

### Requirement: Energy level text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Energie" for the metric, "Geen energie" for 0 and "Vol energie" for 10; English SHALL use "Energy", "No energy" and "Full of energy".

#### Scenario: Dutch card
- **WHEN** the app language is Dutch
- **THEN** the card is titled "Energie" and the picker ends read "Geen energie" and "Vol energie"
