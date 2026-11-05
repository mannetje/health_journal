## 0. Decisions
- [ ] 0.1 Confirm the dispatcher over grouped application services (design decision 2)
- [ ] 0.2 Confirm order against the six in-flight metric changes: this change first (design decision 9)
- [ ] 0.3 Confirm value objects keep validating constructors and gain `of(...)` factories (design decision 4)
- [ ] 0.4 Confirm release plan: own release 1.8.0 or folded into the next feature
- [ ] 0.5 Spec review by the user before implementation starts
- [ ] 0.6 Baseline: run `:domain:test`, `:data:test`, `:app:testDebugUnitTest` and `scripts/check-docs.sh` and record that all pass before the first edit

## 1. Segregated ports (`:domain`, `:data`, `:app` wiring)
- [ ] 1.1 Create the six metric ports under `domain/port/secondary/` with the current method names and semantics
- [ ] 1.2 `RoomHealthLogRepository` implements the six ports; `DataModule` exposes it once per port type
- [ ] 1.3 `DataPortsModule` provides each port; `CsvDataExportAdapter` and `CsvDataImportAdapter` take only the ports they use
- [ ] 1.4 Switch every use case and test fake to the narrow port it needs (`UseCasesTest`, `ViewModelsTest`, `CsvAdaptersTest`)
- [ ] 1.5 Delete `HealthLogRepositoryPort`; confirm no reference remains (code, docs, specs)
- [ ] 1.6 Checkpoint: all three test tasks green, no assertion edited

## 2. Command and result infrastructure
- [ ] 2.1 `domain/command/`: `Command<R>`, `Query<R>`, handler interfaces, `DomainResult<T>`, `DomainError`, `ValidationRule`, `EntryKind`, `MetricKind`, `Dispatcher`
- [ ] 2.2 `app/command/DefaultDispatcher` with the Hilt map multibinding and `CommandModule`
- [ ] 2.3 `DefaultDispatcherTest`: routes by class, returns the typed result, propagates cancellation, and a registry test that every `Command` and `Query` subclass has a handler
- [ ] 2.4 Checkpoint: measure the wiring cost. If the multibinding needs more than one unchecked cast or per-handler boilerplate beyond one `@Provides` line, fall back to grouped application services (design decision 2) and revise sections 3 to 5

## 3. Handlers and typed validation (`:domain`)
- [ ] 3.1 Value object factories `of(...)` returning `DomainResult` for blood pressure, glucose, weight, waist, activity and oxygen saturation; constructors keep `require`; JVM tests for every bound and for diastolic not below systolic
- [ ] 3.2 `RecordEntryCommand` hierarchy and handlers, one per metric, sharing clock and validation; handlers take a `Clock`
- [ ] 3.3 `UpdateEntryCommand` and `DeleteEntryCommand` with `EntryGateway`; JVM tests for all seven kinds including unknown id (`NotFound`)
- [ ] 3.4 `ExportCsvQuery` and `ImportCsvCommand` keyed by `MetricKind`; the old string keys are mapped once at the edge; round-trip tests unchanged
- [ ] 3.5 Query handlers for history, pillbox day, adherence and profile reads that ViewModels still need (wrapping existing use cases)
- [ ] 3.6 Migraine, medication and backup actions as commands where a ViewModel calls them today (end migraine, save, archive, delete, take all, restore)
- [ ] 3.7 JVM tests for every handler: success, each failure variant, no `Instant.now()` (fixed clock)

## 4. Error mapping and single source for ranges (`:app`)
- [ ] 4.1 `DomainError.toUiText()` mapping each rule to an existing string resource (`UiText.Res`), English and Dutch
- [ ] 4.2 Test that every `ValidationRule` resolves a string in `values/` and `values-nl/`
- [ ] 4.3 Remove the duplicated bounds in `LoggingViewModel` (waist lines 384 and 533 and any others found with `MIN_` and `MAX_`); the ViewModel passes typed input and renders the returned error
- [ ] 4.4 Keep `UiTextException` only for UI parse errors; no domain rule uses it

## 5. ViewModel migration (`:app`)
- [ ] 5.1 `HistoryViewModel`: dispatcher only; delete `EntryUseCases`; no secondary port imports
- [ ] 5.2 `LoggingViewModel`: dispatch `RecordEntryCommand`; remove nullable test-only parameters and the second constructor; split the save `when` into one small mapper per metric
- [ ] 5.3 `MedicationViewModel`: dispatcher; delete `MedicationUseCases`
- [ ] 5.4 `MigraineLogViewModel`, `ProfileViewModel`, `RecoveryBackupViewModel`: dispatcher, no port imports
- [ ] 5.5 Replace `catch (e: Exception)` around dispatches with a `when` on `DomainResult`; keep a catch only where an Android API can throw
- [ ] 5.6 Update `UseCaseModule` and `PlatformModule`; delete the now-unused `Record*`, `Update*` and `Delete*UseCase` classes and their `UseCasesTest` cases (replaced by handler tests)
- [ ] 5.7 Update `ViewModelsTest`, `MedicationViewModelTest`, `MigraineLogViewModelTest` and the tests under `ui/history`, `ui/profile`, `ui/backup` to use a fake `Dispatcher` or real handlers over fakes
- [ ] 5.8 Checkpoint: all three test tasks green; behaviour tests keep their assertions

## 6. Guardrails
- [ ] 6.1 No ViewModel imports `domain.port.secondary` (allowed set for non-ViewModel app classes listed in the test)
- [ ] 6.2 No `data class *UseCases(` anywhere in `app/src/main`
- [ ] 6.3 No `catch (e: Exception)` or `catch (e: Throwable)` around a dispatcher call in a ViewModel
- [ ] 6.4 Handlers under `domain/command/` return `DomainResult`; no `Instant.now()` there
- [ ] 6.5 `HealthLogRepositoryPort` does not exist; `:domain` and `:data` still have no Hilt, Dagger or `javax.inject` (existing test stays)

## 7. Documentation and OpenSpec
- [ ] 7.1 New ADR 0036 "Application boundary: commands, queries and typed errors" (decision, alternatives from design, consequences); add to the README ADR index (`RepositoryGuardrailsTest` checks it)
- [ ] 7.2 Amend ADR 0002: the application layer's primary boundary and the segregated ports; link ADR 0036
- [ ] 7.3 Update `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md` to the new shape (one port, one command set, one handler per kind, one string per rule)
- [ ] 7.4 README architecture section and diagrams: ports list and the dispatcher
- [ ] 7.5 Delta spec for `architecture-governance` is merged on archive; check `openspec validate --all --strict`
- [ ] 7.6 Update the six in-flight `add-*` changes: replace `HealthLogRepositoryPort` and per-metric use case tasks with the new ones (or note the dependency)
- [ ] 7.7 CHANGELOG entry (Changed: internal structure, no behaviour change) if released
- [ ] 7.8 Run `scripts/check-docs.sh`

## 8. Verification
- [ ] 8.1 `:domain:test`, `:data:test`, `:app:testDebugUnitTest`, lint, and the instrumented migration test pass; schema version still 8
- [ ] 8.2 Emulator pass, English and Dutch, light and dark: log, edit, delete each of the seven entry types; invalid input shows the specific message; CSV export and import for every metric; encrypted backup and restore; medication take-all; migraine start and end
- [ ] 8.3 Compare a backup file and a CSV export made before and after: identical content
- [ ] 8.4 CI green on all workflows, including OpenSpec strict validation
