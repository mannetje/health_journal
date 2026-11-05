## Context

The hexagon is sound at the module level: `:domain` has no framework dependency, DI stays in `:app` ([ADR 0035](../../../docs/adr/0035-hilt-for-dependency-injection.md)) and `RepositoryGuardrailsTest` already enforces that. The friction is inside the application layer ([ADR 0002](../../../docs/adr/0002-hexagonal-architecture.md) names Use Cases but never defined a primary boundary). Messages shown to the user must stay localized ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)); metrics stay plain, with no assessment ([ADR 0026](../../../docs/adr/0026-oxygen-saturation-without-assessment.md) and the later ones).

## Goals / Non-Goals

**Goals:** ISP-sized ports; ViewModels that touch only the application boundary; typed validation errors with no duplicated bounds; fewer classes per new metric; checks that stop the drift coming back.
**Non-Goals:** any user-visible change, database or backup change, domain events, richer metric aggregates, a new DI framework, changes to the `Medication` aggregate or `Pillbox`.

## What the review got right, wrong and missed

**Confirmed against the code:** the fat port (30 methods, 67 lines); `EntryUseCases` (14) and `MedicationUseCases` (8); `HistoryViewModel` injecting `ProfileRepositoryPort`, `MigraineRepositoryPort` and four export/import ports; the duplicated waist bounds (`LoggingViewModel` lines 384 and 533); `require` in value objects and `catch (e: Exception)` in ViewModels; no primary port layer; the long `when (selectedMetric)` save switch.

**Corrected:**
- The leak is wider than two ViewModels. Six import secondary ports (Logging, History, Medication, MigraineLog, Profile, RecoveryBackup). The review's migration plan and its new guardrail `viewModelsDoNotImportSecondaryPorts` would fail on the four it never mentions.
- `MedicationUseCases` is counted as a smell, but no task in the review's plan removes it, and its own `noSyntheticUseCaseBundlesExist` test would fail on it.
- The review says `TakeAllForSlotUseCase` and `SaveMedicationUseCase` call `RearmRemindersUseCase` directly. They do not: re-arming is driven from `ReminderCoordinator` in `:app`. So the "procedural chaining" hot spot, and the case for domain events, does not exist. Domain events are out of scope.
- The review's error mapping uses `UiText.Dynamic(message)`. That type does not exist (`UiText` has `Res` and `Plain`), and passing the domain's English message would show English to Dutch users. Errors carry codes and parameters; the UI maps them to string resources.
- The review's file list names tests that do not exist (`HistoryViewModelTest`, `LoggingViewModelTest`). The real ones are `ViewModelsTest`, `UseCasesTest`, `CsvAdaptersTest`, `MedicationViewModelTest`, `MigraineLogViewModelTest` and tests under `ui/history`, `ui/profile`, `ui/backup`.
- "Anemic metric aggregates" is a deliberate property here: metrics are plain records with no assessment. Not a defect to fix.

**Missed by the review:**
- CSV routing by metric name strings in `HistoryViewModel` (`"bp"`, `"spo2"`, ...). Becomes a `MetricKind` enum.
- `LoggingViewModel` has nullable port and use case parameters with defaults, plus a second `@Inject` constructor, only so tests can build it. The dispatcher removes the need.
- `Instant.now()` is used as a default in 8 places in `:domain`. Handlers take a `Clock`.
- The sample `DeleteEntryHandler` injects all seven repositories, which is a fat dependency list again.
- `CsvDataExportAdapter` and `CsvDataImportAdapter` in `:data` also depend on the fat port.
- The `add-a-metric` how-to and playbook describe the old shape and must change, or every later metric reintroduces it.
- Six metric changes are in flight and each adds port methods and use cases in the old shape (see decision 9).

## Decisions

### 1. Segregated ports
One port per metric in `domain/port/secondary/`: `WeightRepositoryPort`, `BloodPressureRepositoryPort`, `GlucoseRepositoryPort`, `ActivityRepositoryPort`, `WaistCircumferenceRepositoryPort`, `OxygenSaturationRepositoryPort`. Method names and semantics are unchanged (`save`, `get...History`, `observe...History`, `update` returning Boolean, `delete` returning Boolean). `RoomHealthLogRepository` implements all six and `DataModule` exposes it once under each type; no new Room classes. `HealthLogRepositoryPort` is deleted (clean cut, no deprecation period: it has no consumers outside this repository). Alternative, a generic `EntryStore<E>` base: rejected for now because the entries differ in shape (`ActivitySession` vs `*Entry`) and named ports read better at call sites. The six ports share identical method shapes, so a generic base stays possible later.

### 2. Application boundary: typed commands and a dispatcher
- `:domain/command/`: `Command<R>`, `Query<R>`, `CommandHandler<C, R>`, `QueryHandler<Q, R>`, `DomainResult<T>`, `DomainError`, and an interface `Dispatcher` with `suspend fun <R> dispatch(command: Command<R>): DomainResult<R>` and `suspend fun <R> query(query: Query<R>): DomainResult<R>`. The result type is carried by the command, so a call site cannot state a wrong `R` (the review's `dispatch<C, R>` signature allows that).
- `:app/command/DefaultDispatcher` looks the handler up by command class in a Hilt `@Multibinds` map (`@ClassKey`). `:domain` has no Hilt, so handlers are plain classes and `CommandModule` in `:app` provides and registers them.
- A missing handler is a programming error and fails a unit test at build time (a test enumerates every `Command` and `Query` subclass under `command/` and checks each has a registered handler), not a runtime `DomainError`.
- Alternative considered: grouped application services per area (`EntryService`, `ExchangeService`, `PillboxService`) injected directly, no dispatcher. Smaller and easier to read, but the dispatcher was the stated choice. Kept as the fallback if task 2.4 shows the multibinding costs more than it saves.
- Existing use cases that carry real logic and are not CRUD (`GetHealthHistoryUseCase`, `GetPillboxDayUseCase`, `GetAdherenceUseCase`, `RearmRemindersUseCase`, the backup use cases) stay as classes, wrapped by a query handler only where a ViewModel needs them. They are not rewritten.

### 3. Consolidation
- `DeleteEntryCommand(kind: EntryKind, id)` and `UpdateEntryCommand` replace the seven `Delete*` and six `Update*` classes. The handler reaches the ports through a small `EntryGateway` (a map from `EntryKind` to the narrow port operations), not seven constructor parameters. `EntryKind` covers the six metrics and migraine.
- `RecordEntryCommand` is a sealed hierarchy (`RecordWeight`, `RecordBloodPressure`, ...) so each keeps its own typed fields. Handlers share validation and clock handling. Adding a metric then means one command, one handler, one port.
- `ExportCsvQuery(profile, kind: MetricKind)` and `ImportCsvCommand(profile, kind, content)` replace the string switch. The old string keys are mapped once, at the edge, where the Profile screen produces them.

### 4. Typed errors, safe factories
- `DomainError`: `Invalid(field, rule, params)`, `NotFound(kind, id)`, `Storage(cause)`. No display text. `ValidationRule` is an enum (`OutOfRange`, `SystolicNotAboveDiastolic`, `Required`, ...).
- Value objects gain `of(...)` factories returning `DomainResult`. The primary constructor keeps its `require` checks, so Room mapping, tests and any missed caller still cannot build an invalid object. The review proposed private constructors; that breaks the mapper layer and the 71 `require` sites for little gain, and is a non-goal here.
- Bounds exist once, as constants on the value object. `LoggingViewModel` stops using `takeIf { in MIN..MAX }`: it passes typed input to the command and renders the returned error. Live previews (waist and glucose range chips) keep calling the pure classifiers.
- The UI maps `DomainError` to `UiText.Res` with the existing strings (`log_err_waist` and the like). Where a rule has no specific string today, the generic one is used; no new copy unless a task says so. `UiTextException` stays for UI-only parse errors (a field that is not a number) and is no longer used for domain rules.

### 5. Time
Handlers take a `java.time.Clock` (already used by `TakeAllForSlotUseCase`). Commands carry the timestamp the user chose, or `null` for "now", resolved in the handler. No `Instant.now()` default remains in command classes.

### 6. ViewModels
All six ViewModels depend on the dispatcher plus preferences and display state only. Nullable test-only parameters and second constructors go away; tests pass a fake `Dispatcher` or real handlers over in-memory fakes.

### 7. Guardrails
Added next to `RepositoryGuardrailsTest`: no ViewModel imports `domain.port.secondary`; no `data class *UseCases(` bundle; no `catch (e: Exception)` around a dispatcher call in a ViewModel; handlers under `domain/command/` return `DomainResult`; no `Instant.now()` in `domain/command/`; `HealthLogRepositoryPort` does not exist. A failure names the file. Non-ViewModel app classes that legitimately use ports (Hilt modules, `AppLockManager`, `ReminderCoordinator`, alarm scheduling) are the allowed set.

### 8. Behaviour preservation
This is a refactor under test. The existing JVM, CSV and migration tests are the safety net and keep their assertions; only setup may change. An emulator pass at the end covers log, edit, delete, export and import for every metric, in English and Dutch.

### 9. Order against the in-flight metric changes
Six `add-*` changes (body temperature, daily activity, energy, fluid, mood, sleep) each add port methods and use cases in the old shape. Recommended: land this change first, then update their tasks and designs, which name `HealthLogRepositoryPort` and `Record/Update/DeleteX` classes. The updated add-a-metric playbook then makes each of them smaller. If some must land first, they land unchanged and this change absorbs their ports at the cost of one more port and handler each.

## Risks / Trade-offs
- **Large diff touching every screen.** Mitigated by phase order: each phase compiles and passes tests on its own, and phases 1 and 2 add structure without moving behaviour.
- **Dispatcher indirection** hides the call graph from "find usages". Mitigated by one handler per command and a naming rule (`FooCommand` / `FooHandler`).
- **Hilt multibinding with generic handlers** needs `@JvmSuppressWildcards` and one unchecked cast, contained in `DefaultDispatcher` and covered by its test.
- **Error mapping drift:** a new `ValidationRule` without a string. A test iterates the enum and resolves a string for each rule in both languages.

## Open Questions
- Confirm the dispatcher over grouped application services (decision 2, fallback noted).
- Release on its own (1.8.0, CHANGELOG "Changed: internal structure, no behaviour change") or fold into the next feature release.
