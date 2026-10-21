## Context

Body temperature is a plain metric and follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`, mirroring `add-oxygen-saturation` (single value, optional site). Decisions are in [ADR 0029](../../../docs/adr/0029-body-temperature-without-assessment.md); the no-assessment pattern comes from [ADR 0026](../../../docs/adr/0026-oxygen-saturation-without-assessment.md), wording from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md).

## Goals / Non-Goals

**Goals:** a quick log with one number and an optional site, a clear history and trend, nothing assessed, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Domain (`:domain`)
- `BodyTemperature(id, profileId, timestamp, tenthsCelsius, site?, comment?)`. The value is kept as an integer number of tenths of a degree (370 means 37.0), so there is no floating-point drift in storage, CSV or averages. Validation: 300 to 450 tenths; input with more than one decimal is refused, not rounded.
- `TemperatureSite` (EAR, MOUTH, ARMPIT, FOREHEAD, RECTAL) with stable codes.
- Port methods on `HealthLogRepositoryPort` and use cases `RecordBodyTemperature`, `UpdateBodyTemperature`, `DeleteBodyTemperature`.
- `TemperatureStatistics`: lowest, highest, average (one decimal, half up on the tenths sum) and count for a range. No thresholds, no clock, no per-site split.

### 2. Data (`:data`)
- `BodyTemperatureEntity`, DAO, mapper, repository methods; table `body_temperatures` indexed by profile and timestamp.
- **One Room migration, `MIGRATION_<n>_<n+1>` with n the current database version at implementation time**, added to `addMigrations`: `CREATE TABLE IF NOT EXISTS` plus the index. A migration test opens the previous-version database with rows and checks the new version, old rows and an empty new table. An unknown site code read from the database maps to none.
- CSV adapter with `CsvQuoting.cell`; value written as degrees with one decimal and a point (`37.2`), independent of region. Backup snapshot and restore include `bodyTemperatures`.

### 3. App (`:app`)
- Log tab: a `TemperatureCard` with the value (smart input picker, [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md), comma or point accepted per `units-presentation`), the site choice, date and time, and comment with counter. No tip text (nothing factual is needed beyond the site note).
- History: rows, filter chip, edit dialog and delete through the existing code.
- Graph: dot chart in one hue with the shared range, axis, zoom and minimum-data code; value axis from the lowest reading minus 0.5 to the highest plus 0.5 degrees; figures block; text descriptions for screen readers.
- Profile notice: the oxygen sentence already says the app records and does not assess; the text is checked to cover temperature. Strings in `values/` and `values-nl/`, ViewModel text via `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)); a test scans the temperature strings for assessment words, including "fever" and "koorts".
- Display and entry follow a third device setting, `TemperatureUnit` (SYSTEM, CELSIUS, FAHRENHEIT), persisted like the other two and shown in the Profile settings next to them; the System default resolves by region in the same helper as the other units. One decimal in the active region separator, symbol "°C" or "°F". Conversion lives in a pure `:domain` function used only at the UI boundary: display F = round half up of (tenths / 10 x 9 / 5 + 32) to one decimal; entered F becomes tenths of C by round half up of ((F - 32) x 5 / 9 x 10), and the 300 to 450 tenths check runs on the converted value (86.0 to 113.0 F). Fahrenheit input with more than one decimal is refused. The graph axis, figures and History rows convert for display only; statistics are computed on stored tenths.

### 4. Layer rules
Rules and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- A reader may expect a fever marker; the notice and the label-free design handle that and are the intended behaviour.
- Readings from different sites differ by site; the figures mix them. Accepted: the site is shown on each row and the app does not correct or split. A per-site split can be a later change.
- The entry range 30.0 to 45.0 is a typo guard and could be read as a claim; it is described as an input limit only.
- Another migration on the upgrade path; covered by the migration test.
- Converting at the UI boundary means a Fahrenheit value typed and shown back can differ by 0.1 from what was typed (rounding through tenths of C); accepted and the same kind of effect as the lb to kg conversion.

## Migration Plan
0. The migration takes the next free number at implementation time (current database version + 1).
1. Domain and tests; 2. table, migration and repository with the migration test; 3. CSV and backup with the format docs; 4. screens; 5. emulator pass with screenshots; 6. docs, changelog and release.
Rollback: the table is additive; Room refuses a downgrade, as for every migration.

## Open Questions
- Region list for the Fahrenheit System default (proposal).
- Site names and codes against the zib value set once read in full.
