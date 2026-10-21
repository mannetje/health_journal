# zib mapping

The Dutch health information standards (zibs, *zorginformatiebouwstenen*, maintained by Nictiz) give shared names and value sets for health concepts. The app uses them as a **naming and value-set guide only**: it makes no claim of conformance and does not exchange data with care systems (see [ADR 0026](../adr/0026-oxygen-saturation-without-assessment.md) and the `compliance` capability). Back to the [developer docs](README.md).

> **Status: draft.** The zib documents could not be read in full when this page was started (the server did not answer). Rows marked *to verify* come from search summaries or from memory of the standard and must be checked against the published zib before they are relied on. Rows marked *own* have no zib counterpart.

## How to use this page
When a new metric or field is designed, look up its zib on [zibs.nl](https://zibs.nl), compare its concept names, units and value sets with the plan, and add a row here. Follow the zib where it costs nothing and adds no function; say here where and why the app differs.

## Mapping

| App metric | zib | Concepts we keep | Where we differ | Status |
|---|---|---|---|---|
| Oxygen saturation | [O2Saturatie](https://zibs.nl/images/2/2b/Nl.zorg.O2Saturatie-v4.0%282024NL%29.pdf) | saturation as a percentage; the time of the measurement; a comment | measurement site and "oxygen given" left out of version 1; optional context is our own note; no interpretation | to verify |
| Pulse (in blood pressure and oxygen) | Polsfrequentie | beats per minute | stored as an optional field of the entry | to verify |
| Blood pressure | Bloeddruk | systolic and diastolic in mmHg | NHG band names in our own enum; no position or cuff site | to verify |
| Weight | Lichaamsgewicht | kg | BMI derived in the app | to verify |
| Waist circumference | none known | | | own |
| Glucose | Laboratoriumuitslag (glucose) | mmol/L | context FASTING / POSTPRANDIAL is ours | to verify |
| Migraine log | Hoofdpijn (to look up) | start, end, intensity | see [`add-migraine-log`](../../openspec/changes/add-migraine-log/proposal.md) | to verify |
| Mindfulness | none | | stores nothing | own |

## Not planned
- A FHIR or zib export for care providers. It moves towards a care context and would be its own change with a compliance review.
