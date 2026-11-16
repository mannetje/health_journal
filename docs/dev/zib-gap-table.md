# zib gap table

What it would take to bring the app's existing metrics closer to the Dutch zibs. This page is a **planning aid**, not a decision. The mapping itself, with the zib versions read, is in [zib-mapping.md](zib-mapping.md); this page adds how far each metric is from its zib and what closing the gap would cost. Back to the [developer docs](README.md).

> **Status: draft for review.** The "Gap" column comes from [zib-mapping.md](zib-mapping.md) (zib pages read on zibs.nl, versions named there). The "Effort" and "Risk" columns are **an assessment, not measured**: the code was not inspected for this page, so the list of what a change touches is what I expect, to be confirmed in the OpenSpec change that would make it. No zib is claimed to be implemented; the app still makes no claim of conformance (see the `compliance` capability).

## The three levels
| Level | What changes | Touches |
|---|---|---|
| **1. Closer in the file** | The FHIR-based export (change `add-fhir-based-export`) maps stored values to zib names, codes and units. | Only the export mapper in `:data`. No database, UI or backup change. |
| **2. Add optional zib fields** | The app stores a zib concept it skips today, for example blood pressure position. | Expected: domain model, a Room migration, the backup document and format page, the entry dialogs, EN/NL strings, tests, screenshots. |
| **3. Change the data model** | The app's entry shape is replaced by the zib's shape. | Everything in level 2, plus a reason the person gains something. Usually not worth it. |

The rule on [zib-mapping.md](zib-mapping.md) stays: follow a zib where it costs nothing and adds no function, and say where and why the app differs.

## Gap table

| Metric | zib (version read) | Gap to the zib | Level | Effort | Risk | Suggestion |
|---|---|---|---|---|---|---|
| Weight | Lichaamsgewicht v4.0.1 | Only the optional clothing (Kleding) is missing. Value, unit, time and comment match. | 1 done in the export; 2 for clothing | Level 2: medium for a rarely used field | Low | Leave clothing out; no reader gains from it |
| Blood pressure | Bloeddruk v5.1 | Optional concepts missing: position (Houding), cuff type, measurement method, body location, mean pressure, Korotkoff end point. Systolic, diastolic, time and comment match. | 1 done in the export; 2 for the optional fields | Level 2: medium to high per field (migration, backup, dialog) | Medium: cuff type and Korotkoff read as clinical detail, against the no-assessment stance | Consider position only, as an optional pick list; leave the rest |
| Pulse | Hartfrequentie v3.4.1 | Stored as an optional field of a blood pressure or oxygen entry, not as an entry of its own; no method or regularity. | 1 (the export writes a separate heart rate Observation) | Level 1: low | Low | No model change |
| Oxygen saturation | O2Saturatie v4.0 | Value, time, comment, extra oxygen and location are kept; the location list is our own (the zib refers to a general anatomical location list). Pulse and situation are our own notes. | 1, in a later export change | Low, once the matching profile in the Nictiz package has been checked (not yet done) | Low | Add to the export after weight, blood pressure and pulse |
| Body temperature (proposed, not built) | Lichaamstemperatuur v3.1.2 | Planned sites are all in the zib's value set; the zib offers more sites than the app. | 1 | Low: map the site enum to its code in the export | Low | Do it when the metric ships |
| Glucose | LaboratoriumUitslag v7.1 | The zib is a laboratory result with a sample and a test code; the app records a home reading. Context (fasting, after meal) is ours. | 3 | High, and the claim would be weaker than the data | Medium: the zib's lab context is a clinical one | Leave out of the export and the model (see the open question in `add-fhir-based-export`) |
| Fluid intake | Vochtbalans v1.0.1 | The zib is a clinical total over a period with several intake routes and output; the app logs single drinks by mouth and derives a day total. | 3 | High | Medium: the zib's purpose is clinical | Leave as it is |
| Migraine | none (Pijnscore v4.1 and Symptoom v2.0 are the nearest) | No matching zib; only the 0 to 10 scale is borrowed. | none | not applicable | not applicable | Leave as it is |
| Waist circumference, mindfulness, activity recording, daily activity (proposed), sleep (proposed), mood (proposed), energy (proposed) | none found | Own models. Nothing to move closer to. | none | not applicable | not applicable | Leave as they are |
| Medication | not yet looked up | The zibs on medication agreement and use are candidates; the app's schedule, pillbox and intake log are its own model. | unknown | unknown | Medium: medication is the area closest to care | Look up the zibs before any decision |

## Suggested order
1. **Level 1 first**, as part of the export work: weight, blood pressure and pulse, then temperature and oxygen saturation. It costs no migration and no UI.
2. **Blood pressure position** is the one level 2 field worth a separate OpenSpec change, because it changes how a reading is read. Decide it on its own merits, not to fit the zib.
3. **Level 3 only if a metric is redesigned for its own reasons.**

## Before any level 2 or 3 change
- Write or amend an ADR if the policy "naming and value-set guide only" ([ADR 0026](../adr/0026-oxygen-saturation-without-assessment.md) and the `compliance` capability) is to change.
- One OpenSpec change per metric group, with a compliance check of the new wording.
- A new field means a backup-format change: the page in [`docs/backup-format/`](../backup-format/README.md) and the `schemaVersion` rules apply.

## Not covered
- Whether the zib2020 FHIR package has a profile for oxygen saturation and body temperature has not been checked.
- The zib versions are those read on 2026-11-16 (2026-1 pre-publication); newer ones may exist.
