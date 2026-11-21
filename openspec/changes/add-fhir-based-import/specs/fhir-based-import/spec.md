## ADDED Requirements

### Requirement: Import of a FHIR-based file
The system SHALL offer one import in the History screen that reads a JSON file the person picks through the system document picker. It SHALL read a FHIR `Bundle` written by this app, recognised by the source marker and by the entry identifier system `urn:health-journal:entry` on its Observations, and SHALL also read a `Bundle` from another tool that holds the known readings, applying the same checks to both. It SHALL refuse a file that is not a FHIR `Bundle` in JSON, or from which no reading can be read, with the message that no readings were found. The system SHALL NOT read any file without the person's action and SHALL make no network call.

#### Scenario: Own file accepted
- **WHEN** the person picks a file written by the FHIR-based export of this app
- **THEN** the app shows a preview of what the file contains

#### Scenario: File from another tool accepted
- **WHEN** the person picks a FHIR Bundle without the source marker of this app that holds weight, blood pressure or pulse Observations
- **THEN** the app shows a preview of the readings it can read, with the others counted as skipped

#### Scenario: File without readings refused
- **WHEN** the person picks a file that is not a FHIR Bundle, or holds none of the known readings
- **THEN** the app says that no readings were found and reads nothing into the database

#### Scenario: No network
- **WHEN** the import runs
- **THEN** the app makes no network call and still declares no `INTERNET` permission

### Requirement: Preview and confirmation before anything is saved
The system SHALL show, before saving, how many weight, blood pressure and pulse entries are new, how many already exist and how many are skipped, and SHALL save nothing until the person confirms. After the confirmation it SHALL report how many entries were added and how many were skipped.

#### Scenario: Preview then confirm
- **WHEN** the person picks a valid file and confirms the preview
- **THEN** the new entries are added and the app reports the added and skipped counts

#### Scenario: Cancelled
- **WHEN** the person cancels at the preview
- **THEN** the data is unchanged

### Requirement: Merge without overwriting
The import SHALL add only entries that do not exist yet. For an Observation with an entry identifier an entry exists when that identifier exists. For an Observation without one, an entry exists when the app holds an entry of the same kind with the same time (to the minute, compared as instants) and the same value and unit. An entry that exists SHALL be kept as it is and counted as skipped. The import SHALL run in one database transaction, so a failure leaves the data unchanged.

#### Scenario: Import twice
- **WHEN** the same file, own or from another tool, is imported twice
- **THEN** the second import adds nothing and reports every entry as already present

#### Scenario: Existing entry not overwritten
- **WHEN** an entry with the same identifier exists with a different value
- **THEN** the existing value is kept and the entry is counted as skipped

#### Scenario: Same time, different value
- **WHEN** a reading without an identifier has the same time as an existing entry but another value
- **THEN** it is added as a new entry and the existing entry is unchanged

#### Scenario: Failure halfway
- **WHEN** the file is truncated or a write fails
- **THEN** nothing from the file is imported

### Requirement: Only the three known readings are read, and values are validated
The import SHALL read only Observations with the category `vital-signs`, status `final`, an effective date and time and one of the codes LOINC 29463-7 (weight), 85354-9 (blood pressure) and 8867-4 (pulse), convert values to the storage units (kg, mm[Hg], /min; weight in g is converted to kg), and apply the app's own value validation. An Observation with another code, a missing or unknown unit, a missing field or a value outside the valid range SHALL be skipped and counted, not imported. A pulse Observation SHALL be imported only together with the blood pressure Observation at the same time; otherwise it SHALL be skipped.

#### Scenario: Blood pressure with pulse
- **WHEN** the file holds a blood pressure of 121/81 and a pulse of 68 at the same time
- **THEN** one blood pressure entry with a pulse of 68 is added

#### Scenario: Unknown unit
- **WHEN** a weight Observation has a unit the app does not know
- **THEN** it is skipped and counted

### Requirement: Nothing about the person is imported, and the file is treated as untrusted
The import SHALL NOT write a `Patient`, name, birth date, sex or any identifier from the file into the database. It SHALL refuse a file larger than the size limit or with more entries than the entry limit, SHALL NOT follow references or URLs in the file, and SHALL store the `note` only through the existing comment rules.

#### Scenario: Patient with a name
- **WHEN** a file holds a Patient with a name
- **THEN** no part of the name is stored

#### Scenario: Oversized file
- **WHEN** the file is larger than the limit
- **THEN** it is refused before it is read in full and nothing is imported

### Requirement: The notice and the wording
The preview SHALL show, in English and Dutch, that the file is read as it is, that the person is responsible for the data they import, and that the makers of the app are not responsible for the data or for how the file was made. The app, the README, the store listing and the documentation SHALL call it a "FHIR-based" or "based on FHIR" import and SHALL NOT claim that it works with, connects to or is compatible with any care system, EHR system, GP system or personal health environment.

#### Scenario: Notice shown
- **WHEN** the preview is shown
- **THEN** the notice is visible in the language of the app

#### Scenario: Document scan
- **WHEN** the repository documents and string resources are scanned
- **THEN** none contains a claim of conformance, certification, compatibility or interoperability for the import
