## ADDED Requirements

### Requirement: A FHIR-based export of the person's own entries
The system SHALL offer one export, in the History export dialog, that writes the person's weight, blood pressure and pulse entries as a single FHIR R4 `Bundle` of type `collection` in a JSON file. The file SHALL be created only after the person's action and saved or shared only to a destination the person chooses through the system document picker. The system SHALL NOT send the file or any data anywhere itself.

#### Scenario: Export is user initiated
- **WHEN** the person taps the export button
- **THEN** the system lets them choose where to save the file, and writes it there, and does nothing else with it

#### Scenario: No network
- **WHEN** the export runs
- **THEN** the app makes no network call and still declares no `INTERNET` permission

### Requirement: Each reading declares the profile it is shaped by
Each exported reading SHALL be an `Observation` with the category `vital-signs`, status `final`, a subject reference, an effective date and time with the offset of the person's zone, and `meta.profile` naming the Nictiz profile it follows (`zib-BodyWeight`, `zib-BloodPressure` or `zib-HeartRate`). Weight SHALL be in `kg`, blood pressure in `mm[Hg]` with a systolic and a diastolic component, and pulse in `/min`, whatever the person's display units. The entry's comment SHALL be written as the `note`. The file SHALL state the version of the profile package it was built against.

#### Scenario: Blood pressure
- **WHEN** a blood pressure of 121/81 with a pulse of 68 is exported
- **THEN** the bundle holds a blood pressure Observation with systolic 121 and diastolic 81 mm[Hg] and a separate heart rate Observation of 68 /min at the same time

#### Scenario: Display units do not change the file
- **WHEN** the person's units are pounds and the weight is 75.6 kg
- **THEN** the exported value is 75.6 kg

### Requirement: Nothing is interpreted and nothing identifies the person by default
The export SHALL NOT write an `interpretation`, a reference range, a flag or any other assessment. It SHALL NOT write a national identifier or the person's name. Birth date and sex SHALL be written only if the person ticks them in the export dialog.

#### Scenario: Default file
- **WHEN** the person exports without ticking anything
- **THEN** the `Patient` holds a generated id only, and no Observation holds an interpretation or reference range

#### Scenario: Opted-in details
- **WHEN** the person ticks birth date and sex
- **THEN** only those two values are added to the `Patient`

### Requirement: The notice and the wording
The export dialog SHALL show, in English and Dutch, that the file is based on the Dutch zib and FHIR formats, has not been checked or approved by anyone, is not for use in care, is not encrypted, and is used at the person's own risk. The app, the README, the store listing and the documentation SHALL call it a "FHIR-based" or "based on FHIR" export and SHALL NOT claim that it conforms to, is certified for, or works with any care system, EHR system, GP system or personal health environment.

#### Scenario: Notice shown
- **WHEN** the person opens the export dialog
- **THEN** the notice is visible above the export button in the language of the app

#### Scenario: Document scan
- **WHEN** the repository documents and string resources are scanned
- **THEN** none contains a claim of conformance, certification, compatibility or interoperability for the FHIR-based export

### Requirement: The output is checked against the profiles
The change SHALL include a test that produces sample bundles and a documented local run of the official HL7 validator against the Nictiz package named in the file, with the result and package version recorded. The profile package and the validator SHALL NOT be bundled in the app.

#### Scenario: Validator run
- **WHEN** the documented validation is run on the sample bundles
- **THEN** it reports no errors, or each reported error is listed in `docs/dev/testing.md` with the reason it remains
