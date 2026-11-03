## 0. Decisions
- [x] 0.1 Own small release (requested 2026-10-21); Migraine has shipped, so the migration is 7 → 8
- [x] 0.2 Fields: percent 50 to 100, optional pulse, optional context, optional site, optional oxygen-given tick, date and time, comment
- [x] 0.3 No assessment of any kind; zibs as a naming guide only
- [x] 0.3a Dismissible factual form tip kept (confirmed 2026-10-21)
- [x] 0.4 Specs updated for Hilt, the metric order and migration 7 → 8; implementation requested 2026-11-03
- [x] 0.5 Read zib O2Saturatie in full and complete `docs/dev/zib-mapping.md`
- [x] 0.6 Final English and Dutch wording agreed (card, contexts, tip, notice)
- [x] 0.7 Measurement site and "oxygen given" added as optional notes (requested 2026-10-21)

## 1. Domain (`:domain`) and data (`:data`)
- [x] 1.1 `OxygenSaturation`, `OxygenContext`, `OxygenSite`, validation; JVM tests
- [x] 1.2 `OxygenStatistics`; JVM tests for empty range, one reading, ties, rounding
- [x] 1.3 Port methods and use cases
- [x] 1.4 Entity, DAO, mapper, the migration 7 → 8, repository; migration test from the previous version
- [x] 1.5 CSV export and import with row rejection; round-trip tests
- [x] 1.6 Backup snapshot and restore of `oxygenSaturations`; older-file test

## 2. App (`:app`)
- [x] 2.0 `UseCaseModule` `@Provides` per use case, `EntryUseCases`, `MetricSlot`, `MetricType` and `HistoryFilter` entries; ViewModel `@Inject` constructors
- [x] 2.1 Oxygen card and form with tip and comment
- [x] 2.2 History rows, filter chip, edit dialog, delete
- [x] 2.3 Graph view and figures with chart descriptions
- [x] 2.4 Strings in English and Dutch; scan test for assessment words; Profile notice sentence
- [x] 2.5 Export and import entries in Profile with the unencrypted warning
- [x] 2.6 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [x] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [x] 3.2 `docs/dev/database.md` (ER diagram, current version, version history and the naming conventions for `oxygen_saturations`), other `docs/dev/` pages, `docs/dev/how-to/add-a-metric.md` if needed, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
