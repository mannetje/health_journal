## ADDED Requirements

### Requirement: Sleep text in English and Dutch
All sleep labels (English "Sleep", "Went to bed", "Woke up", "Quality", "Times woken"; Dutch "Slaap", "Naar bed", "Wakker geworden", "Kwaliteit", "Aantal keer wakker"), durations written as hours and minutes ("7 h 30 min" in both languages), captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates and times (12 or 24 hour clock as the device uses), (English chart description "Bars show hours slept per night", Dutch "Staven tonen de uren slaap per nacht"; figure labels "Average", "Shortest", "Longest", "Nights logged", "Average bed time", "Average wake time" and Dutch "Gemiddeld", "Kortst", "Langst", "Nachten gelogd", "Gemiddelde bedtijd", "Gemiddelde wektijd"; CSV import and export messages) and fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the form is open
- **THEN** every label and message is Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the card, the pickers, the quality choices and the graph figures stay readable
