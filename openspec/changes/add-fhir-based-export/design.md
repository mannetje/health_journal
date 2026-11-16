# Design: FHIR-based export

## Context
The app is a personal, offline log (ADR 0021, ADR 0026). It makes no claim to follow the zibs ([zib-mapping](../../../docs/dev/zib-mapping.md)), exchanges nothing with care systems and has no `INTERNET` permission. CSV and `.hjbackup` are the existing exports. This design adds a third that follows the Nictiz FHIR profiles as closely as the stored data allows, without claiming conformance or interoperability.

## Decisions

### Technical conformance, labelled "based on"
The file is built to validate against the Nictiz `zib2020` profiles, and each resource says which profile it follows in `meta.profile`. The label to the person stays "based on FHIR": the profiles are drafts (package 0.12.1-beta.1), and no authority has approved the output. A conformance statement would be stronger than anything the app can back up, so it is not made.

### Bundle shape
One `Bundle` of type `collection`, no `request` or `response` entries, with `Patient` first, then the `Observation` resources oldest first. Resource ids are UUIDs derived from the entry ids so a repeated export of the same data gives the same ids. Times are written with the offset of the person's zone at the time of the reading (`2026-11-16T10:36:00+01:00`).

### Mapping
| App | Resource and profile | Notes |
|---|---|---|
| Weight | `Observation`, `zib-BodyWeight` | LOINC 29463-7; `valueQuantity` in `kg` (UCUM); comment as `note`; no clothing |
| Blood pressure | `Observation`, `zib-BloodPressure` | LOINC 85354-9; components 8480-6 and 8462-4 in `mm[Hg]`; comment as `note`; no position, cuff, method, site or mean |
| Pulse in a blood pressure entry | `Observation`, `zib-HeartRate` | LOINC 8867-4; `valueQuantity` in `/min`; same time as the blood pressure; see open question 4 |
| Person | `Patient`, `zib-Patient` | generated id; birth date and sex only if the person chooses; never name by default, never a BSN |

Every Observation has the category `vital-signs`, `status` `final`, a `subject` reference to the Patient and an `effectiveDateTime` (required by `pattern-VitalSigns`). Display units follow storage: weight is exported in kg and glucose, if added, in mmol/L, whatever the person's display setting is (ADR 0014).

No `interpretation`, `referenceRange` or flag is ever written, which keeps the file consistent with the compliance rules.

### Glucose is not in the first version
`zib-LaboratoryTestResult` requires the category `laboratory`, a code from a Nictiz code list and describes a laboratory test. A home reading is not one. Options: leave glucose out, export it as a plain `Observation` with a LOINC code and no profile claim, or claim the profile with a caveat. The conservative default is to leave it out until the code list is read, and to say so in the docs.

### Identity
The `Patient` is minimal by default. Birth date and sex are opt-in through two tick boxes in the export dialog. The name is not offered. No national identifier is ever written. The file says nothing about who the person is beyond what they chose, and the notice tells them the file is not encrypted.

### Layering
`:domain` gets a read-only port that returns the entries to export and a use case that selects the range. `:data` gets `FhirBasedBundleWriter`, which maps domain entries to JSON and has no Android dependency. `:app` adds the button, the two options, the notice and the document picker. The writer is pure and unit tested.

### Verification plan
1. JVM tests assert structure: required elements present, codes and units as in the table, ids stable, times with offsets, no `interpretation`.
2. A local conformance run: the official HL7 validator (`validator_cli.jar`) with the Nictiz package, over sample bundles produced by a test, with no errors expected. The validator and package are not committed or bundled; the run is documented in `docs/dev/testing.md`.
3. A test that fails if a bundle contains a field outside the allowed list (no name, no identifier, no interpretation).

### Notice and wording
Shown in the dialog above the button and written in the docs. English: "This file is based on the Dutch zib and FHIR formats. It has not been checked or approved by anyone, is not for use in care and is not encrypted. Use it at your own risk." Dutch: "Dit bestand is gebaseerd op de Nederlandse zib- en FHIR-formaten. Het is door niemand gecontroleerd of goedgekeurd, niet bedoeld voor gebruik in de zorg en niet versleuteld. Gebruik is op eigen risico." The compliance wording review applies.

## Risks
- **Draft profiles:** a later package may change element names or required elements; the file states the package version, and the docs say it is a snapshot.
- **A claim by implication:** a store or README sentence that mentions care systems would defeat the wording rule. The doc scan test fails on the banned phrases.
- **Identity:** a file with a birth date and sex plus readings is personal data the person controls; the notice states it is unencrypted.
- **EHDS and trademark points** are unconfirmed; see the proposal.
