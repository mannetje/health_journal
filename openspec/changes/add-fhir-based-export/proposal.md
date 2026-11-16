# Add a FHIR-based export

## Why
A person who keeps weight, blood pressure and pulse in the app may want to take them to another tool of their own choice. The CSV export is a plain table with no codes or units that another program can rely on, and the `.hjbackup` file is private to this app. The Dutch zib profiles and FHIR describe the same readings with agreed names, codes and units. A file shaped by them gives the person a more portable copy, on their own initiative and at their own risk.

This change is **a proposal for review, not yet agreed**. It asks for decisions on scope and wording (see Open questions) before any code is written.

## What Changes
- **One extra export in the History export dialog**, next to the CSV buttons: "Export (based on FHIR)" ("Exporteren (op FHIR gebaseerd)"). It writes one JSON file, a FHIR R4 `Bundle` of type `collection`, to a place the person picks with the system document picker, exactly like the other exports. It is created only after the person's action. Nothing is sent anywhere.
- **Content (first version):** weight, blood pressure and the pulse that belongs to a blood pressure entry, for the whole history or the range shown. Each reading is an `Observation` that declares the Nictiz profile it follows (`zib-BodyWeight`, `zib-BloodPressure`, `zib-HeartRate`) in `meta.profile`. A `Patient` resource carries only what the person chooses to include.
- **Not in the first version:** glucose (see Open questions), oxygen saturation, waist circumference, fluid intake, migraine, activity, mindfulness and medication.
- **A notice in the dialog and in the file's documentation**, in English and Dutch: the file is based on the Dutch zib and FHIR formats, has not been checked or approved by anyone, is not for use in care, is not encrypted, and is used at the person's own risk.
- **Wording rule:** the app, README, store text and docs never say the file works with, connects to or is compatible with care systems, EHR systems, GP systems or PGOs, and never call it a "FHIR export" without "based on".
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `fhir-based-export` (what is exported, the profile claims, identity handling, the notice, how it is checked)
- **Modified Capability:** `compliance` (an added requirement: the export makes no claim of use in care or interoperability)

## Impact
- **Database and backup:** none. No schema change, `schemaVersion` of the backup stays 1.
- **Affected code:** an export port and use case in `:domain` (reads existing entries, no new rules about values); a serialiser in `:data` next to the CSV adapters that builds the JSON; a button, the notice and the file-name suggestion in `:app`. No new dependency is planned: the JSON is written with the JSON library the app already uses, and the profiles are not bundled.
- **Network and permissions:** none. The `INTERNET` permission stays absent, and the merged-manifest test keeps checking that.
- **Docs:** `docs/dev/zib-mapping.md` (a status column entry per exported metric), `docs/dev/` export page, README, CHANGELOG, `docs/backup-format/` is untouched, screenshots of the dialog (English and Dutch, light and dark).
- **Developer tooling:** conformance is checked in a local test run with the official HL7 validator and the Nictiz package; neither ships in the app. How this is run, and whether it runs in CI, is an open question.

## Sources
Read and used:
- Nictiz `nictiz.fhir.nl.r4.zib2020` package, version 0.12.1-beta.1 (from packages.simplifier.net, read 2026-11-16). Every profile in it is marked `draft`. Profiles read: `zib-BodyWeight` (LOINC 29463-7, a quantity bound to a unit list of kg and g, optional note and clothing), `zib-BloodPressure` (LOINC 85354-9 with required systolic 8480-6 and diastolic 8462-4 components; position, cuff, method, body site and mean pressure optional), `zib-HeartRate` (LOINC 8867-4, a quantity), `zib-PulseRate`, `zib-Patient` (no required elements; BSN an optional slice), `zib-LaboratoryTestResult` (category `laboratory`, code bound to a Nictiz code list) and `pattern-VitalSigns` (a `vital-signs` category, a subject and an effective date and time are required).
- The zib pages on zibs.nl for the same zibs (read 2026-11-16), summarised in `docs/dev/zib-mapping.md`.
- MDCG 2019-11 Rev.1 (June 2025), official PDF: software that only stores, communicates or displays data without a medical purpose of its own is not medical device software; intended purpose follows the labelling, instructions and promotional statements.
- Regulation (EU) 2025/327 (EHDS): **read only on a secondary site (ringholm.com), not on EUR-Lex.** As summarised there, the wellness-application labelling (Articles 47 and 48) is triggered by a manufacturer's claim of interoperability with an EHR system, which this change avoids by wording. To be confirmed against the Official Journal text.

Could not be read or confirmed:
- EUR-Lex (the official EHDS text); any statement from the Inspectie, Nictiz or MedMij on file-only apps.
- Whether a suitable glucose code exists in the Nictiz `TestCode-en-Onderzoek-Codelijsten` for a home reading; the code list was not inspected in full.
- The HL7 FHIR trademark and naming rules for "based on FHIR".

## Non-goals
- No claim of conformance, certification or interoperability, and no statement that any system accepts the file.
- No sending, uploading, sharing integration or network call by the app; the person moves the file.
- No import of FHIR files.
- No interpretation, flag, range or assessment in the file: no `interpretation` element, no reference range, no status beyond `final`.
- No BSN or other national identifier, ever.
- No medication in this change.

## Open questions
1. **Wording:** "based on FHIR" in English, "op FHIR gebaseerd" in Dutch; the maintainer agrees the final strings.
2. **Identity in the file:** by default the `Patient` carries nothing that identifies the person except a generated id. Options the person ticks: birth date, sex (from the Profile). Name is never included by default. Confirm this default.
3. **Glucose:** include it only if a suitable code and a defensible profile claim exist. A home reading is not a laboratory test, so the claim may have to be weaker (a plain `Observation` with a LOINC code and no profile claim) or glucose left out. Confirm.
4. **Pulse link:** the pulse stored with a blood pressure entry becomes its own `HeartRate` Observation. Whether it points back to the blood pressure Observation (`derivedFrom`) is a design choice.
5. **Profile claim:** `meta.profile` names draft, beta profiles; confirm that is acceptable, with the exact package version stated in the file and in the docs.
6. **Verification:** the HL7 validator is a 205 MB Java program. Confirm it is run locally only, not in CI, in this change.
7. **EHDS and trademark:** the maintainer verifies the EHDS text on EUR-Lex and the HL7 naming rules before the store text is written.
