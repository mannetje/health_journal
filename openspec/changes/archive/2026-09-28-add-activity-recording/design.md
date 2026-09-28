# Design

## Approach

Mirror the existing `RecordWeightUseCase` / `RecordBloodPressureUseCase` pattern exactly — no new architectural pattern is introduced, so no ADR is needed.

### Domain: `RecordActivityUseCase`

```kotlin
class RecordActivityUseCase(
    private val healthLogRepository: HealthLogRepositoryPort
) {
    suspend operator fun invoke(
        profileId: ProfileId,
        startTime: Instant,
        endTime: Instant,
        distanceInMeters: Double,
        measurementId: MeasurementId = MeasurementId.generate()
    ): ActivitySession {
        val session = ActivitySession(
            id = measurementId,
            profileId = profileId,
            startTime = startTime,
            endTime = endTime,
            distanceInMeters = distanceInMeters
        )
        healthLogRepository.saveActivity(session)
        return session
    }
}
```

`ActivitySession`'s own `init` block already enforces end-after-start and the 0..1,000,000 m distance range, so the use case does no extra validation — same division of responsibility as the other three use cases (value objects/entities validate, use cases just orchestrate + persist).

### App: manual entry, not GPS tracking

The domain capability's Purpose talks about "GPS-tracked" sessions, but there is no location/GPS integration in this app (confirmed: no location permissions, no `FusedLocationProvider` or similar). Building real GPS tracking is out of scope for this change. Instead, the logging screen gets a simple manual-entry form:

- **Duration (minutes)** — text input, decimal allowed.
- **Distance (km)** — text input, decimal allowed, converted to metres (`km * 1000`) before calling the use case.
- `endTime = Instant.now()`, `startTime = endTime.minusSeconds((minutes * 60).toLong())`.

This satisfies the modified requirement's "Manual session entry via app" scenario without pretending to track GPS. The Purpose wording is left as-is per OpenSpec convention (existing capability Purpose is authoritative and not touched by a delta); if the team wants to soften "GPS-tracked" to also cover manual entry, that's a direct edit to the main spec, called out in the sync summary, not part of this delta.

### UI wiring

- `LoggingViewModel.MetricType` gains `ACTIVITY`.
- `LoggingUiState` gains `activityDurationInput: String`, `activityDistanceInput: String`.
- `saveCurrentMetric()` gets an `ACTIVITY` branch parsing both inputs, rejecting blank/non-numeric/non-positive values client-side with the same `IllegalArgumentException` pattern used by the other branches (caught and surfaced as `errorMessage`).
- `LogMetricScreen` gets a fourth `Tab` and a `when` branch with two `OutlinedTextField`s (no NHG classification badge — activity has no NHG category, matching the domain spec which defines no classification requirement for this capability).
- `HealthJournalApp.onCreate()` constructs `recordActivityUseCase = RecordActivityUseCase(healthLogRepository)`.
- `MainActivity` passes it into `LoggingViewModel.Factory`.

### Docs

- `README.md`: add Activity to the metrics table/feature list and to the "supported import formats" or use-case table wherever Weight/BP/Glucose use cases are already listed, so the living README stays accurate per the `architecture-governance` capability's "Living README maintenance" requirement.
- No new ADR: this is an application of an existing, already-documented pattern (use case + ViewModel + Compose screen), not a new architectural decision.

## Alternatives Considered

- **Full GPS tracking**: rejected as out of scope — no location permission infrastructure exists yet, and the spec change only requires the session to be *recordable*, not automatically tracked.
- **Start/end timestamp pickers instead of duration**: rejected for a first pass — duration + distance is the simplest form consistent with how a user would log a completed activity after the fact, and it still produces valid start/end timestamps satisfying the domain contract.
