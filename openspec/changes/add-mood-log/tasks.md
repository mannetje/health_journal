## 0. Decisions
- [x] 0.1 Metric scope: five-step scale, optional feeling words, several entries per day, comment (drafted 2026-10-21)
- [x] 0.2 No assessment: no questionnaire, score, threshold, alert, advice, reminder or Mindfulness link in v1
- [x] 0.3 Neutral scale wording "unpleasant to pleasant" and no faces or emoji (drafted; see ADR 0028)
- [ ] 0.4 Spec review by the user before implementation starts
- [ ] 0.5 Check zibs.nl for a mood or feeling zib (none found; site returned 503; check *Functionele of Mentale Status*) and complete `docs/dev/zib-mapping.md`
- [ ] 0.6 Final English and Dutch wording agreed (steps, ten feeling words, notice sentences)
- [ ] 0.7 Static pointer to help in the notice: recorded default is none; confirm at the final wording review
- [x] 0.8 No calendar in the first version (decided); open: daily mean line default on or off
- [ ] 0.9 The migration is the next free number at implementation time (current database version + 1); the user picks the build order

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `MoodEntry`, `MoodFeeling`, validation (level 1 to 5, at most three distinct words); JVM tests
- [ ] 1.2 `MoodStatistics` (counts, average, daily means); JVM tests for empty range, one entry, several per day, rounding half up, day boundaries
- [ ] 1.3 Port methods and use cases
- [ ] 1.4 Entity, DAO, mapper, the new migration, repository; migration test from the previous version; unknown code ignored on read
- [ ] 1.5 CSV export and import with row rejection; round-trip tests
- [ ] 1.6 Backup snapshot and restore of `moodEntries`; older-file and unknown-code tests

## 2. App (`:app`)
- [ ] 2.1 Mood card and form with five steps, word chips and comment
- [ ] 2.2 History rows, filter chip, edit dialog, delete
- [ ] 2.3 Chart view with figures and chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan tests for assessment, advice and condition words and for questionnaire text; Profile notice sentences
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 Emulator pass: English and Dutch, light and dark, 320 dp, TalkBack labels for steps; screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.2 `docs/dev/` pages, `docs/dev/zib-mapping.md`, README, CHANGELOG (wording check against compliance), screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
