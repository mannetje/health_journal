# 7. Build Toolchain Upgrade (Kotlin 2.3.0, KSP 2.3.0, Room 2.8.5)

- **Date:** 2026-09-29
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Adding Vico ([ADR 0006](0006-health-trend-visualizations.md)) pulled in artifacts compiled with a newer Kotlin than the project's compiler, so `:app:compileDebugKotlin` failed with a Kotlin binary-metadata-version incompatibility. Fixing that surfaced a chain of further failures, each hidden behind the previous one.

## Decision

All versions stay centralized in `gradle/libs.versions.toml` ([ADR 0003](0003-dependency-minimization.md)):

1. **Kotlin and KSP `2.2.x` → `2.3.0`.** The compiler must be at least as new as the Kotlin used to build Vico's artifacts. KSP is versioned in lockstep with Kotlin, and KSP 2.3.0 runs the KSP2 engine.
2. **Room `2.6.1` → `2.8.5`.** Room 2.6.1's annotation processor crashes under KSP2 with `unexpected jvm signature V`. Room 2.8.x supports KSP2. The bump is a minor version and needs no schema changes. See [ADR 0004](0004-room-for-offline-first-persistence.md).
3. **`kotlinOptions { jvmTarget }` → `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }`** in `:app` and `:data` (`:domain` already used the new form). Kotlin 2.3.0 turned the old DSL into a hard error.
4. **Chart code:** in `ChartPrimitives.kt`, Vico 3 passes the axis formatter's x-value as a `Double`, while the x→`Instant` map is keyed by `Float`. The lookup is now `get(x.toFloat())`, because Kotlin 2.3.0 can no longer infer the map key type from a mismatched argument.

5. **Gradle wrapper `8.10.2` → `8.13`** (`gradle/wrapper/gradle-wrapper.properties`), required by Android Gradle Plugin 8.13.2.

## Consequences

### Positive
- Vico and Room build together under one KSP2-based toolchain.
- No deprecated build DSL remains.

### Negative / Trade-offs
- Kotlin, KSP and Room must now be upgraded together; a mismatch fails at build time.
- The `TabRow` deprecation warning in `LogMetricScreen.kt` remains. Its replacement (`PrimaryTabRow`) needs a newer Compose BOM than the current one, so it is deferred until the BOM is upgraded.
