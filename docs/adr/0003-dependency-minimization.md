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

## Approved Exceptions

### Vico (charting)
- **Date:** 2026-09-29
- **Library:** [`com.patrykandpatrick.vico`](https://github.com/patrykandpatrick/vico) (`compose` + `compose-m3` artifacts), version 3.2.3. (Not 3.3.x: Vico's own AAR metadata requires `compileSdk 37` from 3.3.0 onward — a much larger jump than this project's dependency-minimization spirit calls for. 3.2.3 is the newest release that only requires `compileSdk 36`, which this project adopted alongside this dependency; Vico's 3.3.0 release notes confirm "breaking changes: none" versus 3.2.3, so no application code differs by version.)
- **Why it clears the bar in Decision #2:** [ADR 0006 (Health Trend Visualizations)](0006-health-trend-visualizations.md) originally hand-rolled Weight/BP/Glucose trend charts with Compose `Canvas` plus a custom `detectTransformGestures` pan/zoom handler specifically to avoid a third-party dependency. In practice that meant maintaining bespoke pixel math, gesture math, and axis-label layout — exactly the kind of "more code to maintain than a library" trade-off this ADR's own "Negative" section warned about elsewhere. Vico was evaluated against MPAndroidChart (View-based, maintenance-mode) and YCharts/Compose Charts (smaller communities, less mature scroll/zoom support) and chosen because it is: Compose-native (no View interop bridge), MIT-licensed, actively maintained, ships a `compose-m3` theme integration that reads directly from `MaterialTheme.colorScheme`, and provides pinch-zoom/pan natively so the app's own gesture code could be deleted.
- **What it replaced:** the `Canvas`-based `LineTrendChart` in `ChartPrimitives.kt` and its hand-rolled axis/gridline/gesture code (see ADR 0006 for the technical detail).

### Robolectric (test only)
- **Date:** 2026-10-07
- **Library:** [`org.robolectric:robolectric`](https://robolectric.org/), version 4.17, `testImplementation` in the `data` module only. It is not part of the APK.
- **Why it clears the bar:** the schema is not exported and a migration needs a real SQLite. A JVM unit test cannot open a Room database, and an instrumented test needs an emulator in CI. Robolectric runs the migration test on the JVM, so `./gradlew test` stays the one command. The alternative, testing migrations only by hand on a device. It needs network on the first run to fetch the Android jar it runs against.
