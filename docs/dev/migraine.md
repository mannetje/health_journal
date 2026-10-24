# Migraine

The Migraine log is a personal record of headaches and migraines. It is not a medical device: it gives no advice, makes no diagnosis, predicts nothing and never compares or ranks. The intensity, the 2-hour score and the "this helped me" ticks are the person's own notes and are shown back as entered, never counted or interpreted. The decisions are in [ADR 0025](../adr/0025-migraine-log-data-model.md); the requirements are in the OpenSpec change `openspec/changes/add-migraine-log/`.

## What a person can do

- **Log** on the Migraine tab of the Log screen: a step form (time, intensity, where, type, symptoms, triggers, medication, relief, comment). Every step is optional and Save is on every step. A new entry has no title, like the other metrics; the Migraine tab says what is being logged. An entry without an end time is ongoing.
- **Personal tags:** add your own triggers and relief methods next to the built-in ones. A tag is shared by all entries that use it, and removing it only archives it.
- **History:** the Migraine filter lists each migraine in one compact card with everything that was filled in. An ongoing entry has an "It ended now" button, which ends it at the current time (edit the entry to use another time). Edit opens the same step form in place of the list.
- **Export and import:** the Export and Import dialogs in History (the same place as the other CSV files) have a Migraine button and chip. The export holds one row per migraine; the import reports rows it skipped by line number. Both dialogs warn that CSV files are not encrypted.
- **Graph view:** the graph icon under the Migraine filter shows, for the chosen range (7, 30, 90 days or All): counts of migraines and days, a timeline of intensity, duration groups, intensity groups, weekday counts, a weekday by time-of-day heat map and counts of symptoms, triggers, pain and relief. Counts only.
- **Calendar:** a Graph and Calendar toggle sits above the cards. The calendar (`MigraineCalendarCard.kt`) is a month grid that starts on the locale's first weekday and cannot move past the current month. A day with a migraine is marked in the chart colour with the count from two up, and a small dot marks a day with something taken (`MigraineStatistics.daysWithSomethingTaken`). Tapping a marked day calls `HistoryViewModel.showMigraineDay`: the Migraine filter, the range All and a "Day" chip that `clearMigraineDay` removes.

## Where it lives

| Layer | Path |
|-------|------|
| Domain (model, statistics, use cases, ports) | `domain/src/main/kotlin/nl/healthjournal/domain/model/migraine/`, `domain/src/main/kotlin/nl/healthjournal/domain/usecase/MigraineUseCases.kt`, `domain/src/main/kotlin/nl/healthjournal/domain/port/secondary/MigraineRepositoryPort.kt`, `domain/src/main/kotlin/nl/healthjournal/domain/port/secondary/MigraineDataPorts.kt` |
| Data (tables, DAO, mappers, repository, CSV, backup, migration 6 to 7) | `data/src/main/java/nl/healthjournal/data/local/entity/MigraineEntities.kt`, `data/src/main/java/nl/healthjournal/data/local/dao/MigraineDao.kt`, `data/src/main/java/nl/healthjournal/data/repository/RoomMigraineRepository.kt`, `data/src/main/java/nl/healthjournal/data/csv/CsvMigraineExportAdapter.kt`, `data/src/main/java/nl/healthjournal/data/csv/CsvMigraineImportAdapter.kt` |
| Step form and state | `app/src/main/java/nl/healthjournal/app/ui/migraine/` (`MigraineLogScreen`, `MigraineLogViewModel`) |
| History card and graph | `app/src/main/java/nl/healthjournal/app/ui/history/MigraineHistoryCard.kt`, `app/src/main/java/nl/healthjournal/app/ui/history/charts/MigraineGraphSection.kt`, `app/src/main/java/nl/healthjournal/app/ui/history/charts/MigraineCalendarCard.kt` |

The tables are in the [database page](database.md); the file formats are in [Backup and restore](backup.md) and the README CSV table.

## How it fits together

One `MigraineLogViewModel` serves the form in both places. The Log screen shows it as a tab; `HistoryScreen` swaps its list for the form while `migraineState.open` is true, after `startEdit(entry)`. History reads entries and tags from its own view model, so the card and the graph show the same data as the form saved. See the slice diagram in [C4](c4.md#the-migraine-slice-across-the-modules).

- **Derived, never stored:** duration, weekday and time of day (night, morning, afternoon, evening) come from the start and end times in `:domain`.
- **Statistics** are pure functions of entries, range and zone (`MigraineStatistics`, and `MigraineGraphData` in the graph file). `migraineDateRange` ends today and starts `days - 1` earlier, or at the first entry for All.
- **Set columns** hold stable code names, never translated text, so CSV, backup and database agree.

## Tests

| What | Where |
|------|-------|
| Entry rules, statistics, tags | `domain/src/test/kotlin/nl/healthjournal/domain/model/migraine/` |
| Mapper, repository, CSV, migration | `data/src/test/java/nl/healthjournal/data/` (`RoomMigraineRepositoryTest`, `MigraineCsvTest`, `MigrationTest`) |
| Form and History view models | `app/src/test/java/nl/healthjournal/app/ui/MigraineLogViewModelTest.kt`, `ViewModelsTest.kt` (fakes in `FakeMigraines.kt`), `MigraineStringsTest.kt` (strings) |

## Screens

| Step form | History list | Graph |
|-----------|--------------|-------|
| <img src="../screenshots/light-migraine-step1.png" width="200" alt="Migraine form, first step, light theme"> | <img src="../screenshots/light-migraine-history-list.png" width="200" alt="Migraine cards in History, light theme"> | <img src="../screenshots/light-migraine-history-graph.png" width="200" alt="Migraine graph view, light theme"> |
| <img src="../screenshots/dark-migraine-step1.png" width="200" alt="Migraine form, first step, dark theme"> | <img src="../screenshots/dark-migraine-history-list.png" width="200" alt="Migraine cards in History, dark theme"> | <img src="../screenshots/dark-migraine-history-graph.png" width="200" alt="Migraine graph view, dark theme"> |

## Wording rules

Never use "attack" in the app texts, never give advice, and avoid words that judge a result ("improved", "effective", "worked"). `MigraineStringsTest` keeps English and Dutch in step and fails on "attack", "aanval" and advice or verdict words.

## Extending it

A new answer option is a new enum value plus its labels in both string files; keep the code name stable. A new graph card follows the checklist in [Graphs](graphs.md). A new field changes the entity, a migration, the mapper, the CSV header, the backup document and its schema together; see [Change the database](how-to/change-the-database.md).
