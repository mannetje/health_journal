# zib mapping

The Dutch health information standards (zibs, *zorginformatiebouwstenen*, maintained by Nictiz) give shared names and value sets for health concepts. The app uses them as a **naming and value-set guide only**: it makes no claim of conformance and does not exchange data with care systems (see [ADR 0026](../adr/0026-oxygen-saturation-without-assessment.md) and the `compliance` capability). Back to the [developer docs](README.md).

> **Status: partly checked.** The oxygen saturation row was checked against the published zib on 2026-11-03. The other rows marked *to verify* come from search summaries or from memory of the standard and must be checked against the published zib before they are relied on. Rows marked *own* have no zib counterpart.

## How to use this page
When a new metric or field is designed, look up its zib on [zibs.nl](https://zibs.nl), compare its concept names, units and value sets with the plan, and add a row here. Follow the zib where it costs nothing and adds no function; say here where and why the app differs.

## Mapping

| App metric | zib | Concepts we keep | Where we differ | Status |
|---|---|---|---|---|
| Oxygen saturation | [O2Saturatie](https://zibs.nl/images/2/2b/Nl.zorg.O2Saturatie-v4.0%282024NL%29.pdf) v4.0 (2024, read in full 2026-11-03) | SpO2Waarde, a percentage (our whole number 50 to 100); SpO2SaturatieDatumTijd, the time of the measurement; Toelichting, a comment; ExtraZuurstofToediening, a yes/no for extra oxygen (our "Oxygen given" tick); Meetlocatie, the body location (our site: finger, toe, earlobe, the three places the zib definition names) | the zib has no value list for the location (it refers to AnatomischeLocatie), so our three sites are a short own list; pulse and the situation (at rest, after activity, asleep or just woke, other) have no zib counterpart and are our own optional notes; the zib states a usual value for healthy people in its concept text, which the app never shows or uses; no assessment; see [ADR 0026](../adr/0026-oxygen-saturation-without-assessment.md) | checked against the zib |
| Pulse (in blood pressure and oxygen) | Polsfrequentie | beats per minute | stored as an optional field of the entry | to verify |
| Blood pressure | Bloeddruk | systolic and diastolic in mmHg | NHG band names in our own enum; no position or cuff site | to verify |
| Weight | Lichaamsgewicht | kg | BMI derived in the app | to verify |
| Waist circumference | none known | | | own |
| Glucose | Laboratoriumuitslag (glucose) | mmol/L | context FASTING / POSTPRANDIAL is ours | to verify |
| Migraine log | Hoofdpijn (to look up) | start, end, intensity | see [`add-migraine-log`](../../openspec/changes/archive/2026-10-24-add-migraine-log/proposal.md) | to verify |
| Mindfulness | none | | stores nothing | own |
| Sleep log | none matching (Slaapfunctie v3.0 is a clinical model; seen by title only) | none adopted | bed and wake times with computed duration, never judged; see [`add-sleep-log`](../../openspec/changes/add-sleep-log/proposal.md) | to verify (zibs.nl unreadable) |
| Mood log | none found (check Functionele of Mentale Status) | nothing adopted | own five-step scale and fixed feeling words, no score; see [`add-mood-log`](../../openspec/changes/add-mood-log/proposal.md) | own / to verify |
| Body temperature | [BodyTemperature / Lichaamstemperatuur](https://zibs.nl/wiki/BodyTemperature-v3.1(2018EN)) v3.1 | value in °C; date and time; measurement location as optional site; comment | ear and forehead are our own sites; mouth and armpit match oral and axillary; value kept as tenths; no assessment; see [`add-body-temperature`](../../openspec/changes/add-body-temperature/proposal.md) | to verify (value set not read in full) |
| Fluid intake | [Vochtbalans (FluidBalance)](https://zibs.nl/wiki/FluidBalance-v1.0.1(2024EN)) v1.0.1 (2024, read in full 2026-11-14; a newer 2026-1 exists) | FluidTotalIn, the total intake in ml (our entries by mouth, summed per day); FluidBalanceStartTime and StopTime, the period (our local calendar day); Comment (our comment) | the zib is a clinical total over a period, with intake by mouth, drip or tube feeding and a separate FluidTotalOut; we log single drinks by mouth only, store no output and no balance, and derive the day total; drink type, the person's own limits and the position label have no zib counterpart; the zib's purpose (guiding drinking plans) is clinical and the app gives none; see [ADR 0030](../adr/0030-fluid-intake-quick-log-with-personal-limits.md) and the [Fluid page](fluid.md) | checked against the zib |
| Daily activity | none found (closest: [Mobiliteit](https://zibs.nl/wiki/Mobiliteit-v3.3.1(2024NL)), ability to walk, not a count) | none | own steps, active minutes, type list, one entry per day, no goal; see [`add-daily-activity`](../../openspec/changes/add-daily-activity/proposal.md) | own (zibs.nl index unreadable, to re-check) |
| Energy level | none found (nearest: Symptoom) | timestamp, patient-reported complaint with a comment | own 0 to 10 rating, no severity or assessment; see [`add-energy-level`](../../openspec/changes/add-energy-level/proposal.md) | to verify |

## Not planned
- A FHIR or zib export for care providers. It moves towards a care context and would be its own change with a compliance review.
