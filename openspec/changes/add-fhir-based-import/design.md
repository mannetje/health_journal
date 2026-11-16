# Design: FHIR-based import

## Context
The app is a personal, offline log with no `INTERNET` permission. Restore of a `.hjbackup` already merges without overwriting and reports added and skipped counts (spec `data-backup`, "Restore a backup"). This design gives the FHIR-based export file (`add-fhir-based-export`) a way back in, with the same merge rules and a preview.

## Decisions

### Only the app's own file
The reader accepts a `Bundle` of type `collection` whose source marker names this app and whose Observations carry an identifier in the system `urn:health-journal:entry`. Anything else is refused with one message: "This is not a file written by this app." No partial reading of foreign files. This keeps the first version small, testable and away from the care-record use (see the proposal).

### Reading, in order
1. Check the size against the limit before reading it all.
2. Parse the JSON. Invalid JSON or the wrong resource type refuses the file.
3. Check the source marker and `Bundle.type`.
4. For each `Observation`: accept only the three codes (LOINC 29463-7, 85354-9, 8867-4), the category `vital-signs`, status `final`, an `effectiveDateTime` and the entry identifier. Read value and unit and convert to storage units (kg, mm[Hg], /min). A value outside the app's own validation range, a missing or unknown unit, or a missing field counts the entry as skipped, never as imported.
5. A pulse Observation joins a blood pressure entry only when it has the same effective time; otherwise it is skipped (open question 4).
6. `Patient` and every element not listed above are not read into the database.

### Preview, then merge
The use case returns a preview: new, already present, skipped, with a reason count for skipped. The dialog shows the counts. Only on confirmation does a second step write the new entries in one database transaction, through the existing add-entry use cases so the existing validation applies. A failure leaves the data unchanged. The result is reported as "N added, M skipped".

An entry whose identifier already exists is kept as it is, even if the value differs, and is counted as skipped. Importing the same file twice adds nothing.

### Layering
`:domain` gets an import port and a use case (preview, merge); it has no JSON or Android types. `:data` gets `FhirBasedBundleReader`, pure and unit tested, next to the writer from the export change. `:app` adds the button, the document picker, the preview and result dialogs and the notice. The reader and the writer share one constants object for codes, units and identifier systems so they cannot drift apart.

### Untrusted input
The file is untrusted. The reader streams or bounds its input (size limit, entry limit), does not follow any reference or URL in the file, resolves no `contained` resources, and never executes anything from it. Text fields (`note`) are stored through the existing comment rules (length limit, no markup interpretation).

### Notice and wording
Shown in the preview dialog, English: "This file is read as it is. You are responsible for the data you import. The makers of the app are not responsible for the data or for how the file was made." Dutch: "Dit bestand wordt gelezen zoals het is. Jij bent verantwoordelijk voor de gegevens die je importeert. De makers van de app zijn niet verantwoordelijk voor de gegevens of voor hoe het bestand is gemaakt." The button, strings and docs say "based on FHIR", never name care providers, GP systems or personal health environments, and make no claim of interoperability. The banned-phrase scan from the export change also covers the import strings.

## Verification plan
1. JVM tests: round trip (export then import into an empty store gives equal entries), importing twice adds nothing, existing entries are not overwritten, a wrong unit or out-of-range value is skipped, a foreign Bundle is refused, an oversized file is refused, a `Patient` with a name leaves no trace in the database.
2. A fuzz-style test with truncated and malformed JSON: nothing is written and no crash.
3. Emulator pass: English and Dutch, light and dark, screenshots of the import button, preview and result.

## Risks
- **Draft profiles:** the export file may change with a later package version; the reader checks the package version in the source marker and refuses versions it does not know.
- **Scope creep to care files:** a request to read provider files is a separate change with its own legal review.
- **Hostile files:** bounded size and entry count, no references followed, validation through existing rules.
