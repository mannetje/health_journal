## ADDED Requirements

### Requirement: Fluid intake is a personal log; the app sets no target
The fluid intake metric SHALL only record what the person enters and show it back as numbers, dates and, when the person has set their own minimum or maximum, a neutral position label. The app SHALL NOT set, suggest, prefill, default or recommend a target, minimum or maximum of any source (including Voedingscentrum, Thuisarts or a care team), SHALL NOT use a progress ring, progress bar, percentage or caption toward an amount (the glass gauge drawn from the person's own minimum or maximum, with the plain total printed beside it and no percentage, is the one allowed picture), SHALL NOT remind or alert, SHALL NOT advise (including how much to drink or when to contact a care provider) and SHALL NOT claim to monitor, and SHALL NOT attach caffeine, sugar, alcohol or nutrition framing, a judgement or a per-drink amount advice to any drink (a drink is a label; all drinks count equally). A person MAY enter their own minimum and maximum (for example a maximum from the huisarts for a condition, or a minimum intake); the position label ("Below your minimum", "Within your limits", "Above your maximum", always with the number) and the thin dashed lines are allowed because they compare the total with the person's own number, in the manner of the range labels permitted by "Reference ranges are informational and neutral", and the app SHALL NOT colour them as a warning or word anything about them as advice. The limit fields SHALL have no default, example or hint value. General amounts from Voedingscentrum MAY appear only in the "About these amounts" note, as that source's statements with the source named. The Profile notice SHALL say that the app records what is entered, gives no advice, and that a minimum or maximum shown is the person's own. Naming MAY follow the Dutch health information standard (zib) where this adds no function and no claim of conformance is made.

#### Scenario: Total above the person's maximum
- **WHEN** the person's maximum is 1500 and the total today is 1700 ml
- **THEN** the card shows "Above your maximum · 1500 ml" in the neutral ramp, with no warning colour, alert or advice

#### Scenario: Total below the person's minimum
- **WHEN** the person's minimum is 1400 and the total today is 900 ml
- **THEN** the card shows "Below your minimum · 1400 ml" in the neutral ramp, with no nudge, alert or advice

#### Scenario: No numbers set and a very high total
- **WHEN** no minimum or maximum is set and a day totals 6000 ml
- **THEN** the app shows it like any other day, with no label

#### Scenario: Glass gauge
- **WHEN** the person's maximum is 1500 and the total is 900 ml
- **THEN** the glass shows the fill against the person's own number with a tick, the total in ml, and no percentage, caption, ring or encouragement

#### Scenario: Limit fields
- **WHEN** the limits fields are shown for the first time
- **THEN** both are empty with no default, example or suggested number

#### Scenario: General amounts only in the note
- **WHEN** the screens of the metric are reviewed
- **THEN** the Voedingscentrum amounts appear only in the "About these amounts" note, with the source, and nowhere as a default, hint or target

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered, gives no advice or target, and that a shown minimum or maximum is the person's own, in English and Dutch

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the fluid texts contain no target of the app, recommended amount, advice or verdict word; the position label words and the person's own numbers shown back are allowed
