# Add Sleep log

## Why
People who want to look back at how long they slept have no place for it in the app. A short, everyday entry (went to bed, woke up) next to weight, blood pressure and the other metrics, with a plain history and a few figures, is enough. The duration follows from the two times, so the entry takes seconds. Numbers are shown as they are: no target, no verdict.

## What Changes
- **Sleep card on the Log tab** with "went to bed" and "woke up" (date and time pickers, default last night: bed time yesterday 23:00 and wake time today 07:00 on the device clock, both editable), an optional quality from 1 to 5, an optional "number of times woken" (0 to 30) and the existing 200-character comment. The computed duration is shown under the pickers as plain text (for example "7 h 30 min") before saving.
- **History:** sleep entries in the merged timeline (sorted by wake time) with edit and delete, and a Sleep filter chip.
- **Graph view under the Sleep filter:** a bar per night with the hours slept, with the usual range chips, plus plain figures for the range: average duration, shortest, longest, nights logged, average bed time and average wake time. No band, no target line, no colour threshold, and no words such as enough, too little, good or bad.
- **Export and backup:** CSV export and import (`bed_time,wake_time,quality,times_woken,comment`) and a `sleepLogs` list per profile in the `.hjbackup` document.
- **Notice:** the Profile notice says the app records what you enter and does not assess it (same sentence as for the other metrics).
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `health-metrics/sleep-log` (entry, validation, computed duration, history retrieval, no assessment)
- **Modified Capability:** `entry-management` (sleep entries in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on sleep entries)
- **Modified Capability:** `health-trends` (sleep graph view and figures)
- **Modified Capability:** `data-export` (sleep CSV export and import)
- **Modified Capability:** `data-backup` (sleep list in the backup document)
- **Modified Capability:** `compliance` (sleep log stays a personal log with no assessment)
- **Modified Capability:** `privacy` (same protection as other entries)
- **Modified Capability:** `localization` (text in English and Dutch)

## Impact
- **Database:** a Room migration to the next free version (the migration is the next free number at implementation time: current database version + 1, written `MIGRATION_N_(N+1)` in the implementation), creating one table `sleep_logs` (id, profile id, bed time, wake time, nullable quality, nullable times woken, nullable comment). Existing tables are untouched, so an upgrade only adds an empty table. A migration test opens a database of the previous version with data and checks the new version, the old rows and an empty new table. The change does not depend on the order in which other features are built.
- **Stored data shape:** the backup document gains a `sleepLogs` list per profile; `schemaVersion` stays 1. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Affected code:** `SleepLog`, validation, `SleepStatistics` and use cases in `:domain` (JVM tests including empty range, one night, nights over midnight and daylight-saving changes); entity, DAO, mapper, migration, repository methods and CSV adapters in `:data`; Log card, form, History row and edit dialog, and the graph view in `:app`.
- **Dependencies and permissions:** none.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md`, screenshots (English and Dutch, light and dark), `docs/backup-format/`, [ADR 0027](../../../docs/adr/0027-sleep-log-without-assessment.md).

## Sources
Read during drafting (2026-10-21):
- [Thuisarts, "Ik ga een slaapdagboek invullen om te zien hoeveel ik slaap"](https://www.thuisarts.nl/slecht-slapen/ik-ga-slaapdagboek-invullen-om-te-zien-hoeveel-ik-slaap) (fetched; the page text was returned only as a short summary). A paper diary the person fills in each morning: the time the light went out, when they think they fell asleep, when they woke, how long they stayed in bed, plus daytime sleep and caffeine and alcohol marks. The app follows only the idea of noting bed time and wake time and working out the duration. It does not copy the sleep-onset estimate, the daytime marks, the substances or the advice around the diary.
- [Thuisarts sleep diary PDF and related Thuisarts pages](https://www.thuisarts.nl/sites/default/files/2024-06/Slaapdagboek_Thuisarts.pdf) appeared in search results only; the PDF and the pages "Ik wil beter slapen: slaapadviezen" and "Ik ga korter in bed liggen" were not opened. Their advice (hours in bed, bedtime restriction) is not used.
- **zib:** the Dutch health information standard has [Nl.zorg.Slaapfunctie v3.0 (2016)](https://zibs.nl/images/3/32/Nl.zorg.Slaapfunctie-v3.0%28NL%29.pdf) (sleep function, ICF b134, with a concept about staying asleep). It is a clinical problem-and-function model for sleep disorders, not a sleep log, and its text could not be read: zibs.nl returned HTTP 503 for the wiki page and both PDFs. Only the search-result summary was seen. No zib for a plain night of sleep with bed and wake times was found. The mapping row therefore says "no matching zib, Slaapfunctie checked by title only" and must be verified before final text.
- **Not readable:** NHG-Standaard Slapeloosheid (HTTP 401, login), the Gezondheidsraad site (home page only, no sleep advice found), Slaapwijzer, Nederlandse Slaapfederatie and Hersenstichting (not searched or opened).

- Unverified observation, from general knowledge and not checked during drafting: Apple Health, Google Health Connect and Samsung Health show a night of sleep on the date of waking. The app follows this convention by decision, not because of a source.

Deliberately NOT copied: diagnoses or terms for sleep disorders, hour targets or "enough sleep" ranges from any source, sleep-hygiene or bedtime advice, a time-to-fall-asleep estimate, scores or efficiency percentages, and any advice on when to see a doctor.

## Non-goals
- No target hours, band, goal line, score, grade, streak, reminder or advice; no "enough", "too little", "good" or "bad" wording.
- No sleep stages, no sleep latency, no daytime naps as a separate type (a nap is just an entry with its own times), no caffeine or alcohol marks.
- No import from wearables, Health Connect or phone sensors, and no automatic detection of sleep.
- No link to the pillbox, medication or any other metric.

## Decisions (closed)
- A night belongs to the local date of waking.
- Several sessions on the same date are allowed; a nap is just another entry, with no separate type.
- The 24-hour maximum between bed and wake time stays, as a typo guard and not a clinical limit.

## Open questions
- Spec review by the user before implementation.
- Final English and Dutch wording ("Slaap", "Naar bed", "Wakker geworden", "Kwaliteit", "Aantal keer wakker").
- Zib Slaapfunctie to be read in full (zibs.nl was down) to confirm "no matching zib".
