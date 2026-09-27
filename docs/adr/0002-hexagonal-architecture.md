# 2. Hexagonal Architecture (Ports and Adapters)

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The health journal needs to model critical domain logic (vital signs, clinical thresholds, aggregate invariant rules) that must remain independent of Android framework lifecycles, database technologies, and UI libraries. Direct coupling of business logic to Android components or SQLite queries impedes unit testing, complicates upgrades, and risks domain corruption.

## Decision

We adopt **Hexagonal Architecture (Ports and Adapters)** organized in a multi-module Gradle layout:

1. **`:domain` (Core Hexagon):** Pure Kotlin JVM library containing Aggregate Roots (`Profile`), Entities, Value Objects (`GlucoseLevel`, `BloodPressureReading`, `WeightKg`), Use Cases, and Port Interfaces. Zero Android SDK or persistence dependencies.
2. **`:data` (Driven / Secondary Adapter):** Implements secondary persistence ports (`ProfileRepositoryPort`, `HealthLogRepositoryPort`) using Jetpack Room SQLite, and data portability ports (`DataExportPort`, `DataImportPort`) using UTF-8 CSV streams.
3. **`:app` (Driving / Primary Adapter):** Presentation layer with Jetpack Compose UI and AndroidX ViewModels that invoke domain use cases.

Dependencies flow strictly inward: `:app` -> `:domain` and `:data` -> `:domain`.

## Consequences

### Positive
- Domain models and clinical validation logic are 100% testable via lightning-fast JVM unit tests.
- Infrastructure (Room, CSV, Android SDK) can be refactored or upgraded without altering business logic.
- Compile-time enforcement of architecture boundaries via Gradle module isolation.

### Negative / Trade-offs
- Requires explicit mapping objects between database entities and domain models.
