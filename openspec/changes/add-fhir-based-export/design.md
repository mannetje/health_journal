# Design: FHIR-based export

## Context
The app is a personal, offline log (ADR 0021, ADR 0026). It makes no claim to follow the zibs ([zib-mapping](../../../docs/dev/zib-mapping.md)), exchanges nothing with care systems and has no `INTERNET` permission. CSV and `.hjbackup` are the existing exports. This design adds a third that follows the Nictiz FHIR profiles as closely as the stored data allows, without claiming conformance or interoperability.

## Decisions

### Technical conformance, labelled "based on"
The file is built to validate against the Nictiz `zib2020` profiles, and each resource says which profile it follows in `meta.profile`. The label to the person stays "based on FHIR": the profiles are drafts (package 0.12.1-beta.1), and no authority has approved the output. A conformance statement would be stronger than anything the app can back up, so it is not made.

### Bundle shape
One `Bundle` of type `collection`, no `request` or `response` entries, with `Patient` first, then the `Observation` resources oldest first. Resource ids are UUIDs derived from the entry ids so a repeated export of the same data gives the same ids. Each Observation also carries the app's entry identifier in `identifier` (system `urn:health-journal:entry`), which the separate import change (`add-fhir-based-import`) uses to skip entries that already exist. Times are written with an offset (`2026-11-16T10:36:00+01:00`). The offset is the one stored with the entry if the app stores a zone or offset for it; if it stores only an instant, the offset is that of the device's zone at export time, and the docs say so. Which of the two applies is checked in the code before implementation (task 1.0); a person who travelled or changed zone could otherwise get a wrong offset.

The `Bundle` carries `timestamp` (the export time) and a `meta.source` or `meta.tag` that names the app and its version and the profile package version it was built against (for example "Health Journal 1.x, package nictiz.fhir.nl.r4.zib2020 0.12.1-beta.1"). This makes a file traceable to the build that wrote it. It says nothing about conformance and uses no claim wording.

### Range
The export dialog offers a choice of range: all entries, or the range currently selected in History. The default is all entries. The suggested file name is `healthjournal-<yyyy-mm-dd>.json` for both choices; it carries no "FHIR" brand.

### Mapping
| App | Resource and profile | Notes |
|---|---|---|
| Weight | `Observation`, `zib-BodyWeight` | LOINC 29463-7; `valueQuantity` in `kg` (UCUM); comment as `note`; no clothing |
| Blood pressure | `Observation`, `zib-BloodPressure` | LOINC 85354-9; components 8480-6 and 8462-4 in `mm[Hg]`; comment as `note`; no position, cuff, method, site or mean |
| Pulse in a blood pressure entry | `Observation`, `zib-HeartRate` | LOINC 8867-4; `valueQuantity` in `/min`; same time as the blood pressure; no link to the blood pressure Observation (no `derivedFrom`, no `hasMember`): the zib does not define one and it avoids a modelling choice the validator could question |
| Person | `Patient`, `zib-Patient` | generated id; birth date and sex only if the person chooses; never name by default, never a BSN |

Every Observation has the category `vital-signs`, `status` `final`, a `subject` reference to the Patient and an `effectiveDateTime` (required by `pattern-VitalSigns`). Display units follow storage: weight is exported in kg and glucose, if added, in mmol/L, whatever the person's display setting is (ADR 0014).

No `interpretation`, `referenceRange` or flag is ever written, which keeps the file consistent with the compliance rules.

### Glucose is not in the first version
`zib-LaboratoryTestResult` requires the category `laboratory`, a code from a Nictiz code list and describes a laboratory test. A home reading is not one. Options: leave glucose out, export it as a plain `Observation` with a LOINC code and no profile claim, or claim the profile with a caveat. The conservative default is to leave it out until the code list is read, and to say so in the docs.

### Identity
The `Patient` is minimal by default. Birth date and sex are opt-in through two tick boxes in the export dialog. The name is not offered. No national identifier is ever written. The file says nothing about who the person is beyond what they chose, and the notice tells them the file is not encrypted.

### Layering
`:domain` gets a read-only port that returns the entries to export and a use case that selects the range. `:data` gets `FhirBasedBundleWriter`, which maps domain entries to JSON and has no Android dependency. `:app` adds the button, the range choice, the two identity options, the short notice with its expander and the document picker. The writer is pure and unit tested.

### Verification plan
1. JVM tests assert structure: required elements present, codes and units as in the table, ids stable, times with offsets, no `interpretation`.
2. A local conformance run: the official HL7 validator (`validator_cli.jar`) with the Nictiz package, over sample bundles produced by a test, with no errors expected. The validator and package are not committed or bundled; the run is documented in `docs/dev/testing.md`.
3. A test that fails if a bundle contains a field outside the allowed list (no name, no interpretation, no identifier other than the entry identifier on each Observation and the generated `Patient` id).

### Notice and wording
The dialog shows a short notice above the button and a "More" control that expands the full notice. The full notice is also written in the docs. Short, English: "Based on the Dutch zib and FHIR formats. Not for use in care, not encrypted. Use it at your own risk." Short, Dutch: "Gebaseerd op de Nederlandse zib- en FHIR-formaten. Niet bedoeld voor gebruik in de zorg, niet versleuteld. Gebruik is op eigen risico." The expanded text is the full notice below, which includes the statement that the person is responsible for the data and the makers are not. The expander is a standard accessible control (role, state, touch target), and the full text is reachable with one tap before the export button is used. Full, English: "This file is based on the Dutch zib and FHIR formats. It has not been checked or approved by anyone, is not for use in care and is not encrypted. Use it at your own risk. You are responsible for the data you enter and export. The makers of the app are not responsible for the data or for how the file is used." Dutch: "Dit bestand is gebaseerd op de Nederlandse zib- en FHIR-formaten. Het is door niemand gecontroleerd of goedgekeurd, niet bedoeld voor gebruik in de zorg en niet versleuteld. Gebruik is op eigen risico. Jij bent verantwoordelijk voor de gegevens die je invoert en exporteert. De makers van de app zijn niet verantwoordelijk voor de gegevens of voor het gebruik van het bestand." The compliance wording review applies.

A responsibility statement describes who decides and who answers for the data. It cannot remove liability that the law does not allow anyone to waive (for example under Dutch and EU consumer and product liability rules), so it supports the other wording rules and is not a replacement for them. Whether the same sentence should also appear once for the whole app (the Profile notice) is left for a separate change.

## Risks
- **Draft profiles:** a later package may change element names or required elements; the file states the package version, and the docs say it is a snapshot.
- **A claim by implication:** a store or README sentence that mentions care systems would defeat the wording rule. The doc scan test fails on the banned phrases.
- **Identity:** a file with a birth date and sex plus readings is personal data the person controls; the notice states it is unencrypted.
- **EHDS:** read in the Official Journal. The labelling duty (Articles 47 and 48) depends on a manufacturer's claim of interoperability with an EHR system, and the EHR system definition on the manufacturer's intended use. Both are avoided by the wording rules, not by the file content, so those rules are the control and the scan test guards them. The Regulation does not apply before 26 March 2027 (Articles 47 to 49 not before 2029).
- **Trademark:** HL7 allows naming the standard to describe the file, with the registered-trademark form and its endorsement sentence at first use in the docs. The button text "based on FHIR" follows that; the sentence goes in the README and the docs page, and `FHIR` is not part of the app name or a file-name brand.
