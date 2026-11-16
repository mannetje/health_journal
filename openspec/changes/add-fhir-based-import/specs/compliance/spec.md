## ADDED Requirements

### Requirement: The FHIR-based import makes no claim of use in care
The FHIR-based import SHALL be described only as a way to read back a file this app wrote, for the person's own use at their own risk. The system and its documents SHALL NOT state or imply that the import reads records from care providers, is for healthcare professionals, is part of care, is accepted by, compatible with or interoperable with any care, EHR, GP or personal health environment system, or conforms to or is certified under any standard.

#### Scenario: Wording in the app and documents
- **WHEN** the import, the README, the store listing or the documentation describes the import
- **THEN** it says the app reads back its own "based on FHIR" file and makes none of the claims above

#### Scenario: No interpretation is added
- **WHEN** an entry is imported
- **THEN** the app adds no assessment, range or advice to it
