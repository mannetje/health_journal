# Add Migraine log

## Why
People who get migraines want to note when one starts and ends, how strong it was, where it sat, what came with it and what they took, and later see plain counts and patterns over weeks and months. Today the app has no place for this. The Log tab already holds one card per metric, so a Migraine card fits the same structure. Inspiration came from public migraine diaries (timing, intensity, duration, triggers, notes, medication, export), without copying any of them.

## What Changes
- **Migraine card on the Log tab** with a "Log a migraine" button (Dutch: "Migraine registreren") that opens a short step-by-step screen:
  1. Start time (default now) with a quick "Woke up with it" switch and an optional end time. A migraine may still be going: it can be saved without an end time and ended later. With the switch on, the time is kept as an estimate and History says "Started during sleep".
  2. Intensity 0–10.
  3. Where and what kind, on one screen: head area (a plain list) and pain character (throbbing or pounding, pressing or tight, piercing; several allowed).
  4. Type, a label the person picks: with aura, without aura, recognized (familiar) headache, other headache, not sure.
  5. Symptoms checklist (nausea, aura, light, sound and smell sensitivity, dizziness, vertigo, movement symptoms such as numbness, worse with physical activity, brain fog or trouble thinking, extreme fatigue, other).
  6. Triggers the person noted themselves (sleep, stress, food, weather, menstruation, other), plus an "Add my own" chip for the person's own triggers.
  7. A "took something" tick, a free-text "what I took" (no suggestions), a "took a second dose" tick and, once something was taken, an optional "intensity after 2 hours" 0–10. It is a plain number the person can look back on with their care provider; the app shows both numbers as entered and computes no difference or verdict. If the person wants to say it helped, they can write that in their own comment or set the optional "This helped me" tick, which is kept as their own note and never counted or evaluated.
  8. What I did, on one screen: relief methods besides medication (sleep or rest, dark and quiet room, cold pack, heat, massage, coffee or tea, other, plus the person's own; several allowed) and the skippable "could carry on / had to stop" choice. An optional "This helped me" tick can be set here too.
  9. The existing 200-character comment.
  Every step after the first can be skipped and Save is always available.
- **Personal tags for triggers and relief methods:** an "Add my own" chip on those two lists lets the person type a short tag once (1 to 30 characters). It then appears as a chip on every later entry and is counted like the fixed items. A tag can be renamed (old entries follow) or removed from the list (old entries keep it). Tags stay on the device in the encrypted database and are in the backup and the CSV. The app never suggests, ranks or interprets a tag.
- **History timeline:** migraines appear next to the other entries, with edit, delete and an "it ended" action for ongoing ones.
- **History graph section, "Migraine" view** with a Graph/Calendar toggle and range chips (week, month, 3 months, 6 months, year): plain-count tiles (migraines, days with migraine, days with something taken, migraine-free days); a timeline chart with one bar per migraine on its date, its height the intensity 0–10 (an other-headache bar in a second tone of the same hue); a duration chart in the hour ranges under 4, 4–12 and over 12 (ongoing ones are left out and counted separately); an intensity histogram in the number ranges 0–3, 4–6, 7–10; weekday bars; a heat map of weekday by time of day (night, morning, afternoon, evening) in one hue (migraines that began during sleep are left out and counted in a note); a month calendar with marked days that opens that day's entries; counts per symptom, trigger, pain character and relief method.
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
- **Database:** Room version 6 → 7 with `MIGRATION_6_7` creating two new tables, `migraines` and `migraine_tags` (the person's own trigger and relief tags), (profile id, start, nullable end, intensity, areas, type, symptoms, triggers, took-something, medication text, second dose, nullable intensity after 2 hours, relief methods, comment, woke-up flag and pain character). Existing tables are untouched, so old data stays readable and an upgrade only adds empty tables. The migration is covered by a migration test.
- **Stored data shape:** the backup document gains `migraines` and `migraineTags` lists per profile and `schemaVersion` stays 1 (readers ignore unknown properties; an older reader restoring a newer file loses only the migraines and tags, and the docs say so). `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated. The CSV gains a migraine section in the same conventions.
- **Affected code:** value object, entry, statistics functions and use cases in `:domain` (pure Kotlin, JVM tests including empty range, single entry and ongoing entry); entity, DAO, mapper, repository methods, migration and CSV adapters in `:data`; Log card, step screen, History row and edit, graph view and calendar in `:app`.
- **Dependencies and permissions:** none.
- **Docs:** README, CHANGELOG, `docs/dev/` (architecture, design, c4, testing, add-a-metric notes), screenshots (light and dark, English and Dutch), `docs/backup-format/`.

## Non-goals
- No drawn head map in this version (a plain list of areas first).
- No link to the pillbox medication; "what I took" is free text and no medicine names are suggested or built in.
- No reminders, no forecasts, no trend verdicts, no doctor-appointment guide; the CSV export is the summary to share, with its existing unencrypted-file warning.
- No period note, no weather lookup (the app has no network).
- No diagnosis, advice, warnings or suggested causes.
- No personal tags for symptoms, pain characters, head areas or type; no tag suggestions, no built-in tag library and no sharing of tags between profiles.
- No evaluation of medication or relief methods: the 2-hour score and the relief list are notes shown back as entered, never turned by the app into an effect, a ranking, "what works best" or advice. The person's own words in a comment are theirs and stay untouched.

## Sources
The field list follows what Dutch care providers and patient bodies already ask people to note. Nothing is copied; the app only offers a personal log and never interprets it.
- [Thuisarts.nl headache diary](https://www.thuisarts.nl/sites/default/files/hoofdpijndagboek_2014_def.pdf) (the GP information site of NHG and the Dutch College of General Practitioners): date, aura, accompanying symptoms, pain score 0–10, pain medication (name, strength, number), how long it lasted, and what the person did. It is meant to be talked through with the GP.
- Thuisarts.nl ["I am going to fill in a headache diary"](https://www.thuisarts.nl/hoofdpijn/ik-ga-hoofdpijn-dagboek-invullen) and its PDF "Hoofdpijn-dagboek om te kijken of uw medicijnen tegen hoofdpijn helpen" (a diary to see whether headache medication helps; supplied by the user, added 2026-10-24). It is the source for the pain score two hours after taking something and for menstruation as a note. Pain character, relief methods, "woke up with it" and the brain fog and fatigue symptoms follow what public migraine diaries commonly let a person note. Read in full on 2026-10-24: one date-per-row table with known headache yes/no, pain score 0–10, medication (name, strength, number of pills), other things or side effects, pain after 2 hours (the same, less or none), how long it lasted, and whether the person could carry on. Note the diary asks the 2-hour answer as three choices, while this change deliberately stores a 0–10 number on the same scale as the first intensity (decided 2026-10-24, tasks 0.5d).
- [National headache guideline, migraine module, NVN](https://richtlijnendatabase.nl/richtlijn/hoofdpijn/migraine.html) (published 04-12-2023, with V&VN and the patient association Hoofdpijnnet), including [the section on digital diaries](https://richtlijnendatabase.nl/richtlijn/hoofdpijn/migraine/organisatie_van_zorg_bij_migraine/migraine_organisatie_van_zorg_digitale_dagboeken.html): a digital diary can help a person and their care provider look back together.
- Hospital headache clinic diaries (for example [Zuyderland](https://zuyderland.nl/wp-content/uploads/2024/07/Hoofdpijndagboek.pdf)): pain score, duration, medication used, notes.
- [NHG-Standaard Hoofdpijn](https://richtlijnen.nhg.org/standaarden/hoofdpijn) (M19, published September 2021, last changed April 2025), section "Hoofdpijndagboek", read on 2026-10-24: a diary can show frequency, duration and severity, possible triggers, a relation to menstruation, use and effect of self-care medication, and the effect on daily activities and work or school absence; two months of registration is wanted for a diagnosis; the diary is not a validated diagnostic method and is based on practice and consensus. The standard also has a second diary version that the care provider uses to evaluate prescribed medication. Wording is not quoted, and the app does neither: it gives no diagnosis and no medication-days limit, and leaves evaluation to the care provider.

What the app deliberately does not take over from those diaries: the diagnosis codes on the Thuisarts form (tension-type, migraine, medication-overuse), any count of medication days with a limit, and any advice on when to see a doctor. Those are for the person's own GP or neurologist. The "Type" in the app is only the person's own label.

