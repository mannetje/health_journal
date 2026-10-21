## Context

Oxygen saturation is a plain metric and follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`, with blood pressure (optional pulse) and glucose (context) as the closest examples. Decisions are in [ADR 0026](../../../docs/adr/0026-oxygen-saturation-without-assessment.md); wording rules come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md).

## Goals / Non-Goals

**Goals:** a quick log with an optional pulse and context, a clear history and trend, nothing assessed, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `OxygenSaturation(id, profileId, timestamp, percent, pulse?, context?, comment?)` with validation (whole percent 50 to 100, pulse 30 to 250) and `OxygenContext` with stable codes.
- Port methods on `HealthLogRepositoryPort` (record, update, delete, list) and use cases `RecordOxygenSaturation`, `UpdateOxygenSaturation`, `DeleteOxygenSaturation`.
- `OxygenStatistics`: lowest, highest, average (one decimal), count and counts per context for a range. No thresholds, no clock.

### 2. Data (`:data`)
- `OxygenSaturationEntity`, DAO, mapper, repository methods; table `oxygen_saturations` indexed by profile and timestamp.
- **Database version 6 → 7, `MIGRATION_6_7`**, added to the `addMigrations` list: `CREATE TABLE IF NOT EXISTS` plus the index. Read-mapping alternative (columns on an existing table) rejected in ADR 0026. A migration test opens a version 6 database with rows and checks version 7, old rows and an empty new table. Unknown context codes read from the database map to none.
- CSV adapter with `CsvQuoting.cell`; backup snapshot and restore include `oxygenSaturations`.

### 3. App (`:app`)
- Log tab: an `OxygenCard` with the value, pulse and context fields, the comment counter and the dismissible tip (dismissal in preferences). The value picker follows the smart input pickers of the other metrics ([ADR 0018](../../../docs/adr/0018-smart-input-pickers.md)).
- History: rows, filter chip, edit dialog and delete through the existing code; timeline navigation unchanged.
- Graph: dot chart in one hue with the shared range, axis, zoom and minimum-data code; a figures block; text descriptions for screen readers.
- Profile notice: one added sentence. Strings in `values/` and `values-nl/`, ViewModel text via `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)); a test scans the oxygen strings for assessment words.

### 4. Layer rules
Rules and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- A reader may expect a "normal" marker; the notice and the empty-of-labels design handle that and are the intended behaviour.
- Another migration adds a row to the upgrade path; covered by the migration test.

## Migration Plan
1. Domain and tests; 2. table, migration and repository with the migration test; 3. CSV and backup with the format docs; 4. screens; 5. emulator pass with screenshots; 6. docs, changelog and release (own small release, before Migraine, which then moves to 7 → 8).
Rollback: the table is additive; Room refuses a downgrade of the version, as for every migration.

## Open Questions
- Whether to add the zib's measurement site and "oxygen given" later.
