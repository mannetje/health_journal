# Add Energy level

## Why
Some people want to note, a few times a day or now and then, how energetic they feel, and look back over weeks and months. The app has no place for it. It is a very small metric: one number from 0 to 10, a time and an optional comment. It follows the existing structure and uses the shared 0-10 picker that Migraine intensity also uses.

## What Changes
- **Energy card on the Log tab** with the shared 0-10 picker (0 "No energy", 10 "Full of energy"; Dutch "Geen energie" and "Vol energie"), the date and time (default now) and the existing 200-character comment. The picker shows numbers only; the words appear on the two ends of the scale and nowhere else.
- **Multiple entries per day** are normal. No limit and no merging.
- **History:** entries in the merged timeline with edit and delete, and an Energy filter chip.
- **Graph view under the Energy filter:** one dot per entry with the usual range chips and plain figures: entries, average (one decimal), lowest, highest. No bands, no limit lines, no colour thresholds, no words such as low, normal, tired or good.
- **Export and backup:** CSV export and import (`timestamp,energy_0_10,comment`) and an `energyEntries` list per profile in the `.hjbackup` document (`schemaVersion` stays 1).
- **Notice:** the Profile notice says the app records what you enter and does not assess it.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `health-metrics/energy-level` (entry, validation, scale wording, history retrieval, no assessment)
- **Modified Capability:** `entry-management` (energy entries in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on energy entries)
- **Modified Capability:** `health-trends` (energy graph view and figures)
- **Modified Capability:** `data-export` (energy CSV export and import)
- **Modified Capability:** `data-backup` (energy list in the backup document)
- **Modified Capability:** `compliance` (energy level stays a personal log with no assessment and no screening)
- **Modified Capability:** `privacy` (same protection as other entries)
- **Modified Capability:** `localization` (text in English and Dutch)

## Impact
- **Database:** Room migration to the next free version, creating one table `energy_levels` (id, profile id, timestamp, level, nullable comment). The migration is the next free number at implementation time (current database version + 1); this change does not depend on the order in which features are built. Existing tables are untouched. A migration test opens a database of the previous version with data.
- **Stored data shape:** the backup document gains an `energyEntries` list per profile; `schemaVersion` stays 1 (readers ignore unknown properties, older files restore with none). `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Affected code:** `EnergyEntry`, validation, `EnergyStatistics` (count, average, lowest, highest) and use cases in `:domain` (JVM tests including empty range and a single entry); entity, DAO, mapper, migration, repository methods and CSV adapters in `:data`; Log card, History row and edit dialog and the graph view in `:app`. The shared 0-10 picker is created or reused (see design).
- **Dependencies and permissions:** none.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md`, screenshots (English and Dutch, light and dark), `docs/backup-format/`, [ADR 0032](../../../docs/adr/0032-energy-level-without-assessment.md).

## Sources
Read during drafting (2026-10-21):
- [Thuisarts.nl, "Moeheid: waardoor kun je moe zijn, en wie en wat helpt?"](https://www.thuisarts.nl/moeheid) (NHG and the Dutch College of General Practitioners): only the introduction was returned by the fetch tool. It says tiredness has many causes, names sleep problems, stress, burn-out, depression, ME/CVS and fibromyalgia as related topics, and links two subpages ("Wat kan het zijn?", "Wat kan ik eraan doen?") that were not read. Search summaries of copies of the page also mention advice on sleep, exercise and when to contact the GP; those are second-hand and not relied on.
- [zibs.nl](https://www.zibs.nl): the server answered 503 and could not be read. A web search found no zib for fatigue, energy or "Vermoeidheid". The nearest concept is the zib Symptoom (a complaint or finding through which a condition presents itself) within the Probleem family, seen only in search summaries of Nictiz material and a FHIR profile (`cio-Symptom`). Status: no dedicated zib found; Symptoom to be checked when zibs.nl is reachable.
- Not read: the NHG-Standaard Moeheid (HTTP 401, login needed), Hersenstichting and Kenniscentrum Chronische Vermoeidheid (not opened; no page was needed for a plain 0-10 log).

What the app deliberately does not take over: causes lists, diagnostic criteria or durations, red-flag or "when to see the GP" lists, lifestyle or sleep advice, and any validated questionnaire (Checklist Individual Strength, Fatigue Severity Scale, Borg and similar). The 0-10 number is the person's own rating, not a score of any instrument and not comparable with one.

## Non-goals
- No fatigue syndrome, CVS/ME, burn-out or depression wording, screening, or criteria; no suggested causes.
- No threshold, band, limit line, alert, reminder or advice; no "low" or "normal" wording.
- No validated questionnaire (CIS, FSS, Borg or other) and no sum or sub-score.
- No weekday by time-of-day heat map in this version. It is a possible later change (the Migraine change has the same idea) and needs no data-model change, because time of day can be derived from the timestamp.
- No link to Mood or any other metric. Mood and Energy are separate metrics with different scale meaning; a later optional "daily check-in" card could ask for both and save two separate entries, as a separate change.
- No import from devices, Health Connect or wearables.

## Open questions
- Final Dutch wording of the endpoints ("Geen energie" / "Vol energie").
