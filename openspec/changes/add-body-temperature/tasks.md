## 0. Decisions
- [x] 0.1 Fields: one value in degrees Celsius with one decimal (30.0 to 45.0), optional site, date and time, comment
- [x] 0.2 No assessment of any kind; no "fever" or "koorts" wording; zibs as a naming guide only
- [x] 0.3 Value stored as integer tenths of a degree
- [x] 0.4 Fahrenheit in scope as display and entry unit only (third device setting); storage, CSV and backup stay Celsius (decided 2026-10-21)
- [ ] 0.5 Spec review by the user before implementation starts
- [ ] 0.6 Open: confirm the Fahrenheit region list (US, BS, BZ, KY, PW, LR; from general knowledge)
- [ ] 0.7 Open: read zib Lichaamstemperatuur in full (value set TemperatuurTypeCodelijst) and complete `docs/dev/zib-mapping.md`; confirm site names and codes
- [ ] 0.8 Open: confirm the range 30.0 to 45.0 as an input guard
- [ ] 0.9 Open: final English and Dutch wording agreed (card, sites, notice)
- [ ] 0.10 The migration is the next free number at implementation time (current database version + 1); no fixed order between features

## 1. Domain (`:domain`) and data (`:data`)
- [ ] 1.1 `BodyTemperature`, `TemperatureSite`, validation; JVM tests (bounds 30.0 and 45.0, 29.9, 45.1, two decimals refused)
- [ ] 1.2 `TemperatureStatistics`; JVM tests for empty range, one reading, ties, rounding
- [ ] 1.3 Port methods and use cases
- [ ] 1.3a `TemperatureUnit`, region default, C/F conversion functions; JVM tests (98.6 F stores 370, 86.0 and 113.0 F bounds, 85.9 and 113.1 refused, half-up rounding)
- [ ] 1.4 Entity, DAO, mapper, migration (next free number), repository; migration test from the previous version
- [ ] 1.5 CSV export and import with row rejection; round-trip tests
- [ ] 1.6 Backup snapshot and restore of `bodyTemperatures`; older-file test

## 2. App (`:app`)
- [ ] 2.1 Temperature card and form with site and comment
- [ ] 2.2 History rows, filter chip, edit dialog, delete
- [ ] 2.3 Graph view and figures with chart descriptions
- [ ] 2.4 Strings in English and Dutch; scan test for assessment words (including fever and koorts); Profile notice check
- [ ] 2.4a Third device setting "Temperature unit" in Profile settings (System default, Celsius, Fahrenheit) with description; card, History, edit dialog and graph use it
- [ ] 2.5 Export and import entries in Profile with the unencrypted warning
- [ ] 2.6 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation and release
- [ ] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [ ] 3.1a Update `openspec/specs/units-presentation` through the archive; unit docs mention the third setting
- [ ] 3.2 `docs/dev/database.md` (ER diagram, current version, version history and the naming conventions for `body_temperatures`), other `docs/dev/` pages, `docs/dev/zib-mapping.md`, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
