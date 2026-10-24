## 0. Decisions
- [x] 0.1 Own small release (requested 2026-10-21); Migraine is built first (2026-10-24), so this change takes the next free migration number
- [x] 0.2 Fields: percent 50 to 100, optional pulse, optional context, optional site, optional oxygen-given tick, date and time, comment
- [x] 0.3 No assessment of any kind; zibs as a naming guide only
- [x] 0.3a Dismissible factual form tip kept (confirmed 2026-10-21)
- [ ] 0.4 Spec review by the user before implementation starts
- [ ] 0.5 Read zib O2Saturatie in full and complete `docs/dev/zib-mapping.md`
- [ ] 0.6 Final English and Dutch wording agreed (card, contexts, tip, notice)
- [x] 0.7 Measurement site and "oxygen given" added as optional notes (requested 2026-10-21)

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `OxygenSaturation`, `OxygenContext`, `OxygenSite`, validation; JVM tests
- [ ] 1.2 `OxygenStatistics`; JVM tests for empty range, one reading, ties, rounding
- [ ] 1.3 Port methods and use cases
- [ ] 1.4 Entity, DAO, mapper, the new migration (next free number), repository; migration test from the previous version
- [ ] 1.5 CSV export and import with row rejection; round-trip tests
- [ ] 1.6 Backup snapshot and restore of `oxygenSaturations`; older-file test

## 2. App (`:app`)
- [ ] 2.1 Oxygen card and form with tip and comment
- [ ] 2.2 History rows, filter chip, edit dialog, delete
- [ ] 2.3 Graph view and figures with chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan test for assessment words; Profile notice sentence
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.2 `docs/dev/database.md` (ER diagram, current version, version history and the naming conventions for `oxygen_saturations`), other `docs/dev/` pages, `docs/dev/how-to/add-a-metric.md` if needed, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
