## 0. Decisions
- [x] 0.1 Delete gesture: no swipe-to-delete; edit and delete icon buttons on each entry (Material guidelines)
- [x] 0.2 No undo Snackbar; confirmation dialog only
- [x] 0.3 Activity edit: same duration and distance inputs as Log, start time kept
- [x] 0.4 Deleting the newest entry moves the newest-anchored trend window (accepted)

## 1. Domain
- [x] 1.1 Extend `HealthLogRepositoryPort` with `updateWeight/BloodPressure/Glucose/Activity` and `deleteWeight/BloodPressure/Glucose/Activity(id)` returning `Boolean`
- [x] 1.2 Add `UpdateWeightUseCase` (validates via `WeightKg`, recomputes BMI from the profile) and `DeleteWeightUseCase`
- [x] 1.3 Add Update/Delete use cases for Blood Pressure, Glucose and Activity (reuse existing value-object validation)
- [x] 1.4 Extend `UseCasesTest` with fakes: update keeps id and profile, invalid values leave data unchanged, unknown id returns not-found, delete removes only the target

## 2. Data
- [x] 2.1 Add `@Update` and `@Query("DELETE FROM <table> WHERE id = :id")` to `WeightDao`, `BloodPressureDao`, `GlucoseDao`, `ActivityDao` (return the affected row count)
- [x] 2.2 Implement the port methods in `RoomHealthLogRepository` via `HealthLogMapper`
- [ ] 2.3 Add DAO/repository tests (instrumented or Room in-memory) for update and delete
- [x] 2.4 Confirm no schema change: database stays at version 2, no migration

## 3. Wiring
- [x] 3.1 Construct the new use cases in `HealthJournalApp`
- [x] 3.2 Pass them to `HistoryViewModel.Factory` in `MainActivity`

## 4. State management
- [x] 4.1 Add `updateWeight/BloodPressure/Glucose/Activity` and `deleteX` actions to `HistoryViewModel`; on success call `loadHistory()`, on failure set `errorMessage`
- [x] 4.2 Add `HistoryUiState` fields for the entry being edited or deleted (id and metric) so dialogs survive recomposition
- [x] 4.3 Extend `ViewModelsTest`: state and chart data refresh after update and after delete, including deleting the newest entry

## 5. UI
- [ ] 5.1 (deviation, see ADR 0012: edit dialog is self-contained, shares strings and ranges) Extract the Log screen's input fields and validation for each metric into shared composables usable by both screens
- [x] 5.2 Add an Edit icon button and a Delete icon button to every History entry card (no swipe); Edit opens a dialog pre-filled with the entry values
- [x] 5.3 Icon buttons follow Material/Android guidelines: 48 dp minimum touch target (`IconButton`), standard `Icons.Filled.Edit`/`Delete`, localized content descriptions (Dutch/English), delete tinted with the error color
- [x] 5.3b Delete opens a confirmation `AlertDialog` (localized), confirm button uses the error color
- [x] 5.4 Add English and Dutch strings: edit title, save, cancel, delete, delete-confirmation title and message per metric, saved/deleted messages
- [ ] 5.5 Verify long Dutch strings do not clip in the dialogs (see ADR 0010 layout rules)

## 6. Verification
- [ ] 6.1 `./gradlew testDebugUnitTest` passes
- [ ] 6.2 On the emulator with the Libra CSV import: edit a weight, delete the newest entry, check the chart, the stat chips and the date-range window update immediately
- [ ] 6.3 Check both light and dark themes and both languages

## 7. Documentation
- [x] 7.1 Add `docs/adr/0012-edit-and-delete-entries.md` (decisions above, trade-offs: permanent delete, REPLACE vs explicit update)
- [x] 7.2 Update the README feature list and the architecture/layer diagrams
- [ ] 7.3 Bump the version and tag when released
