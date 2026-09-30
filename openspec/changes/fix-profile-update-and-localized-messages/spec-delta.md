## ADDED Requirements

### Requirement: Update profile
The system SHALL let the user change the name, date of birth, height and sex of the active profile without creating another profile.

#### Scenario: Edit keeps identity
- **WHEN** the user changes the height of the existing profile from 178 to 180 cm and saves
- **THEN** the active profile SHALL keep its identifier, SHALL show 180 cm, and the number of stored profiles SHALL be unchanged

#### Scenario: Entries stay attached
- **WHEN** the profile is edited after entries were logged
- **THEN** all earlier entries SHALL still belong to the profile

#### Scenario: Invalid edit
- **WHEN** the user saves a blank name or an out-of-range height
- **THEN** the system SHALL show an error and SHALL NOT change the stored profile

### Requirement: Localized messages
The system SHALL show every success, info and error message in the app language (English or Dutch), including messages that contain a unit or a number.

#### Scenario: Dutch banner
- **WHEN** the app language is Dutch and the user saves an activity
- **THEN** the banner SHALL be shown in Dutch

#### Scenario: Language change
- **WHEN** the language is changed while a message is visible
- **THEN** the message SHALL be shown in the new language, because the state holds a resource id and not translated text

#### Scenario: Unit in message
- **WHEN** Imperial is selected and the weight is missing
- **THEN** the error SHALL name the unit in use ("lb") in the app language
