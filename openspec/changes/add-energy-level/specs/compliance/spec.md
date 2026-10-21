## ADDED Requirements

### Requirement: Energy level is a personal log with no assessment or screening
The energy level metric SHALL only record what the person enters and show it back as numbers and dates. It SHALL NOT assess, classify or interpret an entry, SHALL NOT use a threshold or reference range of any source, SHALL NOT screen for or name any condition (including fatigue syndromes, CVS/ME, burn-out or depression), SHALL NOT implement a validated questionnaire (CIS, FSS, Borg or similar) as a score, SHALL NOT raise an alert or reminder from a value, and SHALL NOT advise (including on sleep, exercise or when to contact a care provider). The Profile notice SHALL say that the app records what is entered and does not assess it.

#### Scenario: Very low entered value
- **WHEN** a level of 0 is saved on ten consecutive days
- **THEN** the app shows each like any other entry, with no warning, colour, pattern message or advice

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered and does not assess it, in English and Dutch

#### Scenario: Document check
- **WHEN** the repository documents and string resources of this metric are scanned
- **THEN** they contain no threshold, advice, screening or assessment wording
