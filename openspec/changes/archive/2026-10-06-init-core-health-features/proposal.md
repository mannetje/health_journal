## Why

The existing health_journal repository is an Eclipse-era Android 4.0 Java stub (a single `MainActivity.java` and a raw SQLite schema) that has not been maintained. It cannot be extended to support modern offline-first use cases, Dutch NHG medical benchmarks, or a testable architecture. We need to replace it with a production-quality Kotlin application structured around Hexagonal Architecture (Ports and Adapters) to allow the domain logic, persistence, and UI to evolve independently.

Furthermore, the project requires rigorous engineering governance from the beginning: strict dependency minimization (relying on first-party Android/Jetpack/Kotlin libraries), Architecture Decision Records (ADRs) to capture all significant architectural and technical choices, and a continuously maintained living `README.md` documenting architecture, modules, and workflows.

## What Changes

- **BREAKING**: Retire the `src/` Eclipse project layout entirely; the new project root is a Gradle multi-module build.
- **Hexagonal Architecture (Ports and Adapters)**:
  - Pure Kotlin `domain` module containing Aggregate Roots, Entities, Value Objects, Domain Events, and Use Case ports. Zero `android.*` or `androidx.*` dependencies.
  - `data` infrastructure module implementing Room-backed repository adapters and CSV import/export adapters.
  - `app` presentation module using Jetpack Compose, ViewModels, and dependency injection.
- **Dependency Minimization**:
  - Strictly limit third-party libraries. Rely exclusively on native Android SDK, official Jetpack libraries (Compose BOM, Room, ViewModel, Lifecycle), and core Kotlin/Kotlinx libraries (Coroutines, Serialization, DateTime) first.
  - Third-party open-source libraries are permitted only if strictly necessary and must be reputable, established, and documented in an ADR.
- **Architecture Decision Records (ADR)**:
  - Establish a `docs/adr/` directory following standard ADR markdown templates (Title, Status, Context, Decision, Consequences).
  - Document every major architectural choice (Hexagonal Architecture, Room, Dependency Minimization, etc.).
- **Living README**:
  - Maintain the root `README.md` with concise functionality descriptions, Mermaid diagrams showing Hexagonal boundaries and data flow, and structured markdown tables for core tech, module layout, and import formats.
- **Core Health Metrics & Dutch NHG Benchmarks**:
  - Model **weight**, **blood pressure** (systolic/diastolic), **blood glucose** (primary unit mmol/L, with mg/dL conversion), and **activity/GPS sessions**.
  - Default all metric evaluations to Dutch NHG (Nederlands Huisartsen Genootschap) medical guidelines.
- **Offline-First Persistence**:
  - All operations persist locally to Room SQLite; no external network or cloud required in Phase 1.
- **Tooling & Workflows**:
  - Standardize on GitHub CLI (`gh`) for repository operations, pull requests, and releases.

## Capabilities

### New Capabilities

- `profile`: User profile management — create and read a Profile (name, date-of-birth, optional height); profiles serve as the aggregate root owning all health log entries.
- `health-metrics/weight`: Record and retrieve body weight measurements in kg, with BMI calculation derived from profile height and NHG/WHO classifications.
- `health-metrics/blood-pressure`: Record systolic/diastolic blood pressure readings in mmHg, classified against Dutch NHG guideline thresholds.
- `health-metrics/glucose`: Record blood glucose levels in mmol/L (primary) or mg/dL (converted), classified against Dutch NHG fasting/postprandial reference ranges.
- `health-metrics/activity`: Record activity sessions (start time, end time, distance in metres) and compute durations.
- `data-export`: Export and import metric history in UTF-8 CSV format.
- `architecture-governance`: Enforce ADR tracking in `docs/adr/` and continuous maintenance of the living root `README.md` with Mermaid architecture diagrams.

### Modified Capabilities

*(None — no existing specs exist; this is a greenfield rewrite.)*

## Impact

- **Replaces**: `src/app/` (Eclipse Android project), `src/db/create_db.sql`, `src/db/model.db`.
- **New build system**: Gradle Kotlin DSL (`settings.gradle.kts`, `build.gradle.kts` per module, `libs.versions.toml`).
- **Dependencies**: Minimal dependency footprint restricted to Kotlin stdlib, Kotlinx Coroutines, Jetpack Compose BOM, Jetpack Room, and AndroidX ViewModel/Lifecycle.
- **Documentation**: New `docs/adr/` directory with initial ADR `0001-record-architecture-decisions.md`; living `README.md`.
- **Tooling**: GitHub CLI (`gh`) utilized for version control and PR flows.
