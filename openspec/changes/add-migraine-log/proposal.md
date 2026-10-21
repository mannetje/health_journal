# Add Migraine log

## Why
People who get migraines want to note when one starts and ends, how strong it was, where it sat, what came with it and what they took, and later see plain counts and patterns over weeks and months. Today the app has no place for this. The Log tab already holds one card per metric, so a Migraine card fits the same structure. Inspiration came from public migraine diaries (timing, intensity, duration, triggers, notes, medication, export), without copying any of them.

## What Changes
- **Migraine card on the Log tab** with a "Log a migraine" button (Dutch: "Migraine registreren") that opens a short step-by-step screen:
  1. Start time (default now) and an optional end time. A migraine may still be going: it can be saved without an end time and ended later.
  2. Intensity 0–10.
  3. Head area, a plain list of areas.
  4. Type, a label the person picks: with aura, without aura, other headache, not sure.
  5. Symptoms checklist (nausea, aura, light, sound and smell sensitivity, dizziness, vertigo, movement symptoms such as numbness, other).
  6. Triggers the person noted themselves (sleep, stress, food, weather, other).
  7. A "took something" tick, a free-text "what I took" (no suggestions) and a "took a second dose" tick.
  8. A skippable "daily activities" choice: could carry on, or had to stop.
  9. The existing 200-character comment.
  Every step after the first can be skipped and Save is always available.
- **History timeline:** migraines appear next to the other entries, with edit, delete and an "it ended" action for ongoing ones.
- **History graph section, "Migraine" view** with a Graph/Calendar toggle and range chips (week, month, 3 months, 6 months, year): plain-count tiles (migraines, days with migraine, days with something taken, migraine-free days); a timeline chart with one bar per migraine on its date, its height the intensity 0–10 (an other-headache bar in a second tone of the same hue); a duration chart in the hour ranges under 4, 4–12 and over 12 (ongoing ones are left out and counted separately); an intensity histogram in the number ranges 0–3, 4–6, 7–10; weekday bars; a heat map of weekday by time of day (night, morning, afternoon, evening) in one hue; a month calendar with marked days that opens that day's entries; symptom and trigger counts.
- **Export and backup:** CSV export and import of migraines, and inclusion in the `.hjbackup` file.
- **Neutral wording:** the word "attack" is not used. One hue, no red/green or severity colours, factual captions ("4 migraines started on Monday"), no advice, warnings, predictions or suggested causes.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `migraine` (entry, logging steps, ongoing entries, statistics and view, wording rules)
- **Modified Capability:** `entry-management` (migraine in the History list, edit, delete, end an ongoing one)
- **Modified Capability:** `entry-comments` (migraine entries carry the optional comment)
- **Modified Capability:** `health-trends` (the Migraine view in the graph section)
- **Modified Capability:** `data-export` (migraine CSV export and import)
- **Modified Capability:** `data-backup` (migraines in the backup document)
- **Modified Capability:** `compliance` (migraine log stays a personal log: no diagnosis, advice, prediction or alerts)
- **Modified Capability:** `privacy` (migraine data is encrypted at rest with the rest, never logged or sent)
- **Modified Capability:** `localization` (migraine text in English and Dutch)

## Impact
- **Database:** Room version 6 → 7 with `MIGRATION_6_7` creating one new table `migraine_entries` (profile id, start, nullable end, intensity, areas, type, symptoms, triggers, took-something, medication text, second dose, comment). Existing tables are untouched, so old data stays readable and an upgrade only adds an empty table. The migration is covered by a migration test.
- **Stored data shape:** the backup document gains a `migraines` list per profile and `schemaVersion` stays 1 (readers ignore unknown properties; an older reader restoring a newer file loses only the migraines, and the docs say so). `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated. The CSV gains a migraine section in the same conventions.
- **Affected code:** value object, entry, statistics functions and use cases in `:domain` (pure Kotlin, JVM tests including empty range, single entry and ongoing entry); entity, DAO, mapper, repository methods, migration and CSV adapters in `:data`; Log card, step screen, History row and edit, graph view and calendar in `:app`.
- **Dependencies and permissions:** none.
- **Docs:** README, CHANGELOG, `docs/dev/` (architecture, design, c4, testing, add-a-metric notes), screenshots (light and dark, English and Dutch), `docs/backup-format/`.

## Non-goals
- No drawn head map in this version (a plain list of areas first).
- No link to the pillbox medication; "what I took" is free text and no medicine names are suggested or built in.
- No reminders, no forecasts, no trend verdicts, no doctor-appointment guide; the CSV export is the summary to share, with its existing unencrypted-file warning.
- No period note, no weather lookup (the app has no network).
- No diagnosis, advice, warnings or suggested causes.

## Sources
The field list follows what Dutch care providers and patient bodies already ask people to note. Nothing is copied; the app only offers a personal log and never interprets it.
- [Thuisarts.nl headache diary](https://www.thuisarts.nl/sites/default/files/hoofdpijndagboek_2014_def.pdf) (the GP information site of NHG and the Dutch College of General Practitioners): date, aura, accompanying symptoms, pain score 0–10, pain medication (name, strength, number), how long it lasted, and what the person did. It is meant to be talked through with the GP.
- [National headache guideline, migraine module, NVN](https://richtlijnendatabase.nl/richtlijn/hoofdpijn/migraine.html) (published 04-12-2023, with V&VN and the patient association Hoofdpijnnet), including [the section on digital diaries](https://richtlijnendatabase.nl/richtlijn/hoofdpijn/migraine/organisatie_van_zorg_bij_migraine/migraine_organisatie_van_zorg_digitale_dagboeken.html): a digital diary can help a person and their care provider look back together.
- Hospital headache clinic diaries (for example [Zuyderland](https://zuyderland.nl/wp-content/uploads/2024/07/Hoofdpijndagboek.pdf)): pain score, duration, medication used, notes.
- The NHG-Standaard Hoofdpijn itself requires a login and could not be read; its wording is not quoted. Check it before the final text of the docs page is written.

What the app deliberately does not take over from those diaries: the diagnosis codes on the Thuisarts form (tension-type, migraine, medication-overuse), any count of medication days with a limit, and any advice on when to see a doctor. Those are for the person's own GP or neurologist. The "Type" in the app is only the person's own label.

