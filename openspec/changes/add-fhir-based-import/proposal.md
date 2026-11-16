# Add a FHIR-based import of the app's own file

## Why
A person who exported their weight, blood pressure and pulse with the FHIR-based export (change `add-fhir-based-export`) may want to bring that file back, for example on a new phone or after a reset, without the encrypted `.hjbackup`. Reading the app's own file back closes that loop and lets the export be checked end to end.

This change is **a proposal for review, not yet agreed**. It depends on `add-fhir-based-export` and does not start before that change is agreed.

## What Changes
- **One import in the History screen next to the exports:** "Import (based on FHIR)" ("Importeren (op FHIR gebaseerd)"). The person picks a JSON file with the system document picker. Nothing is read without that action and nothing is fetched from anywhere.
- **First version accepts only the app's own export**, recognised by the source marker in the `Bundle` and by the entry identifier on each Observation. Files from other tools are refused with a clear message. Broader import is a possible later change.
- **A preview before anything is saved:** the app shows how many weight, blood pressure and pulse entries are new, already present and skipped, and the person confirms. Nothing is saved before the confirmation.
- **Merge, never overwrite:** entries whose identifier already exists are kept as they are and counted as skipped, exactly like restoring a backup. The import runs in one database transaction.
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
Importing data a person got from a care provider would come closer to a patient accessing their own record, which is the intended use the EHDS ties to the EHR system definition (read in the Official Journal on 2026-11-16, see `add-fhir-based-export`). This change avoids it by scope and by wording: the import accepts only the app's own file, never mentions care providers, GP systems, hospitals or personal health environments, and claims no interoperability. Whether that is enough is not confirmed by any authority; the wording rules and the document scan test are the control.

## Non-goals
- No import of files from other tools or from care systems in this change.
- No network, sync, share-target or automatic import; the person picks the file.
- No overwriting and no deleting of existing entries.
- No import of a name, a national identifier, birth date or sex from the file.
- No interpretation, range or flag read from or added to the data.
- No claim of conformance, certification or interoperability.

## Open questions
1. **Recognising the own file:** by the source marker plus the entry identifier system. Confirm that both are required.
2. **Preview detail:** counts only, or also a list of the entries to be added. Counts only is proposed.
3. **Limits:** a maximum file size and a maximum number of entries to guard against oversized files. Proposed: 20 MB and 100,000 entries; confirm the numbers.
4. **Pulse:** a pulse Observation without a matching blood pressure Observation at the same time is imported as nothing in the first version (the app stores pulse only with blood pressure) and counted as skipped.
5. **Later broader import:** a separate change, with duplicate matching on time and value and a stricter wording review.
