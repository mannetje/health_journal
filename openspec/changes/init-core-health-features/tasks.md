## 1. Architectural Governance & Repository Baseline

- [x] 1.1 Establish `docs/adr/` directory and create `0001-record-architecture-decisions.md` documenting the ADR adoption and template format; verify file exists and adheres to Nygard format.
- [x] 1.2 Generate initial living `README.md` documenting app features, Dutch NHG guidelines, Mermaid Hexagonal Architecture diagram, and tables for tech, modules, and import formats; verify markdown renders cleanly.
- [x] 1.3 Remove legacy Eclipse `src/` hierarchy and outdated SQLite files (`src/app/`, `src/db/`); verify with `git status` that legacy artifacts are cleared.

## 2. Gradle Multi-Module Build Setup

- [x] 2.1 Create `gradle/libs.versions.toml` specifying strictly minimized dependencies (Kotlin stdlib, Coroutines, Jetpack Compose BOM, Room, ViewModel/Lifecycle); verify file parses cleanly.
- [x] 2.2 Configure root `settings.gradle.kts` and `build.gradle.kts` declaring `:domain`, `:data`, and `:app` modules; verify with Gradle initialization.
- [x] 2.3 Configure `domain/build.gradle.kts` as a pure Kotlin JVM library with zero Android dependencies; verify build configuration succeeds.
- [x] 2.4 Configure `data/build.gradle.kts` (Android Library with Room) and `app/build.gradle.kts` (Android Application with Compose); verify dependencies point inward to `:domain`.

## 3. Pure Kotlin Domain Module (Hexagonal Core)

- [x] 3.1 Implement core Value Objects (`ProfileId`, `GlucoseLevel`, `MeasurementUnit`, `BloodPressureReading`, `WeightKg`, `HeightCm`) with invariant checks; verify with JVM unit tests in `domain/src/test`.
- [x] 3.2 Implement `Profile` Aggregate Root encapsulating personal attributes and health log invariant validations; verify with unit tests.
- [x] 3.3 Implement Dutch NHG classification domain logic for Blood Pressure, Glucose (fasting & postprandial), and BMI; verify NHG benchmark accuracy with unit tests.
- [x] 3.4 Define Secondary Ports (repository interfaces: `ProfileRepositoryPort`, `HealthLogRepositoryPort`, and `DataExportPort`); verify compilation in `:domain`.
- [x] 3.5 Implement Application Use Cases (`CreateProfileUseCase`, `RecordBloodPressureUseCase`, `RecordGlucoseUseCase`, `RecordWeightUseCase`, `GetHealthHistoryUseCase`); verify each with mock-based unit tests.

## 4. Data Infrastructure Module (Room & CSV Adapters)

- [ ] 4.1 Define Room Database (`HealthJournalDatabase`), database entities, and converters in `:data`; verify schema generation.
- [ ] 4.2 Implement Room DAOs and Repository Adapter classes mapping between Room entities and pure Domain entities; verify with in-memory Room tests.
- [ ] 4.3 Implement CSV export and import adapters supporting UTF-8 formats for all metrics; verify parsing and export output with unit tests.

## 5. Presentation Module (Jetpack Compose UI)

- [ ] 5.1 Implement Material 3 Compose theme, design tokens, and base layout in `:app`; verify with Compose previews.
- [ ] 5.2 Implement Profile setup screen and `ProfileViewModel` communicating via domain use cases; verify state flow with ViewModel tests.
- [ ] 5.3 Implement Metric Logging screens (Blood Pressure, Glucose with mmol/L default, Weight) with instant NHG category feedback; verify UI interactivity.
- [ ] 5.4 Implement Health History screen with filtering and CSV export/import action triggers; verify complete round-trip data flow.
