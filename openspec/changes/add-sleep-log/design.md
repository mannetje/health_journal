## Context

The Sleep log follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`, with Oxygen saturation (optional fields, plain figures) as the closest example and the Migraine log for start and end times. It differs in that the entry holds two instants, the duration is derived, and the graph is a bar per night. Decided: the night belongs to the local date of waking; several sessions per date are allowed; the 24-hour maximum is a typo guard. Decisions are in [ADR 0027](../../../docs/adr/0027-sleep-log-without-assessment.md); wording rules come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), pickers from [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md).

## Goals / Non-Goals

**Goals:** a quick log (two times and Save), a clear history and trend, nothing judged, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `SleepLog(id, profileId, bedTime, wakeTime, quality?, timesWoken?, comment?)` with validation (wake after bed, at most 24 hours, quality 1 to 5, times woken 0 to 30) and derived `duration` (`Duration` between the instants). Duration is never stored.
- Port methods on `HealthLogRepositoryPort` (record, update, delete, list) and use cases `RecordSleepLog`, `UpdateSleepLog`, `DeleteSleepLog`.
- `SleepStatistics` as pure functions taking the entries, a range and a `ZoneId`: nights per day of waking, average, shortest, longest, count, circular mean of bed and wake clock times. No thresholds, no clock read. The circular mean uses the unit vector sum of the minutes of the day; a zero resultant gives no mean.
- Day assignment is by the wake time in the given zone. Default form times (23:00 yesterday, 07:00 today) are computed by a small function taking today's date and a zone, for tests.

### 2. Data (`:data`)
- `SleepLogEntity`, DAO, mapper, repository methods; table `sleep_sessions` indexed by profile and wake time. Times stored as epoch instants like the other timestamps.
- **Room migration to the next free version**, added to the `addMigrations` list: `CREATE TABLE IF NOT EXISTS` plus the index. The migration is the next free number at implementation time (current database version + 1, written `MIGRATION_N_(N+1)` in the implementation). A migration test opens a database of version N with rows and checks version N+1, old rows and an empty new table.
- CSV adapter with `CsvQuoting.cell`, ISO-8601 UTC; backup snapshot and restore include `sleepLogs`.

### 3. App (`:app`)
- Log tab: a `SleepCard` with the two pickers (the smart input pickers of [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md)), the duration text updating live, five quality choices (none selected), a number field for times woken and the comment counter.
- History: rows with the duration text, filter chip, edit dialog and delete through the existing code.
- Graph: a bar chart in one hue with the shared range, axis, zoom and minimum-data code, a value axis from 0 hours; a figures block; text descriptions for screen readers.
- Strings in `values/` and `values-nl/`, ViewModel text via `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)); a test scans the sleep strings for assessment, target and diagnosis words in both languages.

### 4. Layer rules
Rules and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- A reader may expect a "recommended" line; the notice and the absence of any line are the intended behaviour.
- Time zone changes and daylight saving: duration uses instants and days use the current device zone, so travelling can move an old night to another day; accepted and documented.
- The circular mean can be undefined for evenly spread times; shown as "-".
- Overlapping entries are not rejected and several sessions on one date are allowed (a nap is another entry); the figures count what was entered.

## Migration Plan
1. Domain and tests; 2. table, migration (next free version, see above) and repository with the migration test; 3. CSV and backup with the format docs; 4. screens; 5. emulator pass with screenshots; 6. docs, changelog and release.
Rollback: the table is additive; Room refuses a downgrade of the version, as for every migration.

## Open Questions
- Zib Slaapfunctie could not be read; confirm "no matching zib" later.
