## 0. Decisions
- [x] 0.1 Name "Migraine" in text, no "attack"; Log tab card, same structure as a metric (requested 2026-10-21)
- [x] 0.2 Fields: times with ongoing, intensity 0-10, head area list, own type label, symptoms, triggers, medication free text with second dose, daily activities tick, comment
- [x] 0.3 Medication stays separate from the pillbox
- [x] 0.4 Graph section "Migraine" view with calendar, heat map, timeline and duration charts; one hue, plain counts
- [x] 0.5 Sources noted (Thuisarts diary, national guideline, clinic diaries); NHG-Standaard text still to be read before the docs page is final
- [ ] 0.6 Spec review by the user before implementation starts
- [ ] 0.7 Final English and Dutch wording of the steps, lists and captions agreed

## 1. Data (`:data`) and domain (`:domain`)
- [ ] 1.1 `MigraineEntry` and enums with validation and derived values; JVM tests
- [ ] 1.2 `MigraineStatistics` pure functions; JVM tests for empty range, single entry, ongoing entry, midnight, time-of-day and bucket boundaries, week start
- [ ] 1.3 Port methods and use cases (record, update, delete, end)
- [ ] 1.4 `MigraineEntity`, DAO, mapper, `MIGRATION_7_8`, version 8, repository methods; migration test from version 7
- [ ] 1.5 CSV export and import with codes and row rejection; tests including round trip and ongoing
- [ ] 1.6 Backup snapshot and restore of `migraines`; older file and newer file tests

## 2. App (`:app`)
- [ ] 2.1 Migraine card on the Log tab
- [ ] 2.2 Step-by-step logging screen with draft, skip, save, discard confirmation
- [ ] 2.3 History rows, filter, edit, delete, "it ended"
- [ ] 2.4 Migraine view: tiles, timeline, duration, intensity, weekday, heat map, symptom and trigger counts, chart descriptions
- [ ] 2.5 Calendar and navigation to a day in History
- [ ] 2.6 Strings in English and Dutch; scan test for "attack" and "aanval"
- [ ] 2.7 Export and import entries in Profile with the unencrypted warning
- [ ] 2.8 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation
- [ ] 3.1 `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page)
- [ ] 3.2 `docs/dev/` (architecture, design, c4, testing, a Migraine feature page with the sources) and `docs/dev/how-to/add-a-metric.md` if the structure differs
- [ ] 3.3 README, CHANGELOG, screenshots page
- [ ] 3.4 Archive the change into `openspec/specs/`; release
