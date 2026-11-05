# architecture-governance Specification

## Purpose
Enforces architectural and documentation governance for the health_journal project: layered (hexagonal) structure, dependency minimization, decision records in `docs/adr/` and a living README.

## Requirements

### Requirement: Layered structure
The system SHALL separate a domain layer (entities, value objects, validation, classification, use cases, command and query handlers) from adapters. The domain layer SHALL NOT depend on any UI, persistence or platform framework. The domain defines primary ports (commands and queries with their handlers) and secondary ports (one repository port per health metric, plus the migraine, medication and profile repository ports, DataExportPort, DataImportPort and the platform ports) that the persistence, CSV and UI adapters implement or call. Business rules live only in the domain layer; the UI layer only converts units and formats text.

#### Scenario: Domain independence
- **WHEN** the domain layer's dependencies are inspected
- **THEN** it contains no reference to UI, database or platform libraries

#### Scenario: Narrow secondary ports
- **WHEN** the repository ports for health metrics are inspected
- **THEN** each metric has its own port, and no single port covers more than one metric

### Requirement: Dependency minimization
The system SHALL minimize third-party dependencies, preferring the platform SDK and its official libraries. Any new dependency SHALL be evaluated against those alternatives and documented in an ADR before inclusion.

#### Scenario: Third-party library proposed
- **WHEN** a new dependency is proposed
- **THEN** it is evaluated against platform alternatives, verified as reputable and documented in an ADR before inclusion

### Requirement: Architecture Decision Records
The repository SHALL keep an ADR log under `docs/adr/` with sequential numbering and the sections Title, Status, Context, Decision and Consequences. Platform-specific choices (persistence engine, UI toolkit, build tooling, icon layers) are recorded in ADRs, not in the specs.

#### Scenario: Decision recorded
- **WHEN** a significant technical choice is made
- **THEN** a new ADR is created in `docs/adr/`

### Requirement: Specs are the platform-neutral source of truth
Everything under `openspec/specs/` SHALL describe business rules in platform-neutral terms so the app can be rebuilt on another platform. Changes to behavior SHALL update the relevant spec, and `openspec/specs/README.md` SHALL index all specs.

#### Scenario: Behavior change
- **WHEN** a business rule changes
- **THEN** the corresponding spec is updated in the same change

### Requirement: Living README
The repository root `README.md` SHALL reflect the actual system: supported metrics with NHG benchmarks, Mermaid diagrams of the architecture boundaries and data flow, and tables of technologies, modules and supported import formats.

#### Scenario: README inspected
- **WHEN** the root README is reviewed
- **THEN** it contains the overview, Mermaid diagrams and tables described above

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

### Requirement: Bounded contexts
The domain layer SHALL be divided into named contexts (profile, metrics, medication, migraine, mindfulness, backup and lock) with a shared kernel for concepts used by several of them. Dependencies between contexts SHALL follow a documented context map: the shared kernel and the profile context are upstream, the metrics, medication and migraine contexts SHALL NOT depend on each other, and only the backup context MAY depend on all of them. An automated test SHALL fail when a model imports across a forbidden edge and SHALL name the file and the edge. New ports, commands and handlers SHALL live in their context's package.

#### Scenario: Forbidden context dependency
- **WHEN** a medication model imports a type from the migraine context
- **THEN** the context test fails and names the file and the forbidden edge

#### Scenario: Shared concept
- **WHEN** a concept is used by more than one context
- **THEN** it lives in the shared kernel and not in one of the contexts that use it
