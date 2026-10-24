## 0. Decisions
- [x] 0.1 Scale 0 "No energy" to 10 "Full of energy", numbers only between the ends (energy direction, not a fatigue-severity scale; see ADR 0032)
- [x] 0.2 Fields: level 0 to 10, date and time (default now), comment; several entries per day
- [x] 0.3 No assessment, no screening, no validated questionnaire, no advice; zibs as a naming guide only (no fatigue zib found)
- [x] 0.4 Shared 0-10 UI component (endpoints, labels, accessibility text as parameters) used by Migraine intensity and Energy; whichever is built first creates it
- [x] 0.5 No heat map in v1; possible later change, no data-model change needed
- [x] 0.6 Mood stays a separate metric; a later optional daily check-in card is a separate change
- [x] 0.7 The migration is the next free number at implementation time (current database version + 1); no fixed order between features
- [ ] 0.8 Spec review by the user before implementation starts
- [ ] 0.9 Final English and Dutch wording agreed (card, endpoints, notice)
- [ ] 0.10 Check zib Symptoom on zibs.nl when reachable and complete `docs/dev/zib-mapping.md`

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `EnergyEntry` and validation; JVM tests (0 and 10 accepted, -1 and 11 and fractions refused)
- [ ] 1.2 `EnergyStatistics`; JVM tests for empty range, one entry, ties, rounding
- [ ] 1.3 Port methods and use cases
- [ ] 1.4 Entity, DAO, mapper, migration to current version + 1, repository; migration test from the previous version
- [ ] 1.5 CSV export and import with row rejection; round-trip tests
- [ ] 1.6 Backup snapshot and restore of `energyEntries`; older-file test

## 2. App (`:app`)
- [ ] 2.0 Create or reuse the shared 0-10 picker
- [ ] 2.1 Energy card and form with the picker and comment
- [ ] 2.2 History rows, filter chip, edit dialog, delete
- [ ] 2.3 Graph view, figures and chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan test for assessment and fatigue-syndrome words; Profile notice sentence
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.2 `docs/dev/database.md` (ER diagram, current version, version history and the naming conventions for `energy_levels`), other `docs/dev/` pages, `docs/dev/zib-mapping.md`, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
