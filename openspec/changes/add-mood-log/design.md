## Context

The Mood log follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`, with oxygen saturation (a small table, optional notes, dot graph) and blood pressure (optional pulse) as the closest examples. Decisions are in [ADR 0028](../../../docs/adr/0028-mood-log-without-assessment.md); wording rules come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), encryption from [ADR 0023](../../../docs/adr/0023-database-encryption-and-app-lock.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md), and the input style from [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md).

## Goals / Non-Goals

**Goals:** a five-second entry, several per day, a clear history and graph, nothing assessed, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `MoodEntry(id, profileId, timestamp, level, feelings, comment?)` with `level` an `Int` 1 to 5 validated in the constructor or a factory, and `feelings` a set of `MoodFeeling` (CALM, CONTENT, CHEERFUL, ENERGETIC, TIRED, TENSE, RESTLESS, IRRITATED, SAD, WORRIED) of at most three. No `Good` or `Bad` type, no `MoodState` derived from the level.
- Port methods on `HealthLogRepositoryPort` (record, update, delete, list, list by range) and use cases `RecordMoodEntry`, `UpdateMoodEntry`, `DeleteMoodEntry`.
- `MoodStatistics` as pure functions: entries, days with entries, average (one decimal), counts per level 1 to 5 and daily means for the chart line. Callers pass the range, the zone and "today"; no clock and no thresholds.

### 2. Scale and labels
- The scale has five steps and stores only the integer. Labels ("Very unpleasant", "Unpleasant", "Neutral", "Pleasant", "Very pleasant"; Dutch "Zeer onaangenaam", "Onaangenaam", "Neutraal", "Aangenaam", "Zeer aangenaam") are string resources. Reasons: pleasant and unpleasant describe how a moment feels (valence) and carry no verdict about the person or about what to do, where good and bad do; they are symmetric around a neutral middle; they exist in plain Dutch. Faces and emoji are rejected because they differ per device and suggest judgement.
- Five steps instead of ten or a slider: quickest to tap, easy to read as counts.
- Feeling words are a fixed set of ten, balanced between pleasant and unpleasant ones, chips with a limit of three. Free-text tags and an editable list are rejected for v1 (spelling variants, no fixed codes for export, and sensitive text on a chip row).

### 3. Data (`:data`)
- `MoodEntryEntity`, DAO, mapper, repository methods; table `mood_entries` (id, profile_id, timestamp, level, feelings, comment) indexed by profile and timestamp. `feelings` is a comma-separated text of code names; unknown names are ignored when read so an older app does not crash on newer data.
- **Database migration to the next free version**, added to the `addMigrations` list: `CREATE TABLE IF NOT EXISTS` plus the index. The migration is the next free number at implementation time (current database version + 1); the order against other metric changes is not fixed. A migration test opens the previous version with data and checks the new version, old rows and an empty table. Alternative of a JSON blob or columns on an existing table rejected in ADR 0028.
- CSV adapter with `CsvQuoting.cell` and `|` joined codes; backup snapshot and restore include `moodEntries` (feelings as a JSON array of codes).

### 4. App (`:app`)
- Log tab: `MoodCard` with five equal-width step buttons in a row (each at least 48 dp tall, two-line labels allowed, so "Zeer onaangenaam" wraps on 320 dp), a chip row for the words with an inline "up to three" note, the date and time row and the comment field. Selected state uses outline plus a check icon plus the primary tint, identical for all steps.
- History: rows, filter chip, edit dialog and delete through the existing code.
- Graph: `MoodView` in the graph section with the dot chart: Compose Canvas, fixed 1 to 5 axis, dots in `primary`, optional thin mean line toggle (preference), shared range chips, zoom and pan, minimum-data rule, and a plain figures block. Text descriptions for the chart and the figures.
- Profile notice: added sentences. Strings in `values/` and `values-nl/`, ViewModel text via `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)). A unit test scans the mood strings in both languages for assessment, advice and condition words; a second test checks that no source file contains questionnaire item text.
- No reminder, notification, widget or Mindfulness link.

### 5. Relation to Energy level
Mood and Energy level stay two separate metrics with their own tables, scales (1 to 5 pleasant, versus 0 to 10 energy), charts and figures. They have different meanings, so there is no shared field and no correlation. A later optional "daily check-in" card on the Log tab could ask for both at once and save two separate entries with the same timestamp; that is a separate later change, not part of this one.

### 6. Layer rules
Rules and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- **Perceived monitoring.** A mood log invites the expectation that the app notices a bad run. The notice, the absence of any reaction and ADR 0028 state that it does not; reviewers must hold that line for every later request (reminders, "streak", insights).
- **Colour reads as judgement.** The chart uses one hue for all dots and no colour by step.
- **Sensitive data.** Mood and a free comment are among the more sensitive entries; covered by the existing encryption, secure screen and no-notification rule. No notification text ever contains a mood value.
- **Feeling-word set is a judgement call.** Ten words, balanced; reviewed with the final wording (task 0.6). Words stay out of all figures so the set can change later without changing results.

## Migration Plan
1. Domain and tests; 2. table, migration and repository with the migration test; 3. CSV and backup with the format docs; 4. card, History, chart; 5. emulator pass with screenshots (light and dark, English and Dutch, 320 dp); 6. docs, changelog and release. The migration is the next free number at implementation time (current database version + 1); the order against other metric changes is not fixed.
Rollback: the table is additive; Room refuses a downgrade of the version, as for every migration.

## Open Questions
- Static help line in the notice: recorded default is no fixed line; still open for the final wording review.
- Final English and Dutch wording and the zib check (tasks 0.5 and 0.6).
- Whether a later change may add an optional daily reminder; out of scope here and would need its own compliance review ([ADR 0022](../../../docs/adr/0022-medication-reminders.md)).
