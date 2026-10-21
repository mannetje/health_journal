# zib mapping

The Dutch health information standards (zibs, *zorginformatiebouwstenen*, maintained by Nictiz) give shared names and value sets for health concepts. The app uses them as a **naming and value-set guide only**: it makes no claim of conformance and does not exchange data with care systems (see [ADR 0026](../adr/0026-oxygen-saturation-without-assessment.md) and the `compliance` capability). Back to the [developer docs](README.md).

> **Status: draft.** The zib documents could not be read in full when this page was started (the server did not answer). Rows marked *to verify* come from search summaries or from memory of the standard and must be checked against the published zib before they are relied on. Rows marked *own* have no zib counterpart.

## How to use this page
When a new metric or field is designed, look up its zib on [zibs.nl](https://zibs.nl), compare its concept names, units and value sets with the plan, and add a row here. Follow the zib where it costs nothing and adds no function; say here where and why the app differs.

## Mapping

| App metric | zib | Concepts we keep | Where we differ | Status |
|---|---|---|---|---|
| Oxygen saturation | [O2Saturatie](https://zibs.nl/images/2/2b/Nl.zorg.O2Saturatie-v4.0%282024NL%29.pdf) | saturation as a percentage; the time of the measurement; measurement site; oxygen given; a comment | both kept as optional notes with no interpretation; optional context is our own note | to verify |
| Pulse (in blood pressure and oxygen) | Polsfrequentie | beats per minute | stored as an optional field of the entry | to verify |
| Blood pressure | Bloeddruk | systolic and diastolic in mmHg | NHG band names in our own enum; no position or cuff site | to verify |
| Weight | Lichaamsgewicht | kg | BMI derived in the app | to verify |
| Waist circumference | none known | | | own |
| Glucose | Laboratoriumuitslag (glucose) | mmol/L | context FASTING / POSTPRANDIAL is ours | to verify |
| Migraine log | Hoofdpijn (to look up) | start, end, intensity | see [`add-migraine-log`](../../openspec/changes/add-migraine-log/proposal.md) | to verify |
| Mindfulness | none | | stores nothing | own |
| Sleep log | none matching (Slaapfunctie v3.0 is a clinical model; seen by title only) | none adopted | bed and wake times with computed duration, never judged; see [`add-sleep-log`](../../openspec/changes/add-sleep-log/proposal.md) | to verify (zibs.nl unreadable) |
| Mood log | none found (check Functionele of Mentale Status) | nothing adopted | own five-step scale and fixed feeling words, no score; see [`add-mood-log`](../../openspec/changes/add-mood-log/proposal.md) | own / to verify |
| Body temperature | [BodyTemperature / Lichaamstemperatuur](https://zibs.nl/wiki/BodyTemperature-v3.1(2018EN)) v3.1 | value in °C; date and time; measurement location as optional site; comment | ear and forehead are our own sites; mouth and armpit match oral and axillary; value kept as tenths; no assessment; see [`add-body-temperature`](../../openspec/changes/add-body-temperature/proposal.md) | to verify (value set not read in full) |
| Fluid intake | [Vochtbalans (FluidBalance)](https://zibs.nl/wiki/FluidBalance-v1.0.1(2024EN)) | total fluid intake in ml over a period | clinical intake and output reduced to entries by mouth; totals per day derived; see [`add-fluid-intake`](../../openspec/changes/add-fluid-intake/proposal.md) | to verify (page 503) |
| Daily activity | none found (closest: [Mobiliteit](https://zibs.nl/wiki/Mobiliteit-v3.3.1(2024NL)), ability to walk, not a count) | none | own steps, active minutes, type list, one entry per day, no goal; see [`add-daily-activity`](../../openspec/changes/add-daily-activity/proposal.md) | own (zibs.nl index unreadable, to re-check) |
| Energy level | none found (nearest: Symptoom) | timestamp, patient-reported complaint with a comment | own 0 to 10 rating, no severity or assessment; see [`add-energy-level`](../../openspec/changes/add-energy-level/proposal.md) | to verify |

## Not planned
- A FHIR or zib export for care providers. It moves towards a care context and would be its own change with a compliance review.
