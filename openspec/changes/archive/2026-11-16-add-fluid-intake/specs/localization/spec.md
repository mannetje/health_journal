## ADDED Requirements

### Requirement: Fluid intake text in English and Dutch
All fluid intake labels, drink names, the count and portion settings, the Today line, the minimum and maximum labels and fields (Dutch "Jouw minimum", "Jouw maximum"), the position labels (Dutch "Onder je minimum", "Binnen je grenzen", "Boven je maximum"), the "About these amounts" note (Dutch "Over deze hoeveelheden") with its source texts, the Undo message, day-total rows, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates, times and number formatting (the unit "ml" is kept in both), and fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the card is open
- **THEN** every label, the buttons, the Today line, the position label and the Undo message are Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the drink buttons, the Today line and the graph figures stay readable
