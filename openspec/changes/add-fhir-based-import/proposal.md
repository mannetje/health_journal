# Add a FHIR-based import

## Why
A person who exported their weight, blood pressure and pulse with the FHIR-based export (change `add-fhir-based-export`) may want to bring that file back, for example on a new phone or after a reset, without the encrypted `.hjbackup`. Reading the app's own file back closes that loop and lets the export be checked end to end. The person may also hold a file of the same three readings from another tool and want it in the app.

This change is **agreed on scope and wording (2026-11-21, see Decisions)**, including reading files from other tools. It depends on `add-fhir-based-export` and does not start before that change is built. Nothing is built yet.

## What Changes
- **One import in the History screen next to the exports:** "Import (based on FHIR)" ("Importeren (op FHIR gebaseerd)"). The person picks a JSON file with the system document picker. Nothing is read without that action and nothing is fetched from anywhere.
- **The app's own file and files from other tools:** a `Bundle` is read for the three known readings only. The app's own export is recognised by the source marker and by the entry identifier on each Observation, which gives exact duplicate detection. A file from another tool has neither, so its readings are matched on time and value (see Decisions). A file that is not a FHIR `Bundle` JSON, or holds none of the three readings, is refused with a clear message.
- **A preview before anything is saved:** the app shows how many weight, blood pressure and pulse entries are new, already present and skipped, and the person confirms. Nothing is saved before the confirmation.
- **Merge, never overwrite:** an entry that already exists (same identifier for the app's own file, same time and value for any other file) is kept as it is and counted as skipped, exactly like restoring a backup. The import runs in one database transaction.
- **Everything else is ignored, not stored:** a `Patient` (name, birth date, sex, identifiers), unknown resources and unknown elements are never written to the database.
- **A notice**, in English and Dutch, with the same own-risk wording as the export: the file is read as is, the person is responsible for what they import.
- **All text in English and Dutch.**

## Capabilities
- **New Capability:** `fhir-based-import` (what is read, the checks, the preview, merging, the notice)
- **Modified Capability:** `compliance` (an added requirement: the import makes no claim of use in care or interoperability)

## Impact
- **Database and backup:** none. No schema change; entries are added through the existing use cases and rules, so the same value validation applies.
- **Affected code:** an import port and use case in `:domain` (preview and merge, no new value rules); a reader in `:data` that parses the JSON with the JSON library already in the app and has no Android dependency; a button, the preview dialog and the notice in `:app`. No new dependency.
- **Network and permissions:** none. The `INTERNET` permission stays absent.
- **Export change:** the export writes an entry identifier on each Observation (amended in `add-fhir-based-export`).
- **Docs:** `docs/dev/` import page, README, CHANGELOG, `docs/dev/zib-mapping.md`, screenshots of the dialog and preview (English and Dutch, light and dark).

## Law and wording
Importing data a person got from a care provider would come closer to a patient accessing their own record, which is the intended use the EHDS ties to the EHR system definition (read in the Official Journal on 2026-11-16, see `add-fhir-based-export`). Accepting files from other tools removes the protection that "only the app's own file" gave. What remains is the wording: the import never mentions care providers, GP systems, hospitals or personal health environments, never says a file from any system is accepted, and claims no interoperability, and the intended use is the person's own log. Whether that is enough is not confirmed by any authority; the wording rules and the document scan test are the only control, so they are stricter for this change: the strings, README, store text and docs say what the app reads (three readings in a FHIR-based JSON file), never where files come from. Before the foreign part is built, the wording is reviewed again against the compliance spec.

## Non-goals
- No claim that files from any named tool or from care systems are accepted, and no list of supported sources.
- No network, sync, share-target or automatic import; the person picks the file.
- No overwriting and no deleting of existing entries.
- No import of a name, a national identifier, birth date or sex from the file.
- No interpretation, range or flag read from or added to the data.
- No claim of conformance, certification or interoperability.

## Decisions (agreed 2026-11-21)
1. **Recognising the files:** the app's own file is recognised by the source marker plus the entry identifier system `urn:health-journal:entry`, and then matches on that identifier. **Files from other tools are also read** (changed from "own file only"): any `Bundle` whose Observations have the three known codes, category `vital-signs`, status `final` and an effective date and time. Such a file has no entry identifier, so an entry counts as already present when an existing entry has the same time (to the minute) and the same value and unit; it is then kept as it is and counted as skipped. Anything else is added.
2. **Preview detail:** counts only (new, already present, skipped, with reasons for skipped).
3. **Limits:** 20 MB and 100,000 entries.
4. **Pulse:** a pulse Observation without a blood pressure Observation at the same time is skipped and counted (the app stores pulse only with blood pressure).
5. **Broader import:** reading other resource types (for example glucose or oxygen saturation) stays a separate change with its own review. Reading foreign files for the three readings is part of this change.
