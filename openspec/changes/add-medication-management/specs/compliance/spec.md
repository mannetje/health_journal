## ADDED Requirements

### Requirement: Intended purpose is a personal logging and reminder tool
The system SHALL be, and SHALL describe itself everywhere as, a personal tool for logging and reminders for private use. It SHALL NOT be intended for, claim, or behave as a medical device under the EU Medical Device Regulation (2017/745), and SHALL NOT be offered as a tool for healthcare professionals, as part of care, or as a substitute for professional advice.

#### Scenario: Intended purpose stated consistently
- **WHEN** the app, the README, the store listing or any documentation describes what the app is for
- **THEN** it SHALL use the same statement: a personal logging and reminder tool for private use, not a medical device

#### Scenario: Not-a-medical-device notice shown
- **WHEN** the user first opens the app, and from the settings or profile screen afterwards
- **THEN** the system SHALL show that the app is a personal logging and reminder tool, is not a medical device, gives no medical advice, and does not replace a doctor or pharmacist

### Requirement: No functions that would make the app a medical device
The system SHALL NOT diagnose, predict, screen for or monitor disease, SHALL NOT recommend or adjust doses or treatment, SHALL NOT check interactions or contraindications, SHALL NOT interpret measurements as a clinical finding, and SHALL NOT trigger alarms or advice based on health values.

#### Scenario: Reminders are time-based only
- **WHEN** a reminder is created or shown
- **THEN** it SHALL depend only on the user-defined schedule and never on a measured value or an interpretation of one

#### Scenario: No clinical recommendations
- **WHEN** the system displays a measurement, a trend, an adherence figure or a missed dose
- **THEN** it SHALL NOT say what the user should do medically, and SHALL point to the doctor, the pharmacist, Thuisarts or apotheek.nl for questions

#### Scenario: No health-value-driven alerts
- **WHEN** a recorded value is high or low
- **THEN** the system SHALL NOT raise an alert, warning or urgency indicator about its health meaning

### Requirement: No specific medicine names in the product or its documents
The system SHALL NOT contain, suggest, list or autocomplete specific medicine or brand names, and the specs, design, README, store listing, screenshots, test data and examples SHALL use neutral placeholders such as "Medication A". Medication names exist only as text the user types.

#### Scenario: No built-in names
- **WHEN** the user adds a medication
- **THEN** the name field SHALL be free text with no suggestions, list, lookup or autocomplete of medicine names

#### Scenario: Documents and tests use placeholders
- **WHEN** a spec, document, screenshot, string resource or test needs an example medication
- **THEN** it SHALL use a placeholder name and SHALL NOT use a real medicine or brand name

### Requirement: No advice of any kind
The system SHALL NOT give advice, instructions, recommendations, warnings, tips, coaching or encouragement about health, measurements, lifestyle or medication, in any screen, notification, message, error text, empty state, documentation or store listing. This applies most strictly to medication: the app SHALL NOT say whether, when, how much or how to take, skip, stop, change or catch up on any medicine. All text about a medication SHALL come from the user's own input or be a neutral status (for example Taken, Skipped, Missed, Pending).

#### Scenario: Notification text is neutral
- **WHEN** a medication reminder is shown
- **THEN** it SHALL contain only the user's medication name and dose and the planned time (or the generic "Medication reminder" when details are hidden), with no instruction wording such as "take now" or "do not miss"

#### Scenario: Validation messages are about input only
- **WHEN** an entered value is rejected
- **THEN** the message SHALL state only what the app accepts (for example the allowed range of the field), and SHALL NOT suggest what is medically right

#### Scenario: Empty states and onboarding
- **WHEN** a screen has no data
- **THEN** it SHALL describe how to use the app and SHALL NOT suggest health actions

#### Scenario: Wording review
- **WHEN** a new or changed user-visible string mentions health or medication
- **THEN** it SHALL be reviewed against this requirement before merge, and strings in English and Dutch SHALL both comply

### Requirement: Reference ranges are informational and neutral
The system SHALL present guideline ranges (for example NHG ranges for blood pressure and glucose, and BMI classes) as informational context with the named source, in neutral wording that does not read as a diagnosis, and SHALL NOT use diagnostic labels as the verdict on the user.

#### Scenario: Neutral wording with source
- **WHEN** a value is shown with a range label
- **THEN** the label SHALL be worded as a range according to a named guideline (for example "in the range X according to the NHG guideline") and not as a medical condition, and a not-a-diagnosis note and a link to Thuisarts SHALL be reachable from that screen

#### Scenario: No coloured warning verdicts
- **WHEN** a value falls outside the healthy range
- **THEN** the system SHALL NOT present it with alarm wording or imagery suggesting danger or a need for treatment

### Requirement: Claims and marketing stay within the intended purpose
The system and all material about it (README, store listing, screenshots, changelog, release notes) SHALL NOT claim to improve health outcomes, treat, cure, prevent or diagnose any condition, SHALL NOT claim certification, endorsement or approval by NHG, KNMP, Voedingscentrum, the IGJ or any other body, and SHALL NOT use those bodies' logos.

#### Scenario: Wording check
- **WHEN** documentation or a store listing is changed
- **THEN** it SHALL be checked against this requirement, and sources SHALL be named only as the origin of published guidelines

### Requirement: Personal use, no care context
The system SHALL be offered for personal use only, SHALL have no accounts, sharing, caregiver, clinician or practice features, and SHALL NOT integrate with care records, pharmacy systems or national health infrastructure.

#### Scenario: Data leaves the app only through the user
- **WHEN** the user wants to show data to a doctor or pharmacist
- **THEN** the system SHALL only offer the user's own CSV export, with no direct transmission to a care provider

### Requirement: Regulatory review gate for new functions
A change that adds drug information, interaction checks, dose advice, diagnosis or prediction, alerts based on health values, data transmission, or care-provider features SHALL NOT be implemented until a regulatory assessment (MDR intended purpose and AVG) has been recorded in an ADR.

#### Scenario: Gate applied to a proposal
- **WHEN** a proposal contains one of the listed functions
- **THEN** the proposal SHALL be marked blocked until the assessment ADR exists and states the outcome

### Requirement: Applicable rules are documented
The project SHALL document the rules it designs against, and where it relies on an exemption: the MDR (intended purpose, supervised by the IGJ), the AVG and UAVG (no data processed outside the device), Google Play health app and privacy requirements, consumer and product-liability rules if the app is ever sold, and the licence terms of anything it reuses.

#### Scenario: Compliance record exists
- **WHEN** the project is published beyond personal use
- **THEN** an ADR SHALL record the intended purpose, the rules above, the assessment outcome and the date of the last review
