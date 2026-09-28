## ADDED Requirements

### Requirement: Support Dutch and English UI language
The system SHALL present all user-facing text in Dutch or English, following the device's system language setting by default, and falling back to English when the device language is neither Dutch nor English.

#### Scenario: Device language is English
- **WHEN** the device's system language is English and no language override is set
- **THEN** the app SHALL display all screens (labels, buttons, dialogs, category names) in English

#### Scenario: Device language is Dutch
- **WHEN** the device's system language is Dutch and no language override is set
- **THEN** the app SHALL display all screens in Dutch

#### Scenario: Device language is neither Dutch nor English
- **WHEN** the device's system language is neither Dutch nor English and no language override is set
- **THEN** the app SHALL fall back to displaying all screens in English

### Requirement: Allow the user to override the app language from Profile settings
The system SHALL let the user choose the app's display language from a "System default" / "English" / "Nederlands" setting in the Profile screen, independent of the device's system language, and SHALL apply that choice immediately across the whole app.

#### Scenario: User selects English regardless of device language
- **WHEN** the user selects "English" in the Profile language setting
- **THEN** the app SHALL display all screens in English until the setting is changed again, even if the device's system language is Dutch or something else

#### Scenario: User selects Nederlands regardless of device language
- **WHEN** the user selects "Nederlands" in the Profile language setting
- **THEN** the app SHALL display all screens in Dutch until the setting is changed again, even if the device's system language is English or something else

#### Scenario: User selects System default
- **WHEN** the user selects "System default" in the Profile language setting
- **THEN** the app SHALL follow the device's system language (with the English fallback described above) instead of a fixed override

#### Scenario: Language override persists across app restarts
- **WHEN** the user has selected a language override and later closes and reopens the app
- **THEN** the app SHALL still display in the previously selected language until the user changes the setting again

### Requirement: Localize clinical category labels
The system SHALL display NHG-derived category names (BMI, blood pressure, glucose categories) and glucose measurement context (fasting/postprandial) as localized text matching the active app language, not as raw enum identifiers.

#### Scenario: Category label matches active language
- **WHEN** a reading is classified into an NHG category and shown on any screen
- **THEN** the displayed category name SHALL be in the app's active language (Dutch or English)
