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
