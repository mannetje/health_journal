## ADDED Requirements

### Requirement: Energy level text in English and Dutch
All energy level labels, the picker end texts and accessibility text, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates and times, and fit narrow phones and long Dutch words without clipping. English terms: "Energy", "No energy", "Full of energy". Dutch terms: "Energie", "Geen energie", "Vol energie".

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the form is open
- **THEN** every label and both picker end texts are Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the card, picker and figures stay readable
