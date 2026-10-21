## ADDED Requirements

### Requirement: Mood text in English and Dutch
All Mood log labels (English "Mood", Dutch "Stemming"), the five step labels, the ten feeling words, captions, chart descriptions, messages from the presentation logic, Profile notice sentences and export or import messages SHALL exist in English and Dutch, follow the app language and region for dates and times, and fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the form is open
- **THEN** every label, step and feeling word is Dutch

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the five steps (with "Zeer onaangenaam" and "Zeer aangenaam"), the word chips and the figures stay readable
