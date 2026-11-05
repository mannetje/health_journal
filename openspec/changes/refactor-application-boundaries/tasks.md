Order: the steps that stand on their own come first (ports, typed errors, clock, guardrails). The dispatcher is decided and built last, with measured cost, so sections 1 to 3 keep their value even if section 4 falls back to grouped application services.

## 0. Decisions
- [x] 0.1 Confirm value objects keep validating constructors and gain `of(...)` factories (design decision 4)
- [x] 0.2 Confirm order against the six in-flight metric changes: this change first (design decision 9)
- [x] 0.3 Confirm release plan: own release 1.8.0 or folded into the next feature
- [x] 0.4 Confirm three ADRs (0036 application boundary, 0037 typed errors, 0038 bounded contexts) and the ADR 0002 and 0035 amendments (design decisions 10 and 11)
- [x] 0.4a Context map confirmed by the user: `nhg` joins `metrics`; `profile` and `metrics` are separate contexts, `profile` upstream; `HeightCm`, `WeightKg` and `EntryComment` go to the shared kernel (design decision 11)
- [x] 0.5 Spec review by the user before implementation starts
- [x] 0.6 Baseline: run `:domain:test`, `:data:test`, `:app:testDebugUnitTest` and `scripts/check-docs.sh` and record that all pass before the first edit

## 1. Segregated ports (`:domain`, `:data`, `:app` wiring)
- [x] 1.1 Create the six metric ports under `domain/port/secondary/` with the current method names and semantics
- [x] 1.2 `RoomHealthLogRepository` implements the six ports; `DataModule` exposes it once per port type
- [x] 1.3 `DataPortsModule` provides each port; `CsvDataExportAdapter` and `CsvDataImportAdapter` take only the ports they use
- [x] 1.4 Switch every use case and test fake to the narrow port it needs (`UseCasesTest`, `ViewModelsTest`, `CsvAdaptersTest`)
- [x] 1.5 Shared in-memory fakes of the secondary ports in one test fixture used by domain and app tests
- [x] 1.6 Delete `HealthLogRepositoryPort`; confirm no reference remains (code, docs, specs)
- [x] 1.7 Checkpoint: all three test tasks green, no assertion edited

## 2. Typed errors, factories and clock (`:domain`, `:app` mapping)
- [x] 2.1 `domain/command/` result types: `DomainResult<T>`, `DomainError` (`Invalid`, `NotFound`, `Storage`), `ValidationRule`; no display text; cancellation is never mapped to `Storage`
- [x] 2.2 Value object factories `of(...)` returning `DomainResult` for blood pressure, glucose, weight, waist, activity and oxygen saturation; constructors keep `require`; JVM tests for every bound and for diastolic not below systolic
- [x] 2.3 Bounds defined once as constants on the value objects; list every duplicate found with `MIN_` and `MAX_` in `:app` (waist at `LoggingViewModel` lines 384 and 533 and any others)
  - Found and replaced with the domain constants: weight `1.0..700.0`, systolic `40..300`, diastolic `20..200` in `LoggingViewModel`; the same three plus oxygen `50..100` in `EntryDialogs`. Waist already used `WaistCircumferenceCm.MIN_CM..MAX_CM`.
  - Left on purpose, UI scroll ranges that are not validation: `StackedBpPulsePicker` 40..260, 20..200 and 30..220; `LogMetricScreen` 20.0..250.0, 50.0..100.0 and 40.0..200.0.
- [x] 2.4 `Clock` injected where a default `Instant.now()` exists in `:domain` (8 places); fixed-clock tests
- [x] 2.5 `DomainError.toUiText()` mapping each rule to an existing string resource (`UiText.Res`), English and Dutch; `UiTextException` stays for UI parse errors only
- [x] 2.6 Test that every `ValidationRule` resolves a string in `values/` and `values-nl/`
- [x] 2.7 Checkpoint: all three test tasks green

## 3. Guardrails that already hold
- [x] 3.1 `HealthLogRepositoryPort` does not exist; `:domain` and `:data` still have no Hilt, Dagger or `javax.inject` (existing test stays)
- [x] 3.2 No `Instant.now()` default in `domain/command/` or in any use case that takes a `Clock`
- [x] 3.3 Guardrails for ViewModels (no secondary port import, no `*UseCases` bundle, no `catch (e: Exception)` around a dispatch) are written now as report-only with the current offenders listed; they become failing in section 6
  - Implemented in `ArchitectureGuardrailsTest`. It started as a ratchet with the known offenders listed; section 6 emptied the lists and 6.8 turned them into plain failing assertions.

- [x] 3.4 Context guardrail: a test scans `model/` imports against the context map (design decision 11), report-only, listing `EntryComment` in `metrics`, `Profile` using `HeightCm` and `WeightKg`, the `metrics` and `nhg` cycle, and `nhg` reading `profile`
- [x] 3.5 Resolve each listed coupling: move `EntryComment`, `HeightCm` and `WeightKg` to the shared kernel; move the `nhg` classifiers into `metrics`; replace `Profile.classifyWaistCircumference` with a domain service in `metrics` that takes the sex as an argument and update its callers (`Profile.calculateBmi` stays); existing classifier and BMI tests keep their assertions; record any accepted exception in the test with a reason
  - Done: kernel types are in `model/common`, the `Nhg*` classifiers are in `model/metrics`, `WaistCircumferenceClassifier.classify(sex, waist)` replaces `Profile.classifyWaistCircumference`. No accepted exception.
- [x] 3.6 Turn the context guardrail into a failing test once the list is empty or every remaining edge is a recorded exception

## 4. Application boundary: dispatcher or grouped services
- [x] 4.1 `Command<R>`, `Query<R>`, `ObserveQuery<R>`, handler interfaces and the `Dispatcher` interface (suspend `dispatch` and `query`, `observe` returning `Flow<DomainResult<R>>`) in `domain/command/`
- [x] 4.2 Spike: `app/command/DefaultDispatcher` with the Hilt `@Multibinds` map and `CommandModule`, with one real handler (delete entry)
- [x] 4.3 `DefaultDispatcherTest`: routes by class, returns the typed result, propagates cancellation; registry test that every `Command` and `Query` subclass has a handler
- [x] 4.4 Decision checkpoint, recorded in the design: measure the wiring cost. If the multibinding needs more than one unchecked cast or per-handler boilerplate beyond one `@Provides` line, switch to grouped application services (`EntryService`, `ExchangeService`, `PillboxService`) with the same commands, results and guardrails, and revise sections 5 and 6 accordingly. Outcome: the dispatcher stays (see design, "Spike result")

## 5. Handlers and consolidation (`:domain`)
- [x] 5.1 One record command and handler per metric (no sealed hierarchy, so the registry can read each declaration), using the factories and the clock
- [x] 5.2 One update command per metric, and `DeleteEntryCommand` with `EntryGateway`; JVM tests for all seven kinds including unknown id (`NotFound`)
- [x] 5.3 `ExportCsvQuery` and `ImportCsvCommand` keyed by `EntryKind` (Libra is a flag); the old string keys are mapped once at the edge; round-trip tests unchanged
- [x] 5.4 `GetActiveProfileQuery` and the observe queries for history and pillbox day, so no ViewModel needs the profile or repository ports
- [x] 5.5 Query handlers for adherence and other reads a ViewModel still needs (wrapping existing use cases)
- [x] 5.6 Migraine, medication and backup actions as commands where a ViewModel calls them today (end migraine, save, archive, delete, take all, restore)
- [x] 5.7 JVM tests for every handler: success, each failure variant, fixed clock
- [x] 5.8 New ports, commands and handlers are placed in their context package (`domain/<context>/{port,command}`); existing models and use cases are not moved. Check whether ending a migraine episode belongs on `MigraineEntry`; move the rule if so, with tests

## 6. ViewModel migration and final guardrails (`:app`)
- [x] 6.1 `HistoryViewModel`: dispatcher only; delete `EntryUseCases`; no secondary port imports
- [x] 6.2 `LoggingViewModel`: dispatch `RecordEntryCommand`; remove the duplicated bounds, nullable test-only parameters and the second constructor; split the save `when` into one small mapper per metric
- [x] 6.3 `MedicationViewModel`: dispatcher; delete `MedicationUseCases`
- [x] 6.4 `MigraineLogViewModel`, `ProfileViewModel`, `RecoveryBackupViewModel`: dispatcher, no port imports
- [x] 6.5 Replace `catch (e: Exception)` around dispatches with a `when` on `DomainResult`; keep a catch only where an Android API can throw
- [x] 6.6 Update `UseCaseModule` and `PlatformModule`; delete the now-unused `Record*`, `Update*` and `Delete*UseCase` classes and their `UseCasesTest` cases (replaced by handler tests)
- [x] 6.7 Update `ViewModelsTest`, `MedicationViewModelTest`, `MigraineLogViewModelTest` and the tests under `ui/history`, `ui/profile`, `ui/backup` to use a fake `Dispatcher` or real handlers over the shared fakes
- [x] 6.8 Turn the report-only guardrails of 3.3 into failing tests; allowed set for non-ViewModel app classes is listed in the test; handlers under `domain/command/` return `DomainResult`
- [x] 6.9 Checkpoint: all three test tasks green; behaviour tests keep their assertions

## 7. Documentation and OpenSpec
- [x] 7.1 New ADR 0036 "Application boundary: commands and queries": hexagonal with a command and query application layer, dispatcher (or grouped services if 4.4 fell back), observe queries, main-safety rule, alternatives, consequences
- [x] 7.2 New ADR 0037 "Typed domain errors and a single source for rules": `DomainResult`, `DomainError` with code and parameters and no display text, `of(...)` factories next to validating constructors, bounds once, `Clock`
- [x] 7.3 Amend ADR 0002: add a "Primary ports" section and a line that Clean Architecture's use case idea is borrowed while presenters and per-use-case response models are not adopted; link ADR 0036 and 0037
- [x] 7.4 One-line note in ADR 0035: the handler map lives in `:app`, so the no-Hilt-in-`:domain` rule holds
- [x] 7.4a New ADR 0038 "Bounded contexts and tactical roles": the context map and shared kernel, import rules, entity, value object, domain service and application service roles, where new code goes, and that the move of existing code is a follow-up
- [x] 7.4b `docs/dev` context map and glossary (slot, intake, pillbox, range label, episode)
- [x] 7.5 Add ADR 0036, 0037 and 0038 to the README ADR index (`RepositoryGuardrailsTest` checks it)
- [x] 7.6 Update `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md` to the new shape (one port, one command set, one handler per kind, one string per rule)
- [x] 7.7 README architecture section and diagrams: ports list and the application boundary
- [x] 7.8 Delta spec for `architecture-governance` is merged on archive; check `openspec validate --all --strict`
- [x] 7.9 Update the six in-flight `add-*` changes: replace `HealthLogRepositoryPort` and per-metric use case tasks with the new ones (or note the dependency)
- [x] 7.10 CHANGELOG entry (Changed: internal structure, no behaviour change) if released
- [x] 7.11 Run `scripts/check-docs.sh`

## 8. Verification
- [x] 8.1 `:domain:test`, `:data:test`, `:app:testDebugUnitTest`, lint, and the instrumented migration test pass; schema version still 8
  - Run on 2026-11-05: `:domain:test`, `:data:testDebugUnitTest`, `:app:testDebugUnitTest` and `:app:lintDebug` green; `:data:connectedDebugAndroidTest` green on the Pixel_7_Pro emulator; `HealthJournalDatabase` is still `version = 8` and `data/schemas` is unchanged.
- [x] 8.2 (accepted by the user as good enough: live on the emulator only weight log, list and delete in Dutch, dark, plus the oxygen edit dialog; the other flows are covered by the JVM handler, ViewModel, round-trip and backup tests) Emulator pass, English and Dutch, light and dark: log, edit, delete each of the seven entry types; invalid input shows the specific message; CSV export and import for every metric; encrypted backup and restore; medication take-all; migraine start and end
- [x] 8.3 (accepted by the user as covered by the diff and tests, not a live file comparison: no CSV or backup file was exported from both builds, because export goes through the system save picker. Checked instead: `git diff` of `:data` shows the CSV export adapter changed only in which port it reads from, `BackupJson` only in imports and the waist classifier call, and mappers only in imports; the round-trip and backup JVM tests pass with unchanged assertions) Compare a backup file and a CSV export made before and after: identical content
- [x] 8.4 Measured on 2026-11-05 against a worktree at `9a3752d` (baseline) and the working tree (current). Clean `assembleDebug`: 48 s and 45 s (warm daemon, busy machine, so only "no regression"). Debug APK: 36,737,494 and 36,852,182 bytes (+114,688, +0.3%). Cold start (`am start -W -S`, TotalTime, back to back on one emulator session): mean about 3639 ms and 3610 ms. `:app:assembleRelease` with R8 builds with no keep-rule change; release unsigned APK 32,056,728 bytes. Record debug APK size, cold start and build time before and after; one release-minified build to confirm R8 needs no keep rules
- [ ] 8.5 CI green on all workflows, including OpenSpec strict validation
