# localization Specification

## Purpose
Defines language and region handling: which languages exist, how the active locale is chosen, and how user-visible text (including error and status messages) is produced. Language and region are independent: language controls text, region controls date/number formats and default units.

## Requirements

### Requirement: Supported languages
The application SHALL ship English (en) and Dutch (nl) texts. Every user-visible string SHALL exist in both languages. Clinical category labels and glucose context labels SHALL be localized texts, never raw enum names. If the device language is neither, English SHALL be used.

#### Scenario: Unsupported device language
- **WHEN** the device language is French and the language setting is System default
- **THEN** English text is shown

### Requirement: Language and region choices
The user SHALL be able to choose a language (System default, English, Dutch) and a region (System default, Netherlands (NL), United States (US)). Both choices are persisted per device and default to System default.

#### Scenario: Persisted choice
- **WHEN** the user picks Dutch and restarts the app
- **THEN** the UI is Dutch

### Requirement: Locale resolution
The active locale SHALL combine a language part and a region part. The language part comes from the language choice, or from the device language when the choice is System default. The region part comes from the region choice, or from the device region when System default. When both choices are System default the device locale is used unchanged. The region determines date and number formatting and the System default unit choices (see units-presentation).

#### Scenario: Dutch text with US formats
- **WHEN** language is Dutch and region is US
- **THEN** texts are Dutch while numbers and dates use US formats and imperial units and mg/dL are the unit defaults

### Requirement: Applying a change
Changing language or region SHALL rebuild the visible UI immediately and keep the selected main tab. Formatting of dates and numbers throughout the app SHALL follow the active locale.

#### Scenario: Tab preserved
- **WHEN** the user changes language on the Profile tab
- **THEN** the Profile tab remains selected in the new language

### Requirement: Date and time display
Timestamps SHALL be stored as UTC instants and displayed in the device time zone using the active region's medium date style and short time style.

#### Scenario: Time zone display
- **WHEN** an entry at 08:00Z is shown on a device in UTC+2
- **THEN** the time reads 10:00

### Requirement: Localized messages from the presentation logic
Status and validation messages produced by presentation logic SHALL be carried as a message identifier with arguments (arguments MAY themselves be message identifiers) or as already final text, and SHALL be translated only when displayed. Presentation state SHALL never hold translated text, so a language change re-translates existing messages. Error messages originating from domain validation are English and are shown as plain text; they are not translated.

#### Scenario: Language change re-translates
- **WHEN** a "saved" confirmation is on screen and the language changes
- **THEN** the confirmation appears in the new language

#### Scenario: Domain error text
- **WHEN** a domain rule rejects a value
- **THEN** its English message is shown unchanged in both languages

### Requirement: Medication text in English and Dutch
Every user-visible medication string (screens, labels, validation messages, statuses, the disclaimer, the Sources list) SHALL exist in English and Dutch, SHALL follow the in-app language, and SHALL be carried as a `UiText` message identifier and translated only when displayed. Medication names, notes and units entered by the user are shown as typed and are never translated.

#### Scenario: Language change re-translates medication screens
- **WHEN** the pillbox screen is open and the language changes from English to Dutch
- **THEN** all labels, statuses, forms and units on the screen appear in Dutch and the pillbox screen stays open

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

#### Scenario: Microgram written in full in Dutch
- **WHEN** a strength or dose in micrograms is shown in Dutch
- **THEN** the unit reads "microgram" in full and reads "mcg" in English, and the stored unit is the same in both cases

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

### Requirement: Notifications use the app language
Notification titles, bodies and action labels for medication reminders SHALL be in the in-app language, not the device language.

#### Scenario: English device, Dutch app
- **WHEN** a reminder is posted while the app language is Dutch and the device language is English
- **THEN** the notification title, body, and the Taken all and Snooze actions are in Dutch

#### Scenario: Both languages complete
- **WHEN** the project is tested
- **THEN** a test SHALL fail if a reminder string key exists in one language and not in the other
