# 4. Room SQLite for Offline-First Persistence

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The application is strictly offline-first and requires a reliable, type-safe local storage solution capable of executing queries, maintaining relational integrity (profiles to health entries), and streaming reactive updates to the UI layer via Kotlin Coroutine Flows.

## Decision

We use **Jetpack Room** on top of SQLite in the `:data` infrastructure module:

1. **Entity Encapsulation:** Room entities (`ProfileEntity`, `WeightEntity`, `BloodPressureEntity`, `GlucoseEntity`, `ActivityEntity`) are confined to `:data` and never leak into `:domain`.
2. **DAOs & Reactive Streams:** DAOs provide suspend functions for writes/queries and `Flow<List<T>>` for reactive observation.
3. **Mappers:** Dedicated mappers translate Room entities to pure domain models with validated value objects.

## Consequences

### Positive
- Compile-time verification of SQL queries.
- First-class Kotlin Coroutines and Flow integration.
- Full offline persistence without network dependencies.

### Negative / Trade-offs
- Requires boilerplate mapping between database entities and domain entities.

## Update (2026-09-29)

Room was upgraded from 2.6.1 to 2.8.5 so its annotation processor works with KSP2 (Kotlin/KSP 2.3.0). No entity, DAO or schema changes were needed. See [ADR 0007](0007-build-toolchain-upgrade.md).
