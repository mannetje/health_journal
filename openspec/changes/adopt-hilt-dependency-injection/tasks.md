## 0. Before starting

- [ ] 0.1 `app-scoped-lock-and-activity-free-viewmodels` is archived
- [ ] 0.2 Record the clean build time, APK size and cold-start time for comparison

## 1. Set up

- [ ] 1.1 Add Hilt and KSP to the version catalog and the build files
- [ ] 1.2 `@HiltAndroidApp` and `@AndroidEntryPoint`; `di/` module for the data pieces, preferences and singletons
- [ ] 1.3 Guardrail test: `:domain` and `:data` do not import Hilt, Dagger or `javax.inject`
- [ ] 1.4 Check cold start: encrypted, migrating and key-lost databases

## 2. ViewModels, one at a time

- [ ] 2.1 `ProfileViewModel` (smallest): `@HiltViewModel`, delete its factory, run tests
- [ ] 2.2 `MigraineLogViewModel`, `MedicationViewModel`
- [ ] 2.3 `LoggingViewModel`, `HistoryViewModel`
- [ ] 2.4 `BackupViewModel` and the recovery instance for the lost-key screen
- [ ] 2.5 Receivers through an `@EntryPoint`

## 3. Verify

- [ ] 3.1 Unit tests and lint pass
- [ ] 3.2 Emulator: backup create and restore, language change, a reminder fires, cold start with the lock on
- [ ] 3.3 Compare build time, APK size and cold start with 0.2; if out of proportion, stop and use the `AppContainer` fallback
- [ ] 3.4 Update the architecture and conventions docs and the add-a-metric guide and playbook; accept the ADR
