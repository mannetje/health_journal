## 0. Decisions
- [x] 0.1 Metric: manual sleep log, duration computed, no judgement (requested 2026-10-21)
- [x] 0.2 Fields: bed time, wake time, optional quality 1 to 5, optional times woken 0 to 30, comment
- [x] 0.3 No assessment of any kind; no targets, bands, scores or advice
- [x] 0.4 Night belongs to the day of waking
- [ ] 0.5 Spec review by the user before implementation starts
- [ ] 0.6 Final English and Dutch wording agreed (card, fields, notice)
- [ ] 0.7 Read zib Slaapfunctie in full (zibs.nl returned 503) and complete `docs/dev/zib-mapping.md`
- [x] 0.8 24-hour maximum between bed and wake time kept as a typo guard, not a clinical limit (decided 2026-10-21)
- [x] 0.9 Several sessions on one date allowed; a nap is another entry (decided 2026-10-21)
- [ ] 0.10 At implementation time use the next free migration number (current database version + 1, `MIGRATION_N_(N+1)`); the change is independent of the order of other features

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `SleepLog`, validation, derived duration; JVM tests
- [ ] 1.2 `SleepStatistics` including circular mean; JVM tests for empty range, one night, over midnight, daylight saving, two entries on one day
- [ ] 1.3 Default times function; port methods and use cases
- [ ] 1.4 Entity, DAO, mapper, `MIGRATION_N_(N+1)`, repository; migration test from version N
- [ ] 1.5 CSV export and import with row rejection; round-trip tests
- [ ] 1.6 Backup snapshot and restore of `sleepLogs`; older-file test

## 2. App (`:app`)
- [ ] 2.1 Sleep card and form with live duration and comment
- [ ] 2.2 History rows, filter chip, edit dialog, delete
- [ ] 2.3 Bar graph and figures with chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan test for assessment, target and diagnosis words; Profile notice sentence
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.2 `docs/dev/` pages, `docs/dev/zib-mapping.md`, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
