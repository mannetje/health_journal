# Add Oxygen saturation

## Why
People with a pulse oximeter at home want to note their readings next to weight, blood pressure and glucose, and look back over weeks and months. The app has no place for it. It is a small metric that follows the existing structure, so it ships first, as its own release, before the larger Migraine log ([change `add-migraine-log`](../add-migraine-log/proposal.md)).

## What Changes
- **Oxygen saturation card on the Log tab** with a value in whole percent (50 to 100), an optional pulse (30 to 250 bpm, the same rule as blood pressure), an optional context the person picks (at rest, after activity, asleep or just woke, other), an optional measurement site (finger, toe, earlobe), an optional "oxygen given" tick, the date and time (default now) and the existing 200-character comment.
- **History:** readings in the merged timeline with edit and delete, and an Oxygen filter chip.
- **Graph view under the Oxygen filter:** a dot per reading with the usual range chips, plus plain figures for the range: lowest, highest, average (one decimal) and the number of readings per context. No bands, no limit lines, no colour thresholds and no words such as low or normal.
- **Export and backup:** CSV export and import (`timestamp,spo2_percent,pulse_bpm,context,site,oxygen_given,comment`) and a `oxygenSaturations` list per profile in the `.hjbackup` document.
- **One factual tip on the form** in the app language: "Many people sit still for about 10 minutes first." It is not shown as advice or a warning and can be dismissed for good.
- **Notice:** the Profile notice says the app records what you enter and does not assess it.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `health-metrics/oxygen-saturation` (entry, validation, context, history retrieval, no assessment)
- **Modified Capability:** `entry-management` (oxygen readings in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on oxygen readings)
- **Modified Capability:** `health-trends` (oxygen graph view and figures)
- **Modified Capability:** `data-export` (oxygen CSV export and import)
- **Modified Capability:** `data-backup` (oxygen list in the backup document)
- **Modified Capability:** `compliance` (oxygen saturation stays a personal log with no assessment)
- **Modified Capability:** `privacy` (same protection as other entries)
- **Modified Capability:** `localization` (text in English and Dutch)

## Impact
- **Database:** a Room migration to the next free number at implementation time (current database version + 1; Migraine takes 6 → 7 when it ships first), creating one table `oxygen_saturations` (id, profile id, timestamp, percent, nullable pulse, nullable context, nullable site, oxygen-given flag, nullable comment). Existing tables are untouched, so old data stays readable and an upgrade only adds an empty table. A migration test opens the previous version with data.
- **Stored data shape:** the backup document gains an `oxygenSaturations` list per profile; `schemaVersion` stays 1 (readers ignore unknown properties, older files restore with none). `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Affected code:** `OxygenSaturation` and a context enum, validation, statistics functions and use cases in `:domain` (JVM tests including empty range and a single reading); entity, DAO, mapper, migration, repository methods and CSV adapters in `:data`; Log card, form, History row and edit dialog, and the graph view in `:app`.
- **Dependencies and permissions:** none.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md`, screenshots (English and Dutch, light and dark), `docs/backup-format/`.

## Sources
- The Dutch health information standard [zib O2Saturatie](https://zibs.nl/images/2/2b/Nl.zorg.O2Saturatie-v4.0%282024NL%29.pdf) (Nictiz) names the concept as a percentage, with a measurement site and a note whether oxygen was given. The first version follows the percentage, the measurement site, the oxygen-given note and the comment; site and tick are optional notes that change nothing. The mapping is kept in [docs/dev/zib-mapping.md](../../../docs/dev/zib-mapping.md). The standard page could not be read in full during drafting, so the mapping must be checked against it before the final text.
- Hospital home-measurement sheets (for example [Amphia, COPD](https://www.amphia.nl/uploads/media/65818aded4924/december2023-instructie-thuismetingen-copd-patientversie.pdf)): sit still for about 10 minutes, warm hands, no nail polish, wait about 30 seconds, note saturation and pulse together.
- Consumer and clinic pages quote cut-offs that differ per source and per person (for example with COPD). The app therefore shows no cut-off at all. The NHG-Standaard texts need a login and were not read; no NHG band for home saturation was found.

## Non-goals
- No reference range, band, limit line, alert, reminder or advice; no "low" or "normal" wording.
- The site and the oxygen-given tick are notes only: nothing is derived from them and no figure changes (decided 2026-10-21).
- No import from devices, Bluetooth or Health Connect.
- No link to the pillbox, medication or any other metric.

## Open questions
- None open. The site and tick were added as optional on 2026-10-21; wording of the Dutch labels ("Vinger", "Teen", "Oorlel", "Zuurstof toegediend") is for the final wording review.
