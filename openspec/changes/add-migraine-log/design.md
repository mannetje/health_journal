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
- `MigraineEntry(id, profileId, start, wokeUpWithIt, end?, intensity?, areas, painCharacters, type?, symptoms, triggers, tookSomething, medicationText?, secondDose, intensityAfter2Hours?, reliefMethods, medicationHelpedMe, reliefHelpedMe, activities?, comment?)`, with validation (end not before start, intensity 0 to 10, `intensityAfter2Hours` 0 to 10 and only allowed when `tookSomething` is true, text at most 100 characters) and derived `duration`, start day, weekday and `TimeOfDay`.
- Enums with stable codes: `HeadArea`, `MigraineType`, `MigraineSymptom`, `MigraineTrigger`, `PainCharacter`, `ReliefMethod`, `DailyActivities`. Values:
  - `MigraineSymptom`: nausea, aura, sensitive to light, sound and smell, dizziness, vertigo, movement symptoms, worse with physical activity, brain fog or trouble thinking, extreme fatigue, other. The last two are what people often note after the pain has gone; there is no phase model, they are ordinary symptoms the person can add when editing the entry later.
  - `MigraineTrigger`: sleep, stress, food, weather, menstruation, other.
  - `MigraineType`: with aura, without aura, recognized (familiar) headache, other headache, not sure; the person's own label.
  - `PainCharacter` (multi-choice): throbbing or pounding, pressing or tight, piercing. These replace the two pain wordings that were briefly listed as symptoms, so nothing is stored twice.
  - `ReliefMethod` (multi-choice, what the person did besides taking something): sleep or rest, dark and quiet room, cold pack, heat, massage, coffee or tea, other. The drink is named as a drink, not as caffeine, and there are no medicine or supplement names.
- **"This helped me" ticks:** `medicationHelpedMe` (valid only with `tookSomething`) and `reliefHelpedMe` (valid only when a relief method or personal relief tag is set) are optional, off by default, and the person's own note, like the comment. They are stored and shown on the entry as typed. The app does not count, rate, average, rank or chart them, derives nothing from them (no "worked" figure, no comparison with the 2-hour score) and never suggests setting them. Turning the related field off clears the tick from the draft.
- **Personal tags** (triggers and relief methods only): `MigraineTag(id, profileId, kind, label, archived)` with `kind` TRIGGER or RELIEF, and `MigraineEntry` also holds `customTriggerIds` and `customReliefIds` (sets of tag ids). Entries refer to tags by id, so a rename shows everywhere. Removing a tag sets `archived`: it is no longer offered on new entries but stays on, and counts in, old ones, so history never changes under the person. Validation: label 1 to 30 characters after trimming, no `|`, unique per kind and profile ignoring case (adding a duplicate returns the existing tag), at most 30 non-archived tags per kind. Use cases `AddMigraineTag`, `RenameMigraineTag`, `ArchiveMigraineTag` and a list-tags port method. Symptoms, pain characters, areas and type stay fixed. Deleting a profile removes its tags like its entries.
- `wokeUpWithIt` is a flag that marks the start time as an estimate. The start time is stored as entered. Such entries still count in totals, weekday counts and the calendar, but are left out of the time-of-day heat map and counted in a note, because the clock time is not when it began.
- `intensityAfter2Hours` is a plain stored number, the pain score the person gives two hours after taking something, valid only with `tookSomething`. It is shown next to `intensity` as entered, but the app computes no difference, effect or "it helped" figure from it anywhere. Relief methods are likewise only what the person did; the app never says what worked. The person stays free to write their own subjective view ("this helped") in the comment, which is stored unchanged.
- Port methods on a separate `MigraineRepositoryPort` (keeps `HealthLogRepositoryPort` and its fakes untouched; the Room adapter implements both): record, update, delete, list by profile and range, get by id, end an ongoing one (an update of the end time). Use cases `RecordMigraine`, `UpdateMigraine`, `DeleteMigraine`, `EndMigraine`.
- `MigraineStatistics` as pure functions: summary (migraines, days with migraine, days with something taken, migraine-free days), timeline points, duration buckets (under 4, 4 to 12, over 12 hours; ongoing counted aside), intensity buckets (0-3, 4-6, 7-10; missing counted aside), weekday counts for a given first day of the week, weekday by `TimeOfDay` grid (entries with `wokeUpWithIt` are left out and counted aside, like ongoing ones in the duration buckets), symptom, trigger, pain character and relief method counts (personal tags included, by label), and the days of a month with counts. No clock is read: callers pass the range and "today".

### 2. Data (`:data`)
- `MigraineEntity` and `MigraineDao`, table `migraines` indexed by profile and start, with the columns `wokeUpWithIt`, `medicationHelpedMe` and `reliefHelpedMe` (integers, not null), `intensityAfter2Hours` (nullable integer), `painCharacters` and `reliefMethods` (comma-separated codes, like the other code sets). Mapper to and from the domain entry; code sets are stored as comma-separated names and unknown names are ignored on read.
- `MigraineTagEntity` and its DAO methods, table `migraine_tags` (`id`, `profileId`, `kind`, `label`, `archived`, `createdAt`) indexed by profile and kind. The entry columns `customTriggers` and `customReliefMethods` hold comma-separated tag ids; an id that does not resolve is ignored on read. Tags are never hard-deleted while the profile exists.
- **Database version 6 → 7, `MIGRATION_6_7`**, added to the `addMigrations` list: it only runs `CREATE TABLE IF NOT EXISTS` for two new tables, `migraines` and `migraine_tags` (new, so all columns are part of the create statements and no ALTER is needed), and their indexes. Read-mapping alternative considered: columns on an existing table or a JSON blob, rejected in [ADR 0025](../../../docs/adr/0025-migraine-log-data-model.md). A migration test opens a version 6 database with data and checks that it opens as version 7 with the old rows and two empty tables.
- `RoomHealthLogRepository` implements the new port methods; `DataModule` needs no new wiring beyond the DAO.
- CSV adapters write and read the migraine file with `CsvQuoting.cell`, including the new `woke_up_with_it`, `pain_character`, `intensity_after_2h` and `relief_methods` columns and the `custom_triggers` and `custom_relief_methods` columns (tag labels joined by `|`); the backup snapshot and restore include the `migraines` list with the same fields and a `migraineTags` list.

### 3. App (`:app`)
- Log tab: Migraine is a tab of the Log screen like the other metrics (as built), with a `MigraineCard` holding the button and the latest entry as text. A new entry's form has no title, because the tab already says what is logged; editing shows "Edit migraine".
- Logging: a `MigraineLogScreen` with a step state held in a ViewModel (the current step, the draft, a discard confirmation). Nine steps, kept at nine on purpose: time (with a "Woke up with it" switch), intensity, where and what kind (head areas and pain character on one screen), type, symptoms, triggers, medication, what I did (relief methods and the carry-on or had-to-stop choice on one screen), comment. Steps are small composables sharing one scaffold with Back, Skip, Next and Save. The medication text field uses no suggestions (`KeyboardOptions` with auto-correct off and no autofill) so no medicine names are offered. On the triggers and what-I-did steps an "Add my own" chip opens a small dialog with one text field under the same no-suggestions settings; saving adds the tag and selects it. A long-press on a personal chip, and a TalkBack custom action, opens rename and remove. When the took-something tick is on, the medication step also shows an optional "intensity after 2 hours" 0 to 10 picker; turning the tick off clears that value from the draft. The picker is the shared 0 to 10 component used for intensity (see [`add-energy-level`](../add-energy-level/design.md)); whichever feature is built first creates it.
- History: migraine rows in the merged timeline and filter (a row says "Started during sleep" when the woke-up flag is set and shows the 2-hour score as a plain number when there is one); edit hosts the same step screen in place of the History list (as built); the "it ended" action ends the entry at the current time, and the time can be changed by editing it. Month headers, timeline navigation and delete follow the existing code.
- Graphs: a `MigraineGraphSection` behind the graph icon of the History Migraine filter, with the 7, 30, 90 days and All range chips (as built; the Graph and Calendar toggle and the calendar are task 2.5); bars and heat map cells use one hue (`secondary`, at several alphas; no second tone for other headache yet) and an accessibility description per chart. The calendar is a month grid; tapping a marked day navigates to History with the Migraine filter and the day.
- Strings in `values/` and `values-nl/`, ViewModel text through `UiText` ([ADR 0015](../../../docs/adr/0015-localized-viewmodel-messages.md)). A unit test scans both string files for "attack" and "aanval".

### 4. Layer rules
Entry, rules and statistics are in `:domain` with no Android imports; the table, mapper, repository, CSV and backup code are in `:data`; screens and charts are in `:app`.

### 5. Agreed wording (English and Dutch, 2026-10-24)
Dutch addresses the person as "je". The strings files follow this table. Words never used: attack/aanval, improved/verbeterd, effective/effectief, worked/werkte.

| Where | English | Dutch |
| --- | --- | --- |
| Log tab button | Log a migraine | Migraine registreren |
| Step 1 | Started; Woke up with it; Ended (optional); Still going | Begin; Werd ermee wakker; Einde (optioneel); Nog bezig |
| Step 2 | Pain score (0 = no pain, 10 = worst pain) | Pijncijfer (0 = geen pijn, 10 = ergste pijn) |
| Step 3 | Where and what kind | Waar en hoe voelt het |
| Pain character | Throbbing or pounding; Pressing or tight; Piercing | Kloppend; Drukkend of strak; Stekend |
| Step 4 type | With aura; Without aura; Familiar headache; Other headache; Not sure | Met aura; Zonder aura; Bekende hoofdpijn; Andere hoofdpijn; Weet ik niet |
| Step 5 symptoms | Nausea; Aura; Sensitive to light, sound or smell; Dizziness; Vertigo; Movement symptoms (such as numbness); Worse with physical activity; Brain fog or trouble thinking; Extreme fatigue; Other | Misselijkheid; Aura; Gevoelig voor licht, geluid of geur; Duizeligheid; Draaiduizeligheid; Bewegingsklachten (bijvoorbeeld gevoelloosheid); Erger bij lichamelijke inspanning; Hersenmist of moeite met denken; Extreme vermoeidheid; Anders |
| Step 6 triggers | Sleep; Stress; Food; Weather; Menstruation; Other; Add my own | Slaap; Stress; Eten; Weer; Menstruatie; Anders; Zelf toevoegen |
| Step 7 medication | I took something; What I took; I took a second dose; Pain after 2 hours (0-10); This helped me | Ik heb iets ingenomen; Wat heb ik ingenomen; Tweede dosis ingenomen; Pijn na 2 uur (0-10); Dit heeft mij geholpen |
| Step 8 what I did | Sleep or rest; Dark and quiet room; Cold pack; Heat; Massage; Coffee or tea; Other; I could carry on; I had to stop; This helped me | Slapen of rusten; Donkere, stille kamer; Koud kompres; Warmte; Massage; Koffie of thee; Anders; Ik kon doorgaan; Ik moest stoppen; Dit heeft mij geholpen |
| History row | Started during sleep | Begonnen tijdens de slaap |
| Count caption | 4 migraines started on Monday | 4 migraines begonnen op maandag |
| Heat map note | 1 started during sleep | 1 begonnen tijdens de slaap |
| Tag dialog | Add my own; Rename; Remove from my list; Renaming also changes your older entries | Zelf toevoegen; Hernoemen; Van mijn lijst halen; Hernoemen verandert ook je oudere registraties |

## Risks / Trade-offs
- The more fields, the more form and test code; mitigated by every step being optional and by shared step composables.
- A very full history makes the heat map dense; it shows counts only and scrolls horizontally on a 320 dp phone if needed.
- The comma-separated code columns make per-symptom SQL queries hard; counts are made in memory, which is fine for one profile.
- Personal tags are free text: the person may type anything, including a medicine name. The no-medicine-names rule is about what the app offers, as for "what I took"; the app never suggests, ranks or interprets a tag.
- A rename changes old entries because they refer by id; that is intended and the dialog says so. Removing never changes old entries.
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
