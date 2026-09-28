# Proposal

## Why

The `health-metrics/activity` capability is fully modeled in the domain and data layers (`ActivitySession`, `HealthLogRepositoryPort.saveActivity`, `ActivityDao`) and its history/export already work, but there is no way for a user to actually record an activity session from the app: no application use case exists to create one, and the logging screen only offers Weight, Blood Pressure, and Glucose. The only way an `ActivitySession` currently enters the system is via CSV import (Libra weight import does not apply here; activity has no import format at all). This closes that gap so activity reaches the same level of feature parity as the other three metrics.

## What Changes

- Add a `RecordActivityUseCase` in `:domain`, mirroring the shape of `RecordWeightUseCase`/`RecordBloodPressureUseCase`, that validates and persists an `ActivitySession` via `HealthLogRepositoryPort.saveActivity`.
- Add an `ACTIVITY` option to `LoggingViewModel.MetricType` and wire manual-entry inputs (duration in minutes, distance in km) into `LoggingUiState`, computing start/end timestamps from "now minus duration".
- Add the Activity tab and its input fields to `LogMetricScreen`.
- Wire `RecordActivityUseCase` into `HealthJournalApp`'s manual DI and `LoggingViewModel.Factory`/`MainActivity`.
- Update the root `README.md` metrics overview/table and module table if the activity metric or new use case is not yet reflected there.
- Add an ADR only if a new architectural decision is introduced (none expected — this follows the existing use case / ViewModel pattern).

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `health-metrics/activity`: adds a requirement that the system SHALL allow manually recording an activity session through the application's own UI (not only via bulk import or external data sources), so the capability's "Record activity session" requirement is verifiably reachable end-to-end.

## Impact

- `:domain` — new `RecordActivityUseCase`.
- `:app` — `LoggingViewModel`, `LogMetricScreen`, `HealthJournalApp`, `MainActivity` (DI wiring).
- `README.md` — metrics table / feature list, if it does not already mention activity logging via the UI.
- No changes to `:data` (Room entities/DAOs and repository adapter already support saving activity sessions).
