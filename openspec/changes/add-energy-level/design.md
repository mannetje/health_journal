## Context

Energy level is a plain metric and follows `docs/dev/how-to/add-a-metric.md` and `openspec/playbooks/add-a-metric.md`, with the oxygen saturation change ([`add-oxygen-saturation`](../add-oxygen-saturation/design.md)) as the closest structural example. Decisions are in [ADR 0032](../../../docs/adr/0032-energy-level-without-assessment.md); wording rules come from [ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md), comments from [ADR 0019](../../../docs/adr/0019-entry-comments.md), pickers from [ADR 0018](../../../docs/adr/0018-smart-input-pickers.md), localized messages from [ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md), backup from [ADR 0024](../../../docs/adr/0024-portable-encrypted-backup.md).

## Goals / Non-Goals

**Goals:** a log that takes two taps, a clear history and trend, nothing assessed, English and Dutch.
**Non-Goals:** see the proposal.

## Decisions

### 1. Scale direction and wording
0 means "no energy" and 10 means "full of energy": higher is more energy. Alternative, a fatigue-severity scale (0 no tiredness, 10 worst tiredness), rejected: it reads as a symptom-severity instrument close to clinical fatigue scales, invites "how bad" framing and would need the words tired or fatigue on the card. The energy direction matches the question "how energetic do I feel now" and keeps the card neutral. Only the two ends carry words; the numbers 1 to 9 carry none, no colour gradient is used, and no number is named low or high. The value is the person's own rating and is not a score of CIS, FSS, Borg or any instrument.

### 2. Shared 0-10 picker
The 0-10 picker is a shared UI component in the shared UI package, with the end texts, labels and accessibility text passed as parameters. It is used by Migraine intensity and by Energy. Whichever of the two changes is implemented first creates it; the other reuses it. Neither change depends on the other.

### 3. Domain (`:domain`)
- `EnergyEntry(id, profileId, timestamp, level, comment?)` with validation (whole number 0 to 10).
- Port methods on `HealthLogRepositoryPort` (record, update, delete, list) and use cases `RecordEnergy`, `UpdateEnergy`, `DeleteEnergy`.
- `EnergyStatistics` pure functions: count, average (one decimal), lowest, highest for a range. Empty range gives no figures. No clock is read.

### 4. Data (`:data`)
- `EnergyEntity`, DAO, mapper, repository methods; table `energy_levels` indexed by profile and timestamp.
- **Migration to the next free database version** (current version + 1) at implementation time, added to the `addMigrations` list: `CREATE TABLE IF NOT EXISTS` plus the index. A migration test opens a database of the previous version with rows and checks the new version, old rows and an empty new table.
- CSV adapter with `CsvQuoting.cell`; backup snapshot and restore include `energyEntries`.

### 5. App (`:app`)
- Log tab: an `EnergyCard` with the shared picker, date and time, comment counter. After saving the card clears and no number is selected (no default, so a rating is always a deliberate tap).
- History: rows show the number and the comment; filter chip, edit dialog and delete through the existing code.
- Graph: dot chart in one hue with the shared range chips, axis (fixed 0 to 10), zoom and minimum-data code; a figures block; text descriptions for screen readers.
- Profile notice: one added sentence. Strings in `values/` and `values-nl/`, ViewModel text via `UiText`; a test scans the energy strings for assessment words and for fatigue-syndrome wording.

### 6. Mood and later check-in
Mood and Energy stay separate metrics because the scales mean different things: own card, table and graph, no shared field, no combined chart, no correlation. A later optional "daily check-in" card could ask for both and save two separate entries; that is a separate change, not part of this one.

### 7. No heat map in v1
A weekday by time-of-day heat map is left out of this version. It can come later, like the Migraine one, with no data-model change: time of day is derived from the timestamp.

### 8. Layer rules
Rules and figures in `:domain` (no Android), persistence and CSV in `:data`, screens and charts in `:app`.

## Risks / Trade-offs
- A reader may expect a verdict ("you are often tired"); the notice and the label-free design are the intended behaviour.
- Another migration lengthens the upgrade path; covered by the migration test.

## Migration Plan
1. Create or reuse the shared 0-10 picker; 2. domain and tests; 3. table, migration (next free number, current database version + 1) and repository with the migration test; 4. CSV and backup with the format docs; 5. screens; 6. emulator pass with screenshots; 7. docs, changelog and release.
Rollback: the table is additive; Room refuses a downgrade of the version, as for every migration.

## Open Questions
- Final Dutch endpoint wording.
