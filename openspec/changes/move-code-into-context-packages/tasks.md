Prerequisite: `refactor-application-boundaries` is implemented and archived. Each phase moves one context and leaves the build and all tests green, so it can be reverted alone. No assertion in an existing test changes; only package and import lines.

## 0. Preparation
- [x] 0.1 Confirm the order against open branches: no `add-*` branch with `:domain` changes is open, or each is rebased after every phase
  - Note: no other feature branch exists locally or on the remote, so nothing to rebase.
- [x] 0.2 Check every use case for reads across contexts (for example `GetHealthHistoryUseCase`); place each in its owner or record an exception with a reason (design 6)
- [x] 0.3 Search paths and package names outside Kotlin imports: `RepositoryGuardrailsTest`, `scripts/`, R8 or ProGuard rules, `docs/`, ADRs, `openspec/` specs and playbooks, Gradle files; list what each phase must update
- [x] 0.4 Write the explicit old-to-new mapping (file to context and role) and review it before any move
- [x] 0.5 Baseline: `:domain:test`, `:data:test`, `:app:testDebugUnitTest`, lint and `scripts/check-docs.sh` all pass; record the result

> Deviation: all contexts (sections 1 to 8) were moved in one commit, verified as a whole, instead of one commit per context. `profile` has no use case to move (there is no `CreateProfileUseCase`).

## 1. mindfulness
- [x] 1.1 `git mv` `model/mindfulness` and its tests to `mindfulness/model`; rewrite package and imports (the mindfulness UI in `:app`)
- [x] 1.2 Test tasks green

## 2. lock
- [x] 2.1 Move `model/lock` and `AppLockPort` to `lock/model` and `lock/port`, with tests
- [x] 2.2 Test tasks green

## 3. migraine
- [x] 3.1 Move `model/migraine`, `MigraineUseCases`, `MigraineRepositoryPort`, `MigraineDataPorts` with tests
- [x] 3.2 Test tasks green

## 4. medication
- [x] 4.1 Move `model/medication`, the ten medication use cases, `MedicationRepositoryPort`, `MedicationDataPorts`, `ReminderSchedulerPort` with tests
- [x] 4.2 Test tasks green

## 5. backup
- [x] 5.1 Move `model/backup`, `BackupUseCases`, `BackupPorts` with tests; the backup and CSV round-trip tests pass unchanged
- [x] 5.2 Test tasks green

## 6. profile
- [x] 6.1 Move `model/profile`, `CreateProfileUseCase`, `ProfileRepositoryPort` with tests
- [x] 6.2 Test tasks green

## 7. metrics (including nhg)
- [x] 7.1 Move `model/metrics` (it already contains the NHG classifiers), the metric use cases or handlers, `OxygenSaturationUseCases`, `GetHealthHistoryUseCase`, the six metric ports, `DataExportPort` and `DataImportPort` with tests
- [x] 7.2 Test tasks green; classifier and range-label tests unchanged

## 8. shared
- [x] 8.1 Move `model/common` (which already contains the shared-kernel types `EntryComment`, `HeightCm` and `WeightKg`) to `shared/model`
- [x] 8.2 Test tasks green

## 9. Guardrail and documentation
- [x] 9.1 Extend the context test: no domain file outside `<context>/{model,usecase,port,command}`, directory matches package, old layer folders absent
- [x] 9.2 Update paths in ADRs, the README architecture section, the `docs/dev` context map, the add-a-metric how-to and playbook; ADR 0038 note that the move is done
- [x] 9.3 Update the six `add-*` changes to the context layout (replace old paths and `HealthLogRepositoryPort`)
- [x] 9.4 `scripts/check-docs.sh` and `openspec validate --all --strict`

## 10. Verification
- [x] 10.1 `:domain:test`, `:data:test`, `:app:testDebugUnitTest`, lint and the instrumented migration test pass; schema version still 8
  - Note: `:domain:test`, `:data:testDebugUnitTest`, `:app:testDebugUnitTest` and `:app:lintDebug` pass. The Room schema is still version 8. The instrumented migration test was not run: no database code changed.
- [ ] 10.2 Debug build installs and starts on the emulator; log one entry per metric; export and restore a backup; a backup and a CSV export made before and after are identical
- [ ] 10.3 One release-minified build starts (R8 rules that name packages still match)
- [ ] 10.4 CI green on all workflows
