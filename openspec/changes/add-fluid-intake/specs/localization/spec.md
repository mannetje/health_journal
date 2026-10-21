## ADDED Requirements

### Requirement: Fluid intake text in English and Dutch
All fluid intake labels, quick-add button names and size settings, the Today line (with and without "of" the limit, Dutch "van"), the limit label and field (Dutch "Jouw limiet"), the Undo message, day-total rows, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates, times and number formatting (the unit "ml" is kept in both), and fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the card is open
- **THEN** every label, the buttons, the Today line and the Undo message are Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the three quick-add buttons, the Today line and the graph figures stay readable
