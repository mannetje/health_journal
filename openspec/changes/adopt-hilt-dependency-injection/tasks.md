## 0. Before starting

- [x] 0.1 `app-scoped-lock-and-activity-free-viewmodels` is archived (archived 2026-11-02, with `split-main-activity`)
- [x] 0.2 Record the clean build time, APK size and cold-start time for comparison: baseline at `c0c77dd`, clean `assembleDebug assembleRelease` 1m 7s (153 tasks executed); debug APK 36,557,684 bytes, release APK (unsigned) 31,860,534 bytes; cold start on the Pixel_7_Pro emulator, debug build, `am start -W` TotalTime 1789 to 1978 ms over 5 runs (median 1797 ms)

## 1. Set up

- [x] 1.1 Add Hilt and KSP to the version catalog and the build files (Hilt 2.58: 2.59 and later require AGP 9, the build is on AGP 8.13.2; `testDebugUnitTest :app:lintDebug` passes, 1m 29s)
- [x] 1.2 `@HiltAndroidApp` and `@AndroidEntryPoint`; `di/` module for the data pieces, preferences and singletons (three modules in `app/di/`; `HealthJournalApp` keeps its properties, now field-injected, so the factories and receivers still work until section 2; `kotlin-metadata-jvm` is forced to the Kotlin version because Hilt 2.58 bundles 2.3)
- [x] 1.3 Guardrail test: `:domain` and `:data` do not import Hilt, Dagger or `javax.inject` (`RepositoryGuardrailsTest.domainAndDataDoNotUseHiltDaggerOrJavaxInject`, also checks their build files; seen failing on a probe import)
- [x] 1.4 Check cold start: encrypted, migrating and key-lost databases (debug build on the emulator: fresh install creates the key and an encrypted database; a restart opens it; with the key files removed the recovery screen shows; a plain schema-7 database with a profile and a weight is encrypted at start and the entry shows in History; no crash in any). Warm restarts on the encrypted database took 2079 to 2218 ms, but that is not comparable with the 0.2 baseline (empty database, app lock on); 3.3 repeats the baseline conditions

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
