# 2. Hexagonal Architecture (Ports and Adapters)

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The health journal needs to model critical domain logic (vital signs, clinical thresholds, aggregate invariant rules) that must remain independent of Android framework lifecycles, database technologies, and UI libraries. Direct coupling of business logic to Android components or SQLite queries impedes unit testing, complicates upgrades, and risks domain corruption.

## Decision

We adopt **Hexagonal Architecture (Ports and Adapters)** organized in a multi-module Gradle layout:

1. **`:domain` (Core Hexagon):** Pure Kotlin JVM library containing Aggregate Roots (`Profile`), Entities, Value Objects (`GlucoseLevel`, `BloodPressureReading`, `WeightKg`), command and query handlers, and Port Interfaces. Zero Android SDK or persistence dependencies.
2. **`:data` (Driven / Secondary Adapter):** Implements secondary persistence ports (`ProfileRepositoryPort` and one repository port per metric) using Jetpack Room SQLite, and data portability ports (`DataExportPort`, `DataImportPort`) using UTF-8 CSV streams.
3. **`:app` (Driving / Primary Adapter):** Presentation layer with Jetpack Compose UI and AndroidX ViewModels that call the domain through the `Dispatcher`.

Dependencies flow strictly inward: `:app` -> `:domain` and `:data` -> `:domain`. The composition root lives in `:app` (Hilt modules, [ADR 0035](0035-hilt-for-dependency-injection.md)); `:domain` and `:data` carry no DI annotations.

### Primary ports

The driving side of the hexagon is the application boundary of [ADR 0036](0036-application-boundary-commands-and-queries.md): a ViewModel sends a typed command or query to the `Dispatcher` and gets a `DomainResult` back ([ADR 0037](0037-typed-domain-errors-and-single-source-for-rules.md)). It never touches a secondary port. Use cases with real logic remain classes that a handler wraps. From Clean Architecture only the *use case* idea is borrowed; presenters and per-use-case response models are not adopted, because the ViewModel maps the typed result itself. The domain is split into bounded contexts with fixed import rules ([ADR 0038](0038-bounded-contexts-and-tactical-roles.md)).

## Consequences

### Positive
- Domain models and clinical validation logic are 100% testable via lightning-fast JVM unit tests.
- Infrastructure (Room, CSV, Android SDK) can be refactored or upgraded without altering business logic.
- Compile-time enforcement of architecture boundaries via Gradle module isolation.

### Negative / Trade-offs
- Requires explicit mapping objects between database entities and domain models.
