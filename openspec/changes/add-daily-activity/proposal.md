# Add Daily activity

## Why
Some people want to note how much they moved on a day (steps from a pedometer or watch, or minutes of activity) next to weight, blood pressure and glucose, and look back over weeks and months. The app has no place for it. It stays a plain log: the person types a number, the app shows it back. It reads no sensor and sets no goal.

## What Changes
- **Daily activity card on the Log tab** with a date (default today), optional steps (whole number), optional active minutes (whole minutes; at least one of the two is required), an optional activity type chosen from a short list (walking, cycling, swimming, sport, housework or garden, other) and the existing 200-character comment.
- **One entry per day.** Choosing a date that already has an entry loads it into the form and Save updates it (see design decision 4); there are never two entries for one date.
- **History:** entries in the merged timeline with edit and delete, and an Activity filter chip.
- **Graph view under the Activity filter:** one bar per day with the usual range chips and a switch between Steps and Active minutes, plus plain figures for the range: average per day logged, highest day, days logged and total. No goal line, no progress ring, no colour thresholds, no words such as enough, active enough or too little.
- **Export and backup:** CSV export and import (`date,steps,active_minutes,activity,comment`) and an `activityDays` list per profile in the `.hjbackup` document.
- **Notice:** the Profile notice says the app records what you enter and does not assess it (same sentence as for oxygen saturation, extended to activity).
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `health-metrics/daily-activity` (entry, validation, one entry per day, activity type, history retrieval, no goal or assessment)
- **Modified Capability:** `entry-management` (activity entries in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on activity entries)
- **Modified Capability:** `health-trends` (activity bar graph and figures)
- **Modified Capability:** `data-export` (activity CSV export and import)
- **Modified Capability:** `data-backup` (activity list in the backup document)
- **Modified Capability:** `compliance` (daily activity stays a personal log with no target and no advice)
- **Modified Capability:** `privacy` (same protection as other entries; no sensor access, no new permission)
- **Modified Capability:** `localization` (text in English and Dutch)

## Impact
- **Database:** one new Room migration, creating one table `daily_activities` (id, profile id, date as epoch day, nullable steps, nullable active minutes, nullable activity code, nullable comment) with a unique index on profile id and date. The migration is the next free number at implementation time (current database version + 1). Existing tables are untouched. A migration test opens a database of the previous version with data.
- **Stored data shape:** the backup document gains an `activityDays` list per profile; `schemaVersion` stays 1 (readers ignore unknown properties, older files restore with none). `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Affected code:** `ActivityDay` and an activity type enum, validation, statistics functions and use cases in `:domain` (JVM tests including empty range and a single day); entity, DAO, mapper, migration, repository methods and CSV adapters in `:data`; Log card, form, History row and edit dialog, and the bar graph in `:app`.
- **Dependencies and permissions:** none. The manifest has no INTERNET permission and `PrivacyChecksTest` enforces the permission and logging limits; this change adds no permission (no `ACTIVITY_RECOGNITION`, no Health Connect, no sensor access) and the test stays unchanged.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md`, screenshots (English and Dutch, light and dark), `docs/backup-format/`.

## Sources
Read during drafting (2026-10-21):
- Gezondheidsraad, [Beweegrichtlijnen 2017](https://www.gezondheidsraad.nl/sites/default/files/grpublication/samenvatting_beweegrichtlijnen_2017_1.pdf) (summary PDF, via a fetch summary): for adults, about two and a half hours of moderate-intensity activity per week, muscle and bone strengthening, and less sitting. A search summary of the full advice agrees. **Deliberately not turned into a target:** the app copies no minutes-per-week norm, no step goal, no "10,000 steps" and no weekly progress, because a norm is advice and the app does not advise ([ADR 0021](../../../docs/adr/0021-medication-compliance-and-privacy.md)); norms also differ by age, condition and source.
- Nictiz zibs (via search results of zibs.nl pages): no zib for physical activity or exercise was found. The closest is [Mobiliteit](https://zibs.nl/wiki/Mobiliteit-v3.3.1(2024NL)) (ability to walk and change position, a functional description, not a count of steps or minutes) and ParticipatieInMaatschappij (free-time activities in prose). The app therefore treats this metric as having no zib (own naming), recorded in `docs/dev/zib-mapping.md`. The zibs.nl wiki index returned HTTP 503, so the list was not read in full; "none found" is not a proof that none exists.

Could not be read:
- Thuisarts.nl pages on bewegen (the URLs tried returned 404; no verified Thuisarts text is used). Kenniscentrum Sport & Bewegen, Loketgezondleven and the NHG-Standaarden were not read (NHG needs a login).

## Non-goals
- No sensors, no step counting, no Health Connect or Google Fit, no new permission, no network.
- No target, goal line, progress ring, streak, badge, "enough exercise" verdict, reminder or advice. A self-set personal goal would be its own later change with a compliance review.
- No intensity classification (light, moderate, vigorous), no calories, no distance.
- No link to the pillbox, medication, weight or any other metric.

## Open questions
- Decided: no personal goal for now (a self-set goal would be its own later change with a compliance review).
- Decided: the activity type stays a fixed list of six types; free text is a possible later change only.
- Final Dutch wording ("Dagelijkse beweging", "Stappen", "Actieve minuten", the activity types) is for the wording review.
