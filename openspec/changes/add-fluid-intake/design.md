## Context

Fluid intake follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`, with one difference: it is a high-frequency log (many entries a day), so the quick-add tap and the day grouping matter more than the form. Decisions are in [ADR 0030](../../../docs/adr/0030-fluid-intake-quick-log-without-target.md); wording rules come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md), pickers from [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md), localized messages from [ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md).

## Goals / Non-Goals

**Goals:** a log that costs one tap, own sizes, a plain daily total, an optional personal limit set by the person, a history grouped by day and a bar graph, nothing assessed, no target set by the app, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `FluidIntake(id, profileId, timestamp, ml, comment?)` with validation (whole ml 10 to 5000; constants in one place).
- `QuickAddSizes` value object (three slots: name, ml) with the same validation and the defaults 150, 250, 500; pure, no Android.
- Port methods on `HealthLogRepositoryPort` (record, update, delete, list, list in range) and use cases `RecordFluidIntake`, `UpdateFluidIntake`, `DeleteFluidIntake`. Undo calls delete with the id returned by record.
- `FluidDayTotals`: groups entries by local calendar day using an injected `ZoneId` and `Clock` (no `ZoneId.systemDefault()` inside domain), returns per-day totals for a range, and `FluidStatistics`: average per logged day (whole ml, round half up), highest day and date, days logged. Empty range yields none. No thresholds.

### 2. Data (`:data`)
- `FluidIntakeEntity`, DAO, mapper, repository methods; table `fluid_intakes` indexed by profile and timestamp. Totals are computed in `:domain` from entries, not stored: one source of truth, and editing a timestamp can never leave a stale total.
- **One migration, `MIGRATION_N_N+1`, where N is the current database version at implementation time**, added to the `addMigrations` list: `CREATE TABLE IF NOT EXISTS` for `fluid_intakes` and `fluid_settings` plus the index. A migration test opens a database of version N with rows and checks version N+1, old rows and empty new tables.
- **Personal limit** in `fluid_settings(profileId PRIMARY KEY, dailyLimitMl NULL)`: it must be per profile and inside the encrypted database (preferences are not encrypted and the `privacy` capability forbids health data there); a nullable column on the profile table was rejected because it would change an existing table for one optional value, and a table of its own keeps the migration purely additive. A missing row or null means no limit. The limit is not in the CSV (a setting, not an entry) and is in the backup document as `fluidDailyLimitMl`.
- Quick-add sizes in the existing preferences store (three int and three string keys), with defaults when absent or invalid.
- CSV adapter with `CsvQuoting.cell`; backup snapshot and restore include `fluidEntries`.

### 3. App (`:app`)
- Log tab: a `FluidCard` with three quick-add buttons, a custom entry action (opens the form with the ml field, date and time and comment with counter, using the number picker of [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md)), the "Today" line ("Today: N ml" or "Today: N of L ml"), a limit field (empty until set, clear action), an edit-sizes action and an Undo snackbar (about 5 seconds, announced for screen readers). The Today line is a `StateFlow` recomputed from entries when the day or the entries change, and at local midnight.
- Buttons are at least 48 dp, each with a content description of the form "Add 250 ml". A double tap adds two entries by design (honest log); Undo removes only the last one.
- History: rows per entry; under the Fluid filter a day-total row above each day's entries; filter chip, edit dialog and delete through the existing code.
- Graph: bar chart of day totals in one hue with the shared range, axis, zoom and minimum-data code (axis starts at 0; one thin dashed line at the person's limit only when set; bars never change colour); a figures block; text descriptions for screen readers.
- Profile notice: one added sentence. Strings in `values/` and `values-nl/`, ViewModel text via `UiText`; a test scans the fluid strings for assessment and advice words (including "enough", "too much", "goal", "target", "over", "exceeded", "remaining", "left", "genoeg", "te veel", "doel", "overschreden"); the person's own limit number shown back is allowed.

### 4. Type of drink: later, not in version 1
Version 1 records volume only: a type would add a choice to every tap and invite nutrition framing. Planned later addition: an optional type with the default "water", added by a later migration as a nullable column that reads as water when empty, with no nutrition or caffeine framing. Old rows and backups stay valid without it.

### 5. Why a personal limit, how it stays neutral
A person on a medical fluid restriction compares the total with a number from their care team all day; showing that number next to the total saves arithmetic. It is acceptable only as the person's own number: the field is empty until they fill it, with no default, hint or suggestion; the label says it is set by them or their care team. It appears as "Today: 900 of 1500 ml" and one thin dashed line in the graph's hue. Above the limit the card simply shows "1700 of 1500 ml" in the same style: no colour change, alert, notification, ring, and no "over", "exceeded", "remaining" or "left" wording. Changing or clearing it never touches entries. Risk: even a plain number can feel like a verdict; mitigated by no colour or words and by the Profile notice saying the number is the person's own.

### 6. Layer rules
Rules, grouping and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- A reader may expect a goal or ring as in other water apps; the notice and the design are the intended behaviour.
- Many entries per day make History long; the day-total rows and the graph are the overview. Delete of a wrong tap is by Undo or the History row.
- The daily total depends on the device time zone; a time zone change can regroup entries. Accepted, as for other daily figures; the day is the local day of the timestamp at display time.
- Another migration adds a row to the upgrade path; covered by the migration test.

## Migration Plan
The migration is the next free number at implementation time (current database version + 1); nothing here depends on the order in which features are built.
1. Domain and tests; 2. tables, migration and repository with the migration test; 3. CSV and backup with the format docs; 4. screens; 5. emulator pass with screenshots; 6. docs, changelog and release.
Rollback: the table is additive; Room refuses a downgrade of the version, as for every migration.

## Open Questions
- None. Type of drink is a planned later addition (section 4).
- Whether the Today line should also be shown on the Overview or a widget: not in v1.
