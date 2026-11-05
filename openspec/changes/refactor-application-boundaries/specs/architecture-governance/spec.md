## MODIFIED Requirements

### Requirement: Layered structure
The system SHALL separate a domain layer (entities, value objects, validation, classification, use cases, command and query handlers) from adapters. The domain layer SHALL NOT depend on any UI, persistence or platform framework. The domain defines primary ports (commands and queries with their handlers) and secondary ports (one repository port per health metric, plus the migraine, medication and profile repository ports, DataExportPort, DataImportPort and the platform ports) that the persistence, CSV and UI adapters implement or call. Business rules live only in the domain layer; the UI layer only converts units and formats text.

#### Scenario: Domain independence
- **WHEN** the domain layer's dependencies are inspected
- **THEN** it contains no reference to UI, database or platform libraries

#### Scenario: Narrow secondary ports
- **WHEN** the repository ports for health metrics are inspected
- **THEN** each metric has its own port, and no single port covers more than one metric

## ADDED Requirements

### Requirement: Application boundary
Driving adapters (screens and their ViewModels) SHALL reach the domain only through commands and queries sent to a dispatcher, and SHALL NOT use secondary ports directly. Handlers SHALL report expected failures, such as invalid input or an unknown entry, as typed errors that carry a code and parameters and no display text, and the UI SHALL turn each code into a localized message in English and Dutch. A range or rule SHALL be defined once in the domain; the UI SHALL NOT repeat it. Handlers SHALL read the current time from a clock, not from the system directly. Automated tests SHALL fail when a ViewModel imports a secondary port, when a bundle of use cases is introduced, or when a handler returns anything other than a typed result.

#### Scenario: ViewModel and ports
- **WHEN** the source of any ViewModel is inspected
- **THEN** it imports no secondary port and calls the domain only through the dispatcher

#### Scenario: Specific validation message
- **WHEN** a diastolic value that is not below the systolic value is saved
- **THEN** the save is refused with a typed error and the user sees the existing message for that rule in the chosen language, and nothing is stored

#### Scenario: Unknown entry
- **WHEN** a delete or update names an entry that no longer exists
- **THEN** the handler returns a not-found error and nothing changes

#### Scenario: Missing string
- **WHEN** a validation rule has no message in English or Dutch
- **THEN** a unit test fails and names the rule
