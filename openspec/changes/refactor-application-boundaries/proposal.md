# Refactor application boundaries

## Why
An architecture review (November 2026) found four structural frictions in the application layer. Each one was checked against the code and holds:

- **One fat secondary port.** `HealthLogRepositoryPort` has 30 methods for six metrics, and every new metric adds five more. Six metric changes are in flight (body temperature, daily activity, energy, fluid, mood, sleep), so it grows to about 60.
- **ViewModels reach past the application layer.** Six ViewModels import secondary ports (`HistoryViewModel` holds six of them; `Logging`, `Profile`, `Medication`, `MigraineLog` and `RecoveryBackup` hold one or more). Coordination and CSV routing by metric name (`"weight"`, `"bp"`, `"spo2"` strings) sit in the UI layer.
- **Use case bloat.** 30 files in `usecase/`, two bundle classes (`EntryUseCases` with 14 members, `MedicationUseCases` with 8) and a nullable-port, dual-constructor pattern in `LoggingViewModel` that exists only to keep Hilt and tests working.
- **Invariants live twice and fail untyped.** Waist bounds are checked in `LoggingViewModel` and again in the domain; the domain throws `IllegalArgumentException` through `require` (71 call sites) and ViewModels catch `Exception` (19 places) and show a generic message, so "diastolic must be below systolic" is lost.

This is one change because the pieces depend on each other: the new ports are what the handlers inject, the handlers are what the ViewModels dispatch to, and the guardrail tests only pass once all of them are done. Nothing the user sees changes.

## What Changes
- **Segregated secondary ports:** `HealthLogRepositoryPort` is replaced by one port per metric (weight, blood pressure, glucose, activity, waist circumference, oxygen saturation). Migraine, medication and profile ports stay as they are.
- **Application boundary:** typed commands and queries with handlers in `:domain`, a small in-process dispatcher in `:app` (Hilt map multibinding). ViewModels depend on the dispatcher, not on use case classes or secondary ports.
- **Consolidated handlers:** one delete, one update and one record path across entry types instead of one class per metric; CSV export and import become typed queries and commands keyed by a `MetricKind` enum instead of strings.
- **Typed validation errors:** `DomainError` with a code and parameters (never display text). Safe factories on the value objects return a result; the validating constructors stay as a last line of defence. The UI maps error codes to existing string resources in English and Dutch ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)).
- **Single source for ranges:** the UI reads bounds from the domain constants and no longer repeats them.
- **Time through a clock:** commands take a `Clock`-derived instant from the handler, not `Instant.now()` defaults.
- **All ViewModels migrated:** Logging, History, Medication, MigraineLog, Profile and RecoveryBackup.
- **Guardrail tests** keep it that way (see tasks, section 6).
- **Docs:** new ADR, ADR 0002 amended, the add-a-metric how-to and playbook updated, the architecture section of the README updated.

## Capabilities
- **Modified Capability:** `architecture-governance` (layered structure now names the segregated ports and the application boundary; new requirement for the boundary rules and their automated checks)

No behaviour spec changes: `health-metrics/*`, `entry-management`, `data-export`, `data-backup`, `localization` and the others keep their text, and this change must leave their scenarios passing as they are.

## Impact
- **Database:** none. No migration, schema version unchanged (8).
- **Stored data shape:** none. Backup documents, CSV columns and range labels are unchanged.
- **Medical thresholds:** none touched.
- **User-visible behaviour:** none. Error messages keep their current wording in both languages; where a validation failure now reaches the UI with its own code, it shows the specific existing message instead of the generic "could not save" one (a deliberate improvement, listed in the design).
- **Affected code:** `:domain` (ports, new `command/` package, value object factories, use case removal), `:data` (`RoomHealthLogRepository` implements six ports, CSV adapters take the narrow ports), `:app` (Hilt modules, dispatcher, six ViewModels, error mapping, tests).
- **Dependencies and permissions:** none. Hilt stays in `:app` only ([ADR 0035](../../../docs/adr/0035-hilt-for-dependency-injection.md)).
- **In-flight changes:** the six `add-*` metric changes name `HealthLogRepositoryPort` and per-metric use cases. See design decision 9 for the order.

## Sources
- Architecture review by the user, `architecture-review-and-recommendations2.md` (November 2026), checked line by line against the repository on 2026-11-05. The design lists where the review was corrected.
