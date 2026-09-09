## Purpose

Enforces architectural and documentation governance standards for the health_journal codebase, ensuring strict dependency minimization, architectural decision tracking through ADRs in `docs/adr/`, and continuous maintenance of a living root `README.md` with Mermaid diagrams.

## ADDED Requirements

### Requirement: Dependency minimization
The system SHALL strictly minimize third-party dependencies, prioritizing native Android SDK, official AndroidX/Jetpack libraries (Compose, Room, ViewModel, Lifecycle), and core Kotlin/Kotlinx libraries.

#### Scenario: Third-party library inclusion restricted
- **WHEN** a new dependency is proposed for addition to `libs.versions.toml`
- **THEN** it MUST be evaluated against native/Jetpack alternatives, verified to be reputable and established, and documented in an ADR before inclusion

### Requirement: Architecture Decision Records (ADR)
The repository SHALL maintain an Architecture Decision Record (ADR) log under `docs/adr/` capturing all significant architectural choices.

#### Scenario: Architecture decision recorded
- **WHEN** an architectural or major technical choice is made (e.g. Hexagonal Architecture, persistence engine, library adoption)
- **THEN** an ADR document SHALL be created in `docs/adr/` using sequential numbering and standard sections: Title, Status, Context, Decision, and Consequences

### Requirement: Living README maintenance
The repository root SHALL provide a continuously maintained `README.md` reflecting the actual system state and architecture.

#### Scenario: Living README verified
- **WHEN** the root `README.md` is inspected
- **THEN** it SHALL contain a concise overview of supported health metrics (Weight, BP, Glucose with NHG benchmarks), Mermaid diagrams illustrating Hexagonal Architecture boundaries and data flow, and markdown tables detailing core technologies, module structures, and supported import formats
