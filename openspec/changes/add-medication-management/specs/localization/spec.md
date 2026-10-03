## ADDED Requirements

### Requirement: Medication text in English and Dutch
Every user-visible medication string (screens, labels, validation messages, statuses, notifications, notification actions, the disclaimer, the Sources list) SHALL exist in English and Dutch, SHALL follow the in-app language, and SHALL be carried as a `UiText` message identifier and translated only when displayed. Medication names, notes and units entered by the user are shown as typed and are never translated.

#### Scenario: Language change re-translates medication screens
- **WHEN** the Medication tab is open and the language changes from English to Dutch
- **THEN** all labels, statuses, forms and units on the screen appear in Dutch and the Medication tab stays selected

#### Scenario: Notifications use the app language
- **WHEN** a reminder is posted while the app language is Dutch and the device language is English
- **THEN** the notification title, body, and the Taken and Snooze actions are in Dutch

#### Scenario: Both languages complete
- **WHEN** the project is tested
- **THEN** a test SHALL fail if a medication string key exists in one language and not in the other

### Requirement: Correct translation of forms and dose units
Medication forms and dose units SHALL be shown as localized labels, never as raw enum names, with correct grammatical number where the unit is a countable noun. International symbols (mg, mcg, g, ml) SHALL be shown identically in both languages. Translating a unit label SHALL NOT change the stored unit or the stored amount, and amounts SHALL NOT be converted.

#### Scenario: Countable units in Dutch
- **WHEN** a dose of 1 tablet and a dose of 2 tablets are shown in Dutch
- **THEN** they read "1 tablet" and "2 tabletten", and a dose of 2 puffs reads "2 pufjes"

#### Scenario: Countable units in English
- **WHEN** a dose of 1 puff and a dose of 2 puffs are shown in English
- **THEN** they read "1 puff" and "2 puffs"

#### Scenario: International symbols unchanged
- **WHEN** a dose of 5 ml or 500 mg is shown in Dutch or English
- **THEN** the symbol is the same in both languages

#### Scenario: International unit label
- **WHEN** a dose of 20 international units is shown
- **THEN** it reads "20 IU" in English and "20 IE" in Dutch, and the stored unit is the same in both cases

#### Scenario: Decimal format follows the region
- **WHEN** a dose of 2.5 ml is shown with a Dutch region
- **THEN** it reads "2,5 ml", and with a US region it reads "2.5 ml", while the stored amount is the same in both cases

#### Scenario: Language change keeps the stored data
- **WHEN** the language changes
- **THEN** stored units, forms and amounts are unchanged and only the displayed labels change

### Requirement: Localized dates, times and schedules
Planned times, weekday names, date ranges, "every N days" and time-of-day groups (morning, afternoon, evening, night) SHALL follow the active language and region, using the existing locale resolution.

#### Scenario: Dutch schedule text
- **WHEN** a medication is scheduled on Monday and Thursday at 08:00 with Dutch language and region
- **THEN** the weekdays read "ma" and "do" (or the full names) and the time reads 08:00 in 24-hour style

#### Scenario: Time of day groups
- **WHEN** the pillbox is shown in Dutch
- **THEN** the groups read Ochtend, Middag, Avond and Nacht

### Requirement: Localized layout robustness
Medication screens SHALL remain usable with long Dutch strings and large font settings, without clipped labels or buttons (ADR 0010).

#### Scenario: Long Dutch labels
- **WHEN** the screen is shown in Dutch with the largest font setting
- **THEN** form, unit and action labels wrap or truncate with ellipsis and stay operable
