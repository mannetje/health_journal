## ADDED Requirements

### Requirement: Sleep log is a personal log with no assessment
The sleep log SHALL only record what the person enters and show it back as times, durations and plain figures. It SHALL NOT assess, classify or interpret a duration, quality or number of times woken, SHALL NOT use a target, hour range or threshold of any source, SHALL NOT raise an alert or reminder from a value, SHALL NOT advise (on sleep habits or on when to contact a care provider), SHALL NOT name or suggest a sleep disorder and SHALL NOT claim to measure or monitor sleep. The Profile notice SHALL say that the app records what is entered and does not assess it.

#### Scenario: Very short night entered
- **WHEN** a night of 2 hours is saved
- **THEN** the app shows it like any other entry, with no warning, colour or advice

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered and does not assess it, in English and Dutch

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the sleep texts contain no target, advice, diagnosis or assessment word
