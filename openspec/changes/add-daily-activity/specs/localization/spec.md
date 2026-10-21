## ADDED Requirements

### Requirement: Daily activity text in English and Dutch
All daily activity labels, activity types, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates and number grouping, and fit narrow phones and long Dutch words without clipping. English SHALL use "Daily activity", "Steps", "Active minutes" and the types "Walking", "Cycling", "Swimming", "Sport", "Housework or garden" and "Other"; Dutch SHALL use "Dagelijkse beweging", "Stappen", "Actieve minuten" and "Wandelen", "Fietsen", "Zwemmen", "Sport", "Huishouden of tuin", "Anders". The figures SHALL read in English "Average per day logged", "Highest day", "Days logged" and "Total", and in Dutch "Gemiddeld per dag met invoer", "Hoogste dag", "Dagen met invoer" and "Totaal".

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the form is open
- **THEN** every label and type is Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the card, type choices and graph figures stay readable
