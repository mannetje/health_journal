## ADDED Requirements

### Requirement: Daily activity is a personal log with no target
The daily activity metric SHALL only record what the person types and show it back as numbers and dates. It SHALL NOT set or display a target, goal or norm (including the Dutch Beweegrichtlijn or any step goal), SHALL NOT assess whether the amount is enough, SHALL NOT raise an alert or reminder, SHALL NOT advise, and SHALL NOT claim to measure or track movement. The Profile notice SHALL say that the app records what is entered and does not assess it. A personal goal set by the person is out of scope for this change.

#### Scenario: Very low or very high day
- **WHEN** an entry of 0 steps and an entry of 40000 steps are saved
- **THEN** both are shown like any other entry, with no warning, colour, praise or advice

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered and does not assess it, in English and Dutch

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the activity texts contain no goal, norm, advice or assessment word
