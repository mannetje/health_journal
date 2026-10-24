## 0. Decisions
- [x] 0.1 Name "Migraine" in text, no "attack"; Log tab card, same structure as a metric (requested 2026-10-21)
- [x] 0.2 Fields: times with ongoing and a woke-up-with-it flag, intensity 0-10, head area list, pain characters, own type label (including recognized headache), symptoms (including worse with physical activity, brain fog, extreme fatigue), triggers (including menstruation), medication free text with second dose and optional intensity after 2 hours (0-10), relief methods, daily activities tick, comment
- [x] 0.3 Medication stays separate from the pillbox
- [x] 0.4 Graph section "Migraine" view with calendar, heat map, timeline and duration charts; one hue, plain counts
- [x] 0.5 Sources noted (Thuisarts diary, national guideline, clinic diaries); NHG-Standaard text still to be read before the docs page is final
- [x] 0.5a Extra diary fields added (requested 2026-10-24): woke up with it, pain character, relief methods, intensity after 2 hours, brain fog and fatigue symptoms, menstruation trigger, recognized-headache type; nine steps kept by combining screens; supplied Thuisarts PDF read in full (2026-10-24)
- [x] 0.5b Optional "This helped me" ticks for medication and relief added (agreed 2026-10-24): own note, stored and shown as set, never counted, rated or prompted
- [x] 0.5c Personal tags for triggers and relief methods added (requested 2026-10-24): own table, referenced by id, rename and remove-from-list, max 30 per kind
- [x] 0.5d The 2-hour answer stays a 0–10 number (decided 2026-10-24), on the same scale as the first intensity; the Thuisarts diary uses three choices, which the app does not copy; the app computes nothing from it
- [x] 0.6 Spec review by the user done (2026-10-24)
- [x] 0.7 Final English and Dutch wording agreed, Dutch with "je" (table in design.md section 5)

- [x] 0.8 Migraine is built first: `MIGRATION_6_7`, database version 7 (decided 2026-10-24); oxygen saturation and the others take the next free number

## 1. Data (`:data`) and domain (`:domain`)
- [x] 1.1 `MigraineEntry` (including `wokeUpWithIt`, `painCharacters`, `reliefMethods` and `intensityAfter2Hours`, the last valid only with `tookSomething`) and enums (`PainCharacter`, `ReliefMethod` and the added symptom, trigger and type values) with validation and derived values; JVM tests; `MigraineTag` with its validation (1 to 30 characters, no `|`, unique per kind ignoring case, limit of 30); the two `helpedMe` ticks with their validity rules
- [x] 1.2 `MigraineStatistics` pure functions; JVM tests for empty range, single entry, ongoing entry, midnight, time-of-day and bucket boundaries, week start; a woke-up-with-it entry is left out of the time-of-day grid and counted aside
- [x] 1.3 Port methods and use cases (record, update, delete, end; add, rename, archive and list personal tags)
- [x] 1.4 `MigraineEntity` with the columns `wokeUpWithIt`, `intensityAfter2Hours`, `painCharacters` and `reliefMethods`, DAO, mapper, `MIGRATION_6_7` (creates the table with those columns), the table `migraine_tags` and the entry columns `customTriggers` and `customReliefMethods`, version 7, repository methods; deleting a profile removes its tags (the app has no profile deletion yet, so nothing to cascade); migration test from version 6; columns `medicationHelpedMe` and `reliefHelpedMe`
- [x] 1.5 CSV export and import with codes, the `woke_up_with_it`, `pain_character`, `intensity_after_2h` and `relief_methods` columns and row rejection; tests including round trip, ongoing and intensity after 2 hours without something taken; the `custom_triggers` and `custom_relief_methods` columns, tag creation on import, rejection of a bad label; `medication_helped_me` and `relief_helped_me` columns
- [x] 1.6 Backup snapshot and restore of `migraines` and `migraineTags` including the new fields; older file and newer file tests

## 2. App (`:app`)
- [x] 2.1 Migraine card on the Log tab
- [x] 2.2 Nine-step logging screen with draft, skip, save, discard confirmation: "Woke up with it" switch on step 1, pain character with head area, optional intensity after 2 hours on the medication step, relief methods with the carry-on choice; "Add my own" chip with dialog on the triggers and what-I-did steps, rename and remove by long-press and TalkBack action; optional "This helped me" ticks on the medication and what-I-did steps
- [x] 2.3 History rows, filter, edit, delete, "it ended"; "Started during sleep" and the 2-hour score on the row; the ticks shown as the person's note, no counts
- [x] 2.4 Migraine view: tiles, timeline, duration, intensity, weekday, heat map, symptom and trigger counts, chart descriptions; pain character and relief counts; started-during-sleep note on the heat map; personal tags in the trigger and relief counts
- [x] 2.5 Calendar and navigation to a day in History
- [x] 2.6 Strings in English and Dutch; scan test for "attack" and "aanval"; the tag dialog and messages (`MigraineStringsTest`)
- [x] 2.7 Export and import entries with the unencrypted warning (in the History export and import dialogs, where the other CSV files are)
- [x] 2.8 Emulator pass: English and Dutch, light and dark, 320 dp, screenshots

## 3. Documentation
- [x] 3.1 `docs/backup-format/` (README, JSON schema, `hjbackup.py`, recovery page; the test vectors stay valid because the new properties are optional and `schemaVersion` stays 1)
- [x] 3.2 `docs/dev/` (`database.md` ER diagram, current version and version history for both new tables, architecture, design, c4, testing, a Migraine feature page with the sources) and `docs/dev/how-to/add-a-metric.md` if the structure differs
- [x] 3.3 README, CHANGELOG, screenshots page (the README CSV table has its Migraine row)
- [ ] 3.4 Archive the change into `openspec/specs/`; release
