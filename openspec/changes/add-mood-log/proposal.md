# Add Mood log

## Why
Some people want a very short daily note of how the day feels, next to weight, blood pressure and sleep, and a way to look back over weeks and months. The app has a Mindfulness area for calming exercises (it records nothing) but no place to write down a mood. A mood entry is five seconds: pick a step on a five-step scale, optionally pick a few feeling words, save. It follows the existing metric structure. It is also the metric closest to the line between a personal log and a screening tool, so this change is mostly about what the app will not do ([ADR 0028](../../../docs/adr/0028-mood-log-without-assessment.md)).

## What Changes
- **Mood card on the Log tab** with five labelled steps (1 to 5) in one row, optional feeling words (a small fixed set, at most three), the date and time (default now) and the existing 200-character comment. Several entries per day are allowed. Nothing is pre-selected and the card saves with one step chosen.
- **Scale wording (neutral):** the steps describe how the moment feels, not a verdict: 1 "Very unpleasant", 2 "Unpleasant", 3 "Neutral", 4 "Pleasant", 5 "Very pleasant" (Dutch: "Zeer onaangenaam", "Onaangenaam", "Neutraal", "Aangenaam", "Zeer aangenaam"). The data model stores only the whole number 1 to 5. The words good, bad, happy, sad-as-a-score, better and worse are not used for steps.
- **Feeling words (optional, fixed set of 10):** calm, content, cheerful, energetic, tired, tense, restless, irritated, sad, worried. They are the person's own notes, stored as stable codes; they change no figure and nothing is derived from them.
- **History:** mood entries in the merged timeline with edit and delete, and a Mood filter chip.
- **Graph view under the Mood filter:** one dot per entry on a fixed 1 to 5 axis with the usual range chips, optionally joined by a thin line through the daily mean; plain figures for the range: entries, days with entries, average (one decimal) and the number of entries per step. No bands, no limits, no colour that reads as good or bad, no verdicts.
- **Export and backup:** CSV export and import (`timestamp,mood_level,feelings,comment`) and a `moodEntries` list per profile in the `.hjbackup` document.
- **Notice:** the Profile notice says the mood log records what you enter and does not assess it; it is not a questionnaire, not a screening and not a diagnosis.
- **Relation to Mindfulness:** both live in the app but are separate. Mindfulness stores nothing; the Mood log stores what is typed. There is no link, shortcut or suggestion between them in v1.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `health-metrics/mood` (entry, scale, feeling words, validation, history retrieval, no assessment)
- **Modified Capability:** `entry-management` (mood entries in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on mood entries)
- **Modified Capability:** `health-trends` (mood graph and figures)
- **Modified Capability:** `data-export` (mood CSV export and import)
- **Modified Capability:** `data-backup` (mood list in the backup document)
- **Modified Capability:** `compliance` (the mood log records and does not assess; no questionnaires or scores)
- **Modified Capability:** `privacy` (same protection as other entries; mood is sensitive)
- **Modified Capability:** `localization` (text in English and Dutch)

## Impact
- **Database:** a Room migration (next free version, see below), creating one table `mood_entries` (id, profile id, timestamp, level 1 to 5, nullable feelings text of codes, nullable comment). Existing tables are untouched. A migration test opens a version 9 database with data. The migration is the next free number at implementation time (current database version + 1); the order against other metric changes is not fixed.
- **Stored data shape:** the backup document gains a `moodEntries` list per profile; `schemaVersion` stays 1. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Affected code:** `MoodEntry`, `MoodFeeling`, validation, `MoodStatistics` and use cases in `:domain` (JVM tests including empty range, one entry and several per day); entity, DAO, mapper, migration, repository and CSV adapters in `:data`; Log card, History row and edit dialog, graph view in `:app`.
- **Dependencies and permissions:** none. No reminder or notification in v1.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md`, screenshots (English and Dutch, light and dark), `docs/backup-format/`.

## Sources
Read during drafting (2026-10-21):
- [Thuisarts, "Somberheid"](https://www.thuisarts.nl/somberheid) and [Thuisarts, "Ik voel me somber"](https://www.thuisarts.nl/somberheid/ik-voel-me-somber): general guidance on low mood (talk, routine, movement) and when to contact the GP, with 113 and De Luisterlijn named for thoughts of suicide. Neither page mentions a mood diary or tracking. Used only to know what the app must not become: no advice, no "when to see your GP" logic tied to entries.
- [113 Zelfmoordpreventie](https://www.113.nl): reachable on 113 or 0800-0113, round the clock, chat anonymous (confirmed through search summaries; the site itself did not answer). Relevant only to the open question on a static line below.
- Trimbos-instituut project registrations found by search (Boost My Mood, Moodbuster Light; UMCG ZELF-i): mood self-monitoring with an app has been studied for feasibility in Dutch research, and some of those tools ask several questions several times a day. No effect figures were found. The app copies none of those programmes.
- Huisarts en Wetenschap articles (1997, 2003) found by search: a diary is mentioned as a possible help in a CBT-based approach, and a diagnosis of depression is made by a doctor on history and examination. Search summaries only; the articles were not read in full.

Could not be read:
- zibs.nl (HTTP 503 on the zib list and on the zib *Functionele of Mentale Status* v4.0.1, which appeared in search results and may be the nearest zib). **Finding for now: no zib named for mood, stemming, gevoel or gemoedstoestand was found; status "none found, to verify".**
- NHG-Standaard Depressie (login-gated, not attempted beyond knowing it exists), Mind, Gezondheidsraad and the Trimbos guideline pages: not read.

Deliberately NOT copied:
- Screening or severity scales (PHQ-9, PHQ-2, GAD-7, HADS, Beck and similar), their questions, scoring, cut-offs and bands. The app implements no questionnaire.
- Any threshold such as "low mood for two weeks", any count of low days, any streak or "trend towards worse" statement.
- Crisis-advice logic: no behaviour that reacts to an entered step or word (no pop-up, no hotline prompt triggered by a value).
- The multi-times-a-day prompting of research apps: no reminder in v1.

## Non-goals
- No questionnaire, score, screening, diagnosis, risk estimate, prediction, alert, advice, coaching or encouragement; no words such as depressed, anxious, good, bad, better, worse in the data or in verdict-like text.
- No reminder, notification, streak, goal or badge.
- No calendar view in this version. A calendar view may come in a later change (reusing the Migraine calendar); no data-model change is needed.
- Mood and Energy level stay separate metrics: own tables, scales (1 to 5 pleasant versus 0 to 10 energy) and charts, no shared field and no correlation. A later optional "daily check-in" card on the Log tab could ask for both in one go and save two separate entries with the same timestamp; that would be its own later change.
- No link to Mindfulness, medication, sleep or any other metric; no correlation view in v1.
- No emoji faces as the scale (they carry judgement and vary by platform); no free-text sentiment analysis; no import from other apps or wearables.

## Open questions
- **Static pointer to help (recorded default: no fixed line; still open for the final wording review).** Should the Profile or privacy notice carry one fixed, always-visible sentence such as "If you need to talk to someone, contact your GP; 113 is open day and night"? It is not triggered by any value and is the same for everyone, but it is advice-shaped and the `compliance` capability forbids tips and warnings except the existing pointer to doctor, pharmacist and Thuisarts. Option A: nothing beyond the existing pointer. Option B: one static line in the notice, wording agreed with the user. The drafted specs assume A. A reviewer outside the project should confirm either way.
- Wording of the five step labels and the ten feeling words in English and Dutch (final review).
- Zib check once zibs.nl answers (task 0.5).
