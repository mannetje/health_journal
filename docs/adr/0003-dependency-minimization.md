# 3. Dependency Minimization Policy

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Excessive third-party dependencies inflate binary size, degrade build performance, introduce security vulnerabilities, and increase maintenance overhead through breaking upstream API changes.

## Decision

We enforce a strict **Dependency Minimization Policy**:

1. **First-Party Priority:** Rely exclusively on the native Android SDK, official AndroidX/Jetpack libraries (Compose BOM, Room, Lifecycle/ViewModel), and official Kotlin/Kotlinx libraries (Coroutines, Serialization).
2. **Third-Party Restriction:** No arbitrary third-party libraries may be introduced. Any proposed third-party library must be evaluated against standard Jetpack/Kotlin solutions, verified for stability and reputation, and approved via a dedicated ADR.
3. **Central Version Catalog:** All dependencies and plugins are managed exclusively in `gradle/libs.versions.toml`.

## Consequences

### Positive
- Predictable and fast build times.
- Small APK footprint and minimized attack surface.
- Long-term maintainability and seamless platform upgrades.

### Negative / Trade-offs
- Certain utility features (e.g., lightweight CSV parsing/generation) are implemented directly rather than importing large third-party libraries.
