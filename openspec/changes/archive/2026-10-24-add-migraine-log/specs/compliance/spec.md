## ADDED Requirements

### Requirement: Migraine log is a personal log
The Migraine log SHALL only record what the person enters and show it back as counts, ranges and dates. It SHALL NOT diagnose or classify a migraine, SHALL NOT suggest a type, trigger or cause, SHALL NOT count or limit medication use days, SHALL NOT predict a next migraine, SHALL NOT warn or advise (including when to see a doctor), SHALL NOT raise alerts from entered values and SHALL NOT contain medicine names. It SHALL NOT evaluate or rank a medication or a relief method by effect: the intensity after 2 hours and the relief methods are notes shown back as entered, and the person's own subjective words (for example that something helped) are theirs to write and are never altered or acted on. Personal tags are the person's own words: they are shown as typed and counted plainly, and the app SHALL NOT suggest, rank or interpret them. The optional "This helped me" ticks are the person's own note: stored and shown as set, never counted, rated or prompted. Documents and store text SHALL describe it as a personal log that can be shared with a care provider as a CSV file.

#### Scenario: Many entries
- **WHEN** a range holds many migraines or many days with something taken
- **THEN** the view shows the numbers only, with no warning, colour change or advice

#### Scenario: Type is the person's own
- **WHEN** a migraine has symptoms that resemble aura but the type is "not sure"
- **THEN** the type stays "not sure"

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the Migraine texts contain no medicine names, advice or the word "attack"
