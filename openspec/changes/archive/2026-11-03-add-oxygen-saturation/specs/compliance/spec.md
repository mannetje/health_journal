## ADDED Requirements

### Requirement: Oxygen saturation is a personal log with no assessment
The oxygen saturation metric SHALL only record what the person enters and show it back as numbers and dates. It SHALL NOT assess, classify or interpret a reading, SHALL NOT use a reference range or threshold of any source, SHALL NOT raise an alert or reminder from a value, SHALL NOT advise (including when to contact a care provider) and SHALL NOT claim to measure or monitor. The Profile notice SHALL say that the app records what is entered and does not assess it. Naming and value sets MAY follow the Dutch health information standard (zib) where this adds no function and no claim of conformance is made.

#### Scenario: Very low entered value
- **WHEN** a reading of 82 is saved
- **THEN** the app shows it like any other reading, with no warning, colour or advice

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered and does not assess it, in English and Dutch

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the oxygen texts contain no threshold, advice or assessment word
