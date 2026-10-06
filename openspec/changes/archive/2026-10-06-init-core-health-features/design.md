## Context

The existing project is an Eclipse-era Android 4.0 Java stub with a single Activity, no architecture, and a raw SQLite schema (`create_db.sql`). See `proposal.md → Why` for motivation. There is no existing Kotlin code, no Gradle build, and no tests. This is a greenfield implementation constrained by the requirement to produce an offline-first Android application with a cleanly isolated domain layer.

Furthermore, engineering governance mandates strict dependency minimization, Architecture Decision Records (ADRs), a living `README.md` with Mermaid diagrams, and GitHub CLI (`gh`) integration.

## Goals / Non-Goals

**Goals:**
- Enforce Hexagonal Architecture (Ports and Adapters) via a Gradle multi-module layout where `:domain` has zero Android or Room dependencies.
- Model all health metrics (weight, blood pressure, glucose, activity) as pure Kotlin domain types with validated Value Objects.
- Implement Dutch NHG classification logic entirely within the domain module, testable with standard JVM unit tests.
- Strictly minimize external dependencies, sticking to native Android SDK, official Jetpack libraries, and official Kotlinx libraries.
- Maintain a complete log of architectural decisions in `docs/adr/`.
- Maintain a living root `README.md` containing Mermaid architectural diagrams and technology/module tables.
- Provide Room-backed persistence adapters in `:data` satisfying domain repository ports.
- Provide a Jetpack Compose UI in `:app` driving use cases via ViewModels.
- Support CSV export/import as a secondary adapter in `:data`.

**Non-Goals:**
- Network synchronisation or cloud backup (Phase 1 is local-only).
- Multi-user / account management (one Profile per installation for now).
- Heavy third-party analytics, crash reporting, or proprietary SDKs.
- Wear OS, Android TV, or tablet-specific layouts in Phase 1.

## Decisions

### 1. Gradle multi-module layout with Kotlin DSL
**Decision**: Three core modules — `:domain`, `:data`, `:app` — with a centralized `gradle/libs.versions.toml` version catalog.

```
health_journal/
├── app/                  # Presentation adapter (Compose, ViewModel)
├── data/                 # Infrastructure adapter (Room, CSV, Mapper)
├── domain/               # Pure Kotlin business logic (Ports, Entities, Use Cases)
├── docs/adr/             # Architecture Decision Records
├── build.gradle.kts      # Root build script
├── settings.gradle.kts   # Module settings
└── gradle/libs.versions.toml
```

**Why**: Enforces compile-time isolation. Gradle prevents `:domain` from importing any Android or Room classes.

### 2. Hexagonal Architecture & Dependency Rule
**Decision**: Dependencies flow strictly inward toward `:domain`:
```
:app  ──► :domain (Driving / Primary Adapter)
:data ──► :domain (Driven / Secondary Adapter)
```
`:domain` exposes ports (`interface`). `:data` implements persistence ports. `:app` invokes use cases.

### 3. Dependency Minimization Policy
**Decision**: Rely exclusively on first-party solutions:
- UI: Jetpack Compose (BOM)
- Persistence: Jetpack Room SQLite
- Concurrency: Kotlinx Coroutines & Flow
- Serialization: Kotlinx Serialization
- Architecture: AndroidX Lifecycle & ViewModel
- Dependency Injection: Manual constructor injection / lightweight factory or standard Hilt if strictly justified.
- Third-party policy: No arbitrary third-party libraries. Any external library must be approved, reputable, and documented in `docs/adr/`.

### 4. Architecture Decision Records (ADR)
**Decision**: Establish `docs/adr/` using the Michael Nygard template format:
- `0001-record-architecture-decisions.md` (Process bootstrap)
- `0002-hexagonal-architecture.md`
- `0003-dependency-minimization.md`
- `0004-room-for-offline-first-persistence.md`
- `0005-dutch-nhg-guidelines.md`

### 5. Living README & Visual Documentation
**Decision**: Maintain root `README.md` as the living technical documentation containing:
- High-level overview and features (Weight, BP, Glucose, Activity, NHG benchmarks).
- Mermaid diagram illustrating Hexagonal layers, ports, and data flow.
- Structured Markdown tables for:
  1. Core Technologies
  2. Module Breakdown
  3. Supported Import/Export Formats

### 6. Domain Modelling & Dutch NHG Standards
**Decision**:
- Value Objects: `ProfileId`, `GlucoseLevel`, `BloodPressureReading`, `WeightKg`, `HeightCm`.
- Units: Canonical glucose is `mmol/L` (NHG standard), with `fromMgDl()` converter.
- Evaluations: NHG reference ranges hard-coded as pure domain logic for instant, offline, deterministic classification.

### 7. Developer Tooling & Git Workflow
**Decision**: Use GitHub CLI (`gh`) for managing Git branches, pull requests, and repository releases.

## Risks / Trade-offs

- **Manual DI vs Heavy Framework** → By minimizing dependencies, avoiding complex reflection/codegen libraries keeps build times fast and binary sizes small.
- **BigDecimal for calculations** → Accurate rounding for NHG cutoffs without IEEE 754 floating-point inaccuracies.
- **Strict module isolation** → Requires writing mappers between Room entities and domain entities in `:data`. This slight boilerplate delivers total testability and independence from schema migrations.

## Migration Plan

1. Remove legacy Eclipse `src/` hierarchy.
2. Initialize root `README.md` with Mermaid diagram and technical tables.
3. Establish `docs/adr/` with initial ADRs.
4. Setup Gradle multi-module build with version catalog (`libs.versions.toml`).
5. Implement `:domain` module (Value Objects, Entities, Ports, Use Cases) with JVM unit tests.
6. Implement `:data` module (Room DB, DAOs, CSV adapters) with in-memory SQLite tests.
7. Implement `:app` module (Compose screens and ViewModels).
