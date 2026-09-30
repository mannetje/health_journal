# architecture-governance Specification

## Purpose
Enforces architectural and documentation governance for the health_journal project: layered (hexagonal) structure, dependency minimization, decision records in `docs/adr/` and a living README.

## Requirements

### Requirement: Layered structure
The system SHALL separate a domain layer (entities, value objects, validation, classification, use cases) from adapters. The domain layer SHALL NOT depend on any UI, persistence or platform framework. The domain defines primary ports (use cases) and secondary ports (HealthLogRepositoryPort, ProfileRepositoryPort, DataExportPort, DataImportPort) that the persistence, CSV and UI adapters implement or call. Business rules live only in the domain layer; the UI layer only converts units and formats text.

#### Scenario: Domain independence
- **WHEN** the domain layer's dependencies are inspected
- **THEN** it contains no reference to UI, database or platform libraries

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
