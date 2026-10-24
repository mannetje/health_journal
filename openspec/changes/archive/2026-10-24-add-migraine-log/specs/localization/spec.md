## ADDED Requirements

### Requirement: Migraine text in English and Dutch
All Migraine labels, step titles, list items, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, SHALL follow the app language and region for dates, times, weekdays and the first day of the week, and SHALL fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch steps
- **WHEN** the app language is Dutch and the logging screen is open
- **THEN** every step title and item is Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the heat map, calendar and buttons remain readable and nothing is cut off
