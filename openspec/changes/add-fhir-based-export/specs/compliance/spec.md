## ADDED Requirements

### Requirement: The FHIR-based export makes no claim of use in care
The FHIR-based export SHALL be described only as a copy of the person's own entries in a format based on the Dutch zib and FHIR specifications, for their own use at their own risk. The system and its documents SHALL NOT state or imply that the export is for healthcare professionals, is part of care, is accepted by, compatible with or interoperable with any care, EHR, GP or personal health environment system, or conforms to or is certified under any standard.

#### Scenario: Wording in the app and documents
- **WHEN** the export, the README, the store listing or the documentation describes the export
- **THEN** it uses "based on FHIR" and the own-risk notice and makes none of the claims above

#### Scenario: No interpretation travels with the data
- **WHEN** an entry is exported
- **THEN** the file contains no assessment, range or advice about the value
