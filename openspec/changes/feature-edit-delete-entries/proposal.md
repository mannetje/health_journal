# Edit and Delete Entries

## Why
Entries can only be created (Log screen) or bulk-imported (CSV). A mistyped weight, a wrong glucose context or a duplicated import row cannot be corrected or removed, and with 1000+ imported entries the user has no way to clean up. The History screen is read-only. Users need Update and Delete for every core metric: Weight, Blood Pressure, Glucose and Activity.

## What Changes
- **Domain:** extend `HealthLogRepositoryPort` with `update…()` and `delete…(id)` per metric, and add use cases `UpdateWeightUseCase`, `DeleteWeightUseCase` and the equivalents for Blood Pressure, Glucose and Activity. Updating re-validates the value through the existing value objects and recomputes derived data (BMI for weight; NHG categories are derived at read time).
- **Data:** add Room DAO `@Update` and `@Query("DELETE … WHERE id = :id")` methods for the four tables, implemented in `RoomHealthLogRepository`. No schema change, so no database migration (the database version stays at 2).
- **Presentation:**
  - **Delete:** a Delete icon button on each History entry, behind a localized (Dutch and English) confirmation dialog. No entry is removed without confirmation.
  - **Edit:** the Edit icon on an entry opens an edit dialog that reuses the Log screen's input fields and validation, pre-filled with the entry's values and timestamp, so the user can change values or date/time and save.
- **State:** `HistoryViewModel` gains `updateX()` / `deleteX()` actions that reload the history after success (it loads via `loadHistory()`, it does not observe the database). The trend charts and stat chips are derived from `HistoryUiState`, so they refresh immediately.
- No new dependencies.

## Capabilities
- **Added Capability:** `history/entry-management`
- **Modified Capabilities:** `domain/health-log` (repository port), `history/list` (entries are tappable)

## Impact
- Affected code: `domain` (`HealthLogRepositoryPort`, new use cases, `UseCasesTest`), `data` (four DAOs, `RoomHealthLogRepository`), `app` (`HealthJournalApp` wiring, `HistoryViewModel`, `HistoryScreen`, shared input components from `LogMetricScreen`, `strings.xml` and `values-nl/strings.xml`).
- The existing `insert(... REPLACE)` on the primary key already behaves like an update. This proposal still adds explicit `update` methods so a missing id is an error and not a silent insert.
- Deleting is permanent (no undo, no trash). 
- Documentation: README feature list, a new ADR `0012-edit-and-delete-entries.md`, and the layer diagram in the architecture docs.

## Decisions
1. **Delete gesture:** no swipe. Edit and Delete icon buttons on each entry, following Android/Material guidelines (48 dp targets, localized content descriptions), confirmation required.
2. **Undo:** none, confirmation only.
3. **Profile link:** entries stay bound to their profile; changing owner is out of scope.
4. **Activity editing:** same duration and distance inputs as Log, start time kept.
5. **Trend anchoring:** deleting the newest entry moves the newest-anchored window; accepted.
