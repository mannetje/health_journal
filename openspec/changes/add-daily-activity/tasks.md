## 0. Decisions
- [x] 0.1 Fields: date, optional steps, optional active minutes (at least one), optional type from a fixed list, comment
- [x] 0.2 One entry per day; a second save updates (design decision 4)
- [x] 0.3 Fixed activity type list, no free text in version 1
- [x] 0.4 No target, no assessment, no sensor, no new permission
- [ ] 0.5 Spec review by the user before implementation starts
- [x] 0.6 No personal goal for now; a self-set goal would be its own later change with a compliance review
- [ ] 0.7 Final English and Dutch wording agreed (card, types, figures, notice)
- [ ] 0.8 The migration is the next free number at implementation time (current database version + 1); build order is chosen by the user
- [x] 0.10 Activity type stays a fixed list of six; free text only as a possible later change
- [ ] 0.9 Re-check zibs.nl for an activity zib when the server answers (none found so far) and complete `docs/dev/zib-mapping.md`

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `ActivityDay`, `ActivityType`, validation (at least one number, ranges, date not in the future); JVM tests
- [ ] 1.2 `ActivityStatistics`; JVM tests for empty range, one day, gaps not counted as zero, ties, rounding
- [ ] 1.3 Port methods and use cases (`SaveActivityDay` upserts by date)
- [ ] 1.4 Entity, DAO, mapper, unique index, next-free-number migration, repository; migration test from the previous version
- [ ] 1.5 CSV export and import with row rejection (duplicates, unknown code); round-trip tests
- [ ] 1.6 Backup snapshot and restore of `activityDays`; older-file test; duplicate-date test

## 2. App (`:app`)
- [ ] 2.1 Activity card and form with digit-only fields and load-on-date behaviour
- [ ] 2.2 History rows, filter chip, edit dialog (occupied date refused), delete
- [ ] 2.3 Bar graph, measure switch and figures with chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan test for goal and assessment words; Profile notice sentence
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 `PrivacyChecksTest` stays green; no new permission in the merged manifest
- [ ] 2.7 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.2 `docs/dev/` pages, `docs/dev/zib-mapping.md`, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
