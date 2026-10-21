## ADDED Requirements

### Requirement: Fluid intake is a personal log; the app sets no target
The fluid intake metric SHALL only record what the person enters and show it back as numbers and dates. The app SHALL NOT set, suggest, prefill or recommend a target, minimum or maximum of any source (including Voedingscentrum or a care team), SHALL NOT use a progress indicator toward an amount, SHALL NOT assess or classify an amount or a day (no hydration verdict), SHALL NOT remind or alert, SHALL NOT advise (including how much to drink or when to contact a care provider) and SHALL NOT claim to monitor. A person MAY enter their own daily limit (for example from a care team's fluid restriction); showing that number back as a plain number and a thin dashed line is allowed because the line is the person's own number, not the app's, and the app SHALL NOT colour, flag, alert or word anything about it ("over", "exceeded", "remaining", "left"). The limit field SHALL have no default, example or hint value. The Profile notice SHALL say that the app records what is entered, does not assess it or set a target, and that a limit shown is the person's own. Naming MAY follow the Dutch health information standard (zib) where this adds no function and no claim of conformance is made.

#### Scenario: Total above the person's limit
- **WHEN** the person's limit is 1500 and the total today is 1700 ml
- **THEN** the card shows "1700 of 1500 ml" in the usual style, with no warning, colour, alert or advice

#### Scenario: No limit and a very high total
- **WHEN** no limit is set and a day totals 6000 ml
- **THEN** the app shows it like any other day

#### Scenario: Very low entered total
- **WHEN** a day totals 200 ml
- **THEN** the app shows it like any other day, with no nudge or message

#### Scenario: Limit field
- **WHEN** the limit field is shown for the first time
- **THEN** it is empty with no default, example or suggested number

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered, does not assess it or set a target, and that a shown limit is the person's own, in English and Dutch

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the fluid texts contain no target of the app, recommended amount, advice or assessment word; the person's own limit number shown back is allowed
