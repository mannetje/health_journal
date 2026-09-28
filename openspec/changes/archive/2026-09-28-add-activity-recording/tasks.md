## 1. Domain

- [x] 1.1 Implement `RecordActivityUseCase` in `domain/src/main/kotlin/nl/healthjournal/domain/usecase/RecordActivityUseCase.kt` following the `RecordBloodPressureUseCase` pattern; verify with a JVM unit test covering valid session recording and rejection of end-before-start / negative-distance via `ActivitySession`'s own invariants.

## 2. App wiring

- [x] 2.1 Wire `RecordActivityUseCase` into `HealthJournalApp.onCreate()` and `MainActivity`'s `LoggingViewModel.Factory` construction.
- [x] 2.2 Add `ACTIVITY` to `LoggingViewModel.MetricType`, add `activityDurationInput`/`activityDistanceInput` to `LoggingUiState`, add `onActivityDurationChanged`/`onActivityDistanceChanged` handlers, and add the `ACTIVITY` branch to `saveCurrentMetric()` deriving start/end timestamps from "now minus duration" and converting km to metres.
- [x] 2.3 Add the Activity tab and its two input fields to `LogMetricScreen`. Verified: `./gradlew :domain:test` passes (including `RecordActivityUseCase` valid-session and end-before-start cases) and `./gradlew :app:assembleDebug` builds successfully.

## 3. Docs

- [x] 3.1 Update root `README.md` to reflect that activity sessions can be logged manually from the app (metrics/feature table), if not already covered by the existing text.
