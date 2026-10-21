## Context

The Migraine log follows the structure of a metric (`docs/dev/how-to/add-a-metric.md`, `openspec/playbooks/add-a-metric.md`): a card on the Log tab, a form, History rows and a graph view. It differs in three ways: an optional end time (an ongoing migraine), multi-choice fields, and statistics that group by weekday and time of day. The decision record is [ADR 0025](../../../docs/adr/0025-migraine-log-data-model.md); compliance guard-rails come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), encryption from [ADR 0023](../../../docs/adr/0023-database-encryption-and-app-lock.md) and backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md).

## Goals / Non-Goals

**Goals:**
- Quick to log (start time and save), richer when there is time.
- Plain counts and calendars the person can read with a care provider, in English and Dutch.
- Old data and old backups stay readable.

**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `MigraineEntry(id, profileId, start, end?, intensity?, areas, type?, symptoms, triggers, tookSomething, medicationText?, secondDose, activities?, comment?)`, with validation (end not before start, intensity 0 to 10, text at most 100 characters) and derived `duration`, start day, weekday and `TimeOfDay`.
- Enums with stable codes: `HeadArea`, `MigraineType`, `MigraineSymptom`, `MigraineTrigger`, `DailyActivities`.
- Port methods on `HealthLogRepositoryPort`: record, update, delete, list by profile and range, get by id, end an ongoing one (an update of the end time). Use cases `RecordMigraine`, `UpdateMigraine`, `DeleteMigraine`, `EndMigraine`.
- `MigraineStatistics` as pure functions: summary (migraines, days with migraine, days with something taken, migraine-free days), timeline points, duration buckets (under 4, 4 to 12, over 12 hours; ongoing counted aside), intensity buckets (0-3, 4-6, 7-10; missing counted aside), weekday counts for a given first day of the week, weekday by `TimeOfDay` grid, symptom and trigger counts, and the days of a month with counts. No clock is read: callers pass the range and "today".

### 2. Data (`:data`)
- `MigraineEntity` and `MigraineDao`, table `migraine_entries` indexed by profile and start. Mapper to and from the domain entry; code sets are stored as comma-separated names and unknown names are ignored on read.
- **Database version 6 → 7, `MIGRATION_6_7`**, added to the `addMigrations` list: it only runs `CREATE TABLE IF NOT EXISTS` and the index. Read-mapping alternative considered: columns on an existing table or a JSON blob, rejected in [ADR 0025](../../../docs/adr/0025-migraine-log-data-model.md). A migration test opens a version 6 database with data and checks that it opens as version 7 with the old rows and an empty table.
- `RoomHealthLogRepository` implements the new port methods; `DataModule` needs no new wiring beyond the DAO.
- CSV adapters write and read the migraine file with `CsvQuoting.cell`; the backup snapshot and restore include the `migraines` list.

### 3. App (`:app`)
- Log tab: a `MigraineCard` after the metric cards with the button and the latest entry as text.
- Logging: a `MigraineLogScreen` with a step state held in a ViewModel (the current step, the draft, a discard confirmation). Steps are small composables sharing one scaffold with Back, Skip, Next and Save. The medication text field uses no suggestions (`KeyboardOptions` with auto-correct off and no autofill) so no medicine names are offered.
- History: migraine rows in the merged timeline and filter; edit uses the same step screen with an existing entry; the "it ended" action opens a time picker. Month headers, timeline navigation and delete follow the existing code.
- Graphs: a `MigraineView` in the graph section with the Graph and Calendar toggle and the range chips; charts are Compose Canvas with a single hue (`primary` at several alphas, with a second tone for other headache) and an accessibility description per chart. The calendar is a month grid; tapping a marked day navigates to History with the Migraine filter and the day.
- Strings in `values/` and `values-nl/`, ViewModel text through `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)). A unit test scans both string files for "attack" and "aanval".

### 4. Layer rules
Entry, rules and statistics are in `:domain` with no Android imports; the table, mapper, repository, CSV and backup code are in `:data`; screens and charts are in `:app`.

## Risks / Trade-offs
- The more fields, the more form and test code; mitigated by every step being optional and by shared step composables.
- A very full history makes the heat map dense; it shows counts only and scrolls horizontally on a 320 dp phone if needed.
- The comma-separated code columns make per-symptom SQL queries hard; counts are made in memory, which is fine for one profile.
- Wording is the main compliance risk, so the wording rules are enforced by a test and by the spec.

## Migration Plan
1. Add the table and `MIGRATION_6_7`, with the migration test.
2. Domain model, statistics and tests, then the repository and CSV.
3. Backup list and the `docs/backup-format/` changes with the shared test vectors.
4. Screens: card, logging steps, History, Migraine view, calendar.
5. Emulator pass (English and Dutch, light and dark, 320 dp) with screenshots, then docs, changelog and release.
Rollback: the table is additive; reverting the app leaves an unused table that an older app version ignores (Room would refuse a downgrade of the version, which is the existing behaviour for all migrations).

## Open Questions
- None blocking. Later: the drawn head map, reminders, a link to the pillbox, and a doctor-appointment summary page.
