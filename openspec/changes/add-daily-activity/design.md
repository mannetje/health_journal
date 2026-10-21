## Context

Daily activity follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`. It differs from the other metrics in one way: the key is a date, not a timestamp, and there is at most one entry per date. Decisions are in [ADR 0031](../../../docs/adr/0031-daily-activity-typed-by-hand-without-targets.md); wording rules come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md). Oxygen saturation ([ADR 0026](../../../docs/adr/0026-oxygen-saturation-without-assessment.md)) is the model for "no assessment".

## Goals / Non-Goals

**Goals:** a quick daily note of steps and/or active minutes, a clear history and bar graph, no target, no sensor, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `ActivityDay(id, profileId, date: LocalDate, steps?, activeMinutes?, type?, comment?)` with validation (at least one number; steps 0 to 100000; minutes 0 to 1440; date not after today, with the clock passed in) and `ActivityType` (WALKING, CYCLING, SWIMMING, SPORT, HOUSEWORK_GARDEN, OTHER) with stable codes.
- Port methods on `HealthLogRepositoryPort` (find by date, save, update, delete, list) and use cases `SaveActivityDay` (insert or update by date), `UpdateActivityDay`, `DeleteActivityDay`.
- `ActivityStatistics` per measure: average per day logged (rounded whole), highest day and its date, days logged, total. Days without the measure are skipped, never zero. No thresholds, no clock.

### 2. Data (`:data`)
- `ActivityDayEntity`, DAO, mapper, repository methods; table `daily_activities` with date as epoch day and a **unique index on (profile id, date)**.
- **Database migration to the next free version** (current version + 1), added to the `addMigrations` list: `CREATE TABLE IF NOT EXISTS` plus the unique index. A migration test opens a database of the previous version with rows and checks the new version, old rows and an empty new table. Unknown type codes read from the database map to none.
- CSV adapter with `CsvQuoting.cell`; backup snapshot and restore include `activityDays`.

### 3. App (`:app`)
- Log tab: an `ActivityCard` with date, steps, minutes and type fields, the comment counter. Number fields are digit-only. When the date changes, the card looks up the entry for that date and loads it (decision 4).
- History: rows, filter chip, edit dialog and delete through the existing code. The row shows steps and minutes as plain text with the type.
- Graph: bar chart in one hue with the shared range, axis, zoom and minimum-data code; a Steps / Active minutes switch; axis from 0; figures block; text descriptions for screen readers.
- Profile notice: the existing sentence also covers activity. Strings in `values/` and `values-nl/`, ViewModel text via `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)); a test scans the activity strings for goal and assessment words.

### 4. One entry per day: update, not add
A second save for the same date updates the entry; it does not add a second one or sum them. Reasons: the metric is a day total typed from a watch or pedometer, which is itself a running total (the evening figure replaces the morning one); two entries per day would need a sum rule that hides data or double counts; the bar graph and every figure stay one value per day; and the unique index makes the rule a database guarantee. The cost is that two separate walks need the person to type the total, and one activity type per day; the comment can say more. The form shows that it is updating (a plain "Entry for this day loaded" line) so nothing is overwritten silently. Rejected: add entries and sum them (double counting when typing a running total, a more complex edit dialog); ask "replace?" (a dialog on every second save).

### 5. Activity type: fixed list, not free text
A short fixed list with stable codes keeps CSV and backup clean, translates, and is easy to scan. Free text would need normalisation, a length rule and a second comment-like field; the comment already covers free text. The list is deliberately small and neutral (no intensity). Rejected for version 1: free text; a long list of sports.

### 6. Date, not timestamp
The entry stores a local calendar date (epoch day in the database, ISO date in CSV and backup), not a time. In the merged History list it sorts at 00:00 local on that date. The date cannot be in the future. Changing the device time zone does not move entries.

### 7. Graph and figures
One measure at a time avoids two axes. The axis starts at 0 because bars encode length. Average is over days logged, not calendar days, so a gap is not read as an idle day; the figure caption does not say so in words that judge, it only states "per day logged". No goal line and no colour change by value (see ADR 0031).

### 8. No sensors, no permission
No step counting, Health Connect, Google Fit or `ACTIVITY_RECOGNITION`. The manifest has no INTERNET permission and `PrivacyChecksTest` enforces the permission and logging limits; this change keeps it green without edits.

### 9. Layer rules
Rules and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- Users may expect a goal or a ring because other apps have them; the notice and the plain design are the intended behaviour.
- One entry per day loses a split of several activities; accepted for version 1.
- Another migration adds a row to the upgrade path; covered by the migration test.

## Migration Plan
The migration number is the next free one at implementation time (current database version + 1). 1. Domain and tests; 2. table, migration and repository with the migration test; 3. CSV and backup with the format docs; 4. screens; 5. emulator pass with screenshots; 6. docs, changelog and release.
Rollback: the table is additive; Room refuses a downgrade of the version, as for every migration.

## Open Questions
- Decided: no personal goal for now (own later change with a compliance review).
- Decided: fixed activity type list; free text only as a possible later change.
- Final Dutch wording is for the wording review.
