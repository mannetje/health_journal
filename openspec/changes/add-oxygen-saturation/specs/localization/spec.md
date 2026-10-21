## ADDED Requirements

### Requirement: Oxygen saturation text in English and Dutch
All oxygen saturation labels, contexts, the form tip, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates and times, and fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the form is open
- **THEN** every label, the tip and the contexts are Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the card, context choices and graph figures stay readable
