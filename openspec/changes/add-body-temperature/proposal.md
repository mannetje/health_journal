# Add Body temperature

## Why
People take their temperature with an ear, forehead or other thermometer and want to note the readings next to weight, blood pressure, glucose and oxygen saturation, and look back over days and weeks. The app has no place for it. It is a small metric that follows the existing structure and the same "records, does not assess" pattern as [change `add-oxygen-saturation`](../add-oxygen-saturation/proposal.md) ([ADR 0026](../../../docs/adr/0026-oxygen-saturation-without-assessment.md)).

## What Changes
- **Body temperature card on the Log tab** with one value in degrees Celsius with one decimal (30.0 to 45.0), an optional measurement site the person picks (ear, mouth, armpit, forehead, rectal), the date and time (default now) and the existing 200-character comment.
- **History:** readings in the merged timeline with edit and delete, and a Temperature filter chip.
- **Graph view under the Temperature filter:** a dot per reading with the usual range chips, plus plain figures for the range: lowest, highest, average (one decimal) and the number of readings. No bands, no limit lines, no colour thresholds and no judging words.
- **Export and backup:** CSV export and import (`timestamp,temperature_celsius,site,comment`) and a `bodyTemperatures` list per profile in the `.hjbackup` document.
- **Notice:** the Profile notice says the app records what you enter and does not assess it (sentence added by the oxygen change; this change extends it to temperature).
- **All text in English and Dutch.**
- **Celsius or Fahrenheit for display and entry:** a third device setting "Temperature unit" (System default, Celsius, Fahrenheit) in the Profile settings next to the unit system and glucose unit. Storage, CSV and backup stay Celsius.

## Capabilities
- **New Capability:** `health-metrics/body-temperature` (entry, validation, site, history retrieval, no assessment)
- **Modified Capability:** `entry-management` (temperature readings in History, edit rules, delete)
- **Modified Capability:** `entry-comments` (the comment on temperature readings)
- **Modified Capability:** `health-trends` (temperature graph view and figures)
- **Modified Capability:** `data-export` (temperature CSV export and import)
- **Modified Capability:** `data-backup` (temperature list in the backup document)
- **Modified Capability:** `compliance` (body temperature stays a personal log with no assessment)
- **Modified Capability:** `privacy` (same protection as other entries)
- **Modified Capability:** `localization` (text in English and Dutch)
- **Modified Capability:** `units-presentation` (temperature unit setting, region default, conversion)

## Impact
- **Database:** one Room migration, numbered as the next free number at implementation time (current database version + 1), so this change does not depend on the order in which features are built; it creates one table `body_temperatures` (id, profile id, timestamp, temperature in tenths of a degree Celsius as an integer, nullable site code, nullable comment). Existing tables are untouched; an upgrade only adds an empty table. A migration test opens a database of the previous version with data.
- **Stored data shape:** the backup document gains a `bodyTemperatures` list per profile; `schemaVersion` stays 1. `docs/backup-format/` (README, JSON schema, `hjbackup.py`, test vectors, recovery page) is updated.
- **Affected code:** `BodyTemperature` and `TemperatureSite` in `:domain` with validation, statistics and use cases (JVM tests including empty range and a single reading); entity, DAO, mapper, migration, repository methods and CSV adapters in `:data`; Log card, form, History row and edit dialog and the graph view in `:app`.
- **Dependencies and permissions:** none.
- **Docs:** README, CHANGELOG, `docs/dev/` pages, `docs/dev/zib-mapping.md` (row proposed in the drafting report), screenshots (English and Dutch, light and dark), `docs/backup-format/`.

## Sources
Read and used:
- zib BodyTemperature / Lichaamstemperatuur, through the Nictiz FHIR profile on [Simplifier](https://simplifier.net/stu7/zib-bodytemperature/$download?format=xml) (profile 2.1.3, based on zib v3.1, 2017): concepts TemperatureDateTime, TemperatureValue ("measured in degrees Celsius"), Comment ("Toelichting") and TemperatureType, the anatomical location where the temperature was measured, bound extensibly to the value set TemperatuurTypeCodelijst. Search summaries of the [zibs.nl wiki](https://www.zibs.nl/wiki/BodyTemperature-v3.1(2017EN)) and PDFs name SNOMED CT codes for rectal, oral, axillary and groin temperature (v3.0 also bladder and core). Ear and forehead were not seen in that list, so they are the app's own choices; this is recorded in the mapping page.
- [Thuisarts.nl, Ik heb koorts](https://www.thuisarts.nl/koorts/ik-heb-koorts): the anus is the most reliable place, an ear thermometer is less reliable, once a day is enough. Used only to keep the site list practical; the app gives none of this as advice.
- Consumer pages found by search (Max Vandaag, id.nl, Test-Aankoop) say that mouth, armpit, ear and forehead readings differ from rectal ones and are affected by use. This is the reason the site is recorded as a note: readings from different sites are not comparable. Secondary sources, used only as a reason.

Could not be read: the full zibs.nl wiki pages and PDFs (the server answered 503 every time), so the complete value set and element codes are unverified; the Thuisarts page on how to measure (404 on the guessed address); the NHG-Standaard texts (login needed); RIVM was not searched as nothing suggested it holds home-measurement guidance.

Deliberately NOT copied: fever cut-offs, age-based advice, advice on when to call a doctor or measure again, and any "normal" range. The app shows none of them ([ADR 0029](../../../docs/adr/0029-body-temperature-without-assessment.md)).

## Non-goals
- No reference range, band, limit line, alert, reminder or advice; no "fever" or "koorts" wording (Dutch and English), no "low", "high" or "normal".
- The site is a note only: nothing is derived from it, no figure changes, and readings from different sites are not converted or corrected.
- No import from thermometers, Bluetooth or Health Connect.
- Fahrenheit changes display and entry only: no stored value, CSV cell or backup value is ever in Fahrenheit.
- No link to medication or any other metric.

## Open questions
- The System default region list for Fahrenheit (US, BS, BZ, KY, PW, LR) is from general knowledge, not a verified source; the maintainer confirms it.
- Fahrenheit was decided in scope (display and entry only, 2026-10-21).
- The five site names and codes (EAR, MOUTH, ARMPIT, FOREHEAD, RECTAL) versus the zib vocabulary (oral, axillary, rectal) once the value set is read in full.
- Range 30.0 to 45.0 is the app's own entry guard against typos, not a clinical range; the maintainer confirms it.
- Dutch labels ("Lichaamstemperatuur", "Oor", "Mond", "Oksel", "Voorhoofd", "Rectaal") are for the final wording review.
