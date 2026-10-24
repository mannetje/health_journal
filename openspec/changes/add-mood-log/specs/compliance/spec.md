## ADDED Requirements

### Requirement: Mood log is a personal log with no assessment
The Mood log SHALL only record what the person enters and show it back as numbers, dates and the person's own words. It SHALL NOT diagnose, screen for, monitor or classify any mental or physical condition, SHALL NOT contain or compute any questionnaire or score (including PHQ-9, GAD-7, PSS and similar instruments), SHALL NOT label, band or interpret the optional stress level (no "high stress", no limit, no advice to relax or to seek help), SHALL NOT use a threshold, band, streak or trend verdict, SHALL NOT raise an alert, reminder or prompt from an entered value, SHALL NOT advise or encourage (including when to contact a care provider) and SHALL NOT claim to measure or improve mood or wellbeing. The Profile notice SHALL say that the app records what is entered and does not assess it, and that it is not a questionnaire, a screening or a diagnosis. Documents and store text SHALL describe it as a personal log of how the moments of a day felt. The existing pointer to the doctor, the pharmacist and Thuisarts for questions SHALL apply unchanged; any further fixed pointer (for example to a crisis line) SHALL be added only after an explicit decision recorded in ADR 0028.

#### Scenario: Lowest steps repeated
- **WHEN** many entries of step 1 are saved over weeks
- **THEN** the app shows them like any other entries, with no warning, colour change, prompt or advice

#### Scenario: Stress level
- **WHEN** many entries with a high stress level are saved
- **THEN** the app shows the numbers like any others, with no label, warning, colour change, prompt or advice

#### Scenario: Notice
- **WHEN** the Profile notice is opened
- **THEN** it states that the app records what is entered and does not assess it, and that it is no questionnaire, screening or diagnosis, in English and Dutch

#### Scenario: No instruments
- **WHEN** the source and string resources are scanned
- **THEN** they contain no questionnaire items, scoring or cut-off for any depression or anxiety instrument

#### Scenario: Claims
- **WHEN** the README, store listing or changelog describe the Mood log
- **THEN** they make no claim to improve, treat, prevent or detect any condition

#### Scenario: Document check
- **WHEN** the repository documents and string resources are scanned
- **THEN** the mood texts contain no threshold, advice, condition or assessment word
