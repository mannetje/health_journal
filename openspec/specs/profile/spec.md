# profile Specification

## Purpose
Defines the user profile: the person whose health entries are logged. A profile owns all entries, carries the attributes needed for derived values (height for BMI), and exactly one profile is active at a time.

## Requirements

### Requirement: Profile attributes
A profile SHALL have an identifier (UUID, version 4 or 7), a name, a date of birth, an optional height in whole centimeters, and an optional sex (MALE or FEMALE). An absent sex means "Not set". Sex SHALL NOT influence any classification or calculation. Two profiles are equal if and only if their identifiers are equal.

#### Scenario: Profile created with only mandatory fields
- **WHEN** a profile is created with a name and a valid date of birth and no height or sex
- **THEN** it is stored with height absent and sex absent

### Requirement: Name validation
The profile name SHALL NOT be blank (empty or whitespace only). Creating or reconstructing a profile SHALL trim surrounding whitespace from the name. The UI layer SHALL also trim the name before saving.

#### Scenario: Blank name rejected
- **WHEN** a profile is saved with name "   "
- **THEN** the save is rejected with a validation error and nothing is stored

#### Scenario: Name trimmed
- **WHEN** a profile is created with name "  Anna  "
- **THEN** the stored name is "Anna"

### Requirement: Date of birth validation
The date of birth SHALL be strictly before today (today itself is rejected) and strictly after the date exactly 130 years ago (that exact date is rejected). The UI date picker SHALL offer dates from the year 1900 up to yesterday; today is not selectable, matching the domain rule.

#### Scenario: Birth date today rejected
- **WHEN** the date of birth equals today
- **THEN** the profile is rejected

#### Scenario: Birth date exactly 130 years ago rejected
- **WHEN** the date of birth equals today minus 130 years
- **THEN** the profile is rejected

#### Scenario: Yesterday accepted
- **WHEN** the date of birth is yesterday
- **THEN** the profile is accepted

### Requirement: Height validation
When present, height SHALL be a whole number of centimeters in the inclusive range 50 to 300. A blank height input means "no height" (the stored height is cleared). A non-blank input that is not a whole number SHALL show a validation error and block saving, so a height is never cleared by a typo. Height in the UI MAY be entered in feet and inches (see units-presentation); it SHALL be converted to centimeters only when the user edits it, so an untouched height is never rewritten.

#### Scenario: Height boundaries
- **WHEN** height is 49 or 301
- **THEN** the profile is rejected
- **WHEN** height is 50 or 300
- **THEN** the profile is accepted

#### Scenario: Blank height clears height
- **WHEN** the height field is blank and the profile is saved
- **THEN** the profile is stored without a height

#### Scenario: Non-numeric height rejected
- **WHEN** the height field contains "abc"
- **THEN** an error is shown, saving is disabled and the stored height is unchanged

### Requirement: Active profile
Exactly one profile SHALL be active at a time. Creating a profile SHALL make it active only if no active profile exists yet. Setting a profile active SHALL clear the active flag on all others and set it on the chosen one atomically. All logging and history operate on the active profile. The application SHALL offer a Profile screen as one of three main tabs (Log, History, Profile).

#### Scenario: First profile becomes active
- **WHEN** a profile is created and no active profile exists
- **THEN** it is active

#### Scenario: Second creation does not steal active
- **WHEN** a profile is created while another is active
- **THEN** the existing profile stays active

#### Scenario: Switching active profile
- **WHEN** profile B is made active while A is active
- **THEN** A is not active and B is active

### Requirement: Saving edits the active profile
When the user saves the Profile screen and an active profile exists, the application SHALL update that profile in place (same identifier, same active flag) instead of creating another one. When no active profile exists the application SHALL create a new profile. After a successful save the UI SHALL show a "profile saved" confirmation; validation errors SHALL be shown as messages.

#### Scenario: Update does not add a profile
- **WHEN** an active profile exists and the user changes the name and saves
- **THEN** the number of stored profiles is unchanged and the active profile has the new name

### Requirement: Saving does not recompute history
Changing the height SHALL NOT recompute the BMI stored on existing weight entries (see weight).

#### Scenario: Height change leaves old BMI
- **WHEN** a weight entry with BMI 22.9 exists and the height is changed
- **THEN** that entry still shows BMI 22.9

### Requirement: Persistence contract
Profiles SHALL be stored in a local database table "profiles" with: identifier (string), name, date of birth as ISO date text (YYYY-MM-DD), height in cm (nullable integer), sex (nullable text, enum name), and an active flag. Saving a profile SHALL be an upsert that preserves the existing active flag; it is active on first insert only when no active profile exists.

#### Scenario: Upsert keeps active flag
- **WHEN** an active profile is saved again with changes
- **THEN** it remains active
