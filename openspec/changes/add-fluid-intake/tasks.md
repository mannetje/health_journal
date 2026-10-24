## 0. Decisions
- [x] 0.1 Metric: fluid intake, volume only; quick-add taps plus custom ml entry; each tap is one entry
- [x] 0.2 Range 10 to 5000 ml, whole ml; comment 200 chars; default sizes 150, 250, 500 stored in preferences, editable
- [x] 0.3 The app sets no target, reminder, ring, verdict or advice; no recommended amount copied from any source
- [x] 0.4 Daily total of the local calendar day on the Log card; bars of day totals in the graph; average over logged days
- [x] 0.5 Type of drink NOT in version 1; planned later as an optional type (default water) via a later migration, nullable column read as water when empty
- [x] 0.6 Optional personal limit in version 1, 100 to 10000 ml, set only by the person, no default or hint, per profile in `fluid_settings`, in the backup as `fluidDailyLimitMl`, not in the CSV; plain numbers and one dashed line only
- [ ] 0.7 Spec review by the user before implementation starts
- [ ] 0.8 Read the zib Vochtbalans in full (pages answered 503 during drafting) and complete `docs/dev/zib-mapping.md`
- [ ] 0.9 Final English and Dutch wording agreed (card, button names, Today line, Undo, notice)
- [ ] 0.10 The migration is the next free number at implementation time (current database version + 1); build order is the user's choice

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `FluidIntake`, validation, `QuickAddSizes`; JVM tests
- [ ] 1.2 `FluidDayTotals` and `FluidStatistics` with injected zone and clock; JVM tests for empty range, one entry, midnight boundary, time zone, rounding, unlogged days
- [ ] 1.3 Port methods and use cases (record, update, delete, list, range)
- [ ] 1.4 Entity, DAO, mapper, `fluid_settings`, one migration from the current version N to N+1, repository; migration test from version N
- [ ] 1.5 CSV export and import (`timestamp,ml,comment`) with row rejection; round-trip tests
- [ ] 1.6 Backup snapshot and restore of `fluidEntries` and `fluidDailyLimitMl`; older-file and null-limit tests
- [ ] 1.6a Limit use cases (set, change, clear) with validation 100 to 10000; JVM tests that entries stay unchanged
- [ ] 1.7 Preferences for quick-add sizes with defaults and validation; test that no entry data is stored in preferences

## 2. App (`:app`)
- [ ] 2.1 Fluid card: quick-add buttons, custom entry form, Today line (with and without the limit), size settings, limit field with clear, Undo
- [ ] 2.2 History rows, day-total rows under the Fluid filter, filter chip, edit dialog, delete
- [ ] 2.3 Bar graph view, dashed limit line and figures with chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan test that bans assessment, advice, target and verdict words (over, exceeded, remaining, left) while allowing the person's own limit number; Profile notice sentence
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 Accessibility: 48 dp targets, content descriptions with the amount, Undo announced
- [ ] 2.7 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.2 `docs/dev/database.md` (ER diagram, current version, version history and the naming conventions for `fluid_intakes` and `fluid_settings`), other `docs/dev/` pages, `docs/dev/how-to/add-a-metric.md` if needed, `docs/dev/zib-mapping.md`, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
