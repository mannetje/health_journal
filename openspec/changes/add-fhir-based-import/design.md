# Design: FHIR-based import

## Context
The app is a personal, offline log with no `INTERNET` permission. Restore of a `.hjbackup` already merges without overwriting and reports added and skipped counts (spec `data-backup`, "Restore a backup"). This design gives the FHIR-based export file (`add-fhir-based-export`) a way back in, with the same merge rules and a preview, and reads files from other tools for the same three readings (decision of 2026-11-21).

## Decisions

### The app's own file and files from other tools
The reader accepts a JSON `Bundle`. Two kinds are told apart:
- **The app's own file:** the source marker names this app and the Observations carry an identifier in the system `urn:health-journal:entry`. Duplicates are found by that identifier.
- **Any other Bundle:** no marker and no entry identifier. Duplicates are found by time and value (see the next section). The file is read with exactly the same checks, so it cannot add anything the app's own file could not.

A file that is not a FHIR `Bundle`, or from which no reading can be read, is refused with one message ("No readings found in this file."). The wording never says where a file came from or that any system is supported. Foreign files are the reason the reader is strict about what it takes (below) and the notice says the file is read as it is.

### Reading, in order
1. Check the size against the limit before reading it all.
2. Parse the JSON. Invalid JSON or the wrong resource type refuses the file.
3. Look for the source marker. Present and naming this app: own file, and its package version must be known. Absent: foreign file. A marker naming another app is treated as a foreign file.
4. For each `Observation`: accept only the three codes (LOINC 29463-7, 85354-9, 8867-4), the category `vital-signs`, status `final` and an `effectiveDateTime`. Weight in `kg` or `g` is converted to kg. Read value and unit and convert to storage units (kg, mm[Hg], /min). A value outside the app's own validation range, a missing or unknown unit, or a missing field counts the entry as skipped, never as imported. An Observation without an entry identifier is a foreign reading.
5. A pulse Observation joins a blood pressure entry only when it has the same effective time; otherwise it is skipped (open question 4).
6. `Patient` and every element not listed above are not read into the database.

### Preview, then merge
The use case returns a preview: new, already present, skipped, with a reason count for skipped. The dialog shows the counts. Only on confirmation does a second step write the new entries in one database transaction, through the existing add-entry use cases so the existing validation applies. A failure leaves the data unchanged. The result is reported as "N added, M skipped".

An entry whose identifier already exists is kept as it is, even if the value differs, and is counted as skipped. For a reading without an entry identifier, "already present" means an existing entry of the same kind with the same time to the minute and the same value and unit; it is kept and counted as skipped, anything else is added. Times are compared as instants, so an offset in the file does not matter. Importing the same file twice adds nothing, own or foreign. Two readings in one foreign file with the same time and value are one reading.

### Layering
`:domain` gets an import port and a use case (preview, merge); it has no JSON or Android types. `:data` gets `FhirBasedBundleReader`, pure and unit tested, next to the writer from the export change. `:app` adds the button, the document picker, the preview and result dialogs and the notice. The reader and the writer share one constants object for codes, units and identifier systems so they cannot drift apart.

### Untrusted input
The file is untrusted. The reader streams or bounds its input (size limit, entry limit), does not follow any reference or URL in the file, resolves no `contained` resources, and never executes anything from it. Text fields (`note`) are stored through the existing comment rules (length limit, no markup interpretation).

### Notice and wording
Shown in the preview dialog, English: "This file is read as it is. You are responsible for the data you import. The makers of the app are not responsible for the data or for how the file was made." Dutch: "Dit bestand wordt gelezen zoals het is. Jij bent verantwoordelijk voor de gegevens die je importeert. De makers van de app zijn niet verantwoordelijk voor de gegevens of voor hoe het bestand is gemaakt." The button, strings and docs say "based on FHIR", never name care providers, GP systems or personal health environments, and make no claim of interoperability. The banned-phrase scan from the export change also covers the import strings.

### Strings (approved 2026-11-23)
Approved by the owner on 2026-11-23 with two changes: the confirm and result strings are Android plural resources (quantity one and other) in both languages, and Dutch says "metingen" throughout (also in the too-many-entries string). Plain, neutral and in "je" form in Dutch. No string names a source, a care system or a standard other than "based on FHIR", and none says a file is accepted or compatible. The notice is the one agreed above; the rest is new. Placeholders are the same in both languages (the key parity test checks this).

| Where | English | Dutch |
|---|---|---|
| Button in History | Import (based on FHIR) | Importeren (op FHIR gebaseerd) |
| Preview title | Check before importing | Controleer voor het importeren |
| Counts per reading (weight, blood pressure, pulse) | %1$s: %2$d new, %3$d already in the app, %4$d skipped | %1$s: %2$d nieuw, %3$d al in de app, %4$d overgeslagen |
| Skipped reasons | Unit not known / Value out of range / Not a reading the app can use / Pulse without blood pressure / Information missing | Eenheid onbekend / Waarde buiten het bereik / Geen meting die de app kan gebruiken / Pols zonder bloeddruk / Informatie ontbreekt |
| Notice | This file is read as it is. You are responsible for the data you import. The makers of the app are not responsible for the data or for how the file was made. | Dit bestand wordt gelezen zoals het is. Jij bent verantwoordelijk voor de gegevens die je importeert. De makers van de app zijn niet verantwoordelijk voor de gegevens of voor hoe het bestand is gemaakt. |
| Confirm button (plural: one / other) | Import %1$d entry / Import %1$d entries | %1$d meting importeren / %1$d metingen importeren |
| Cancel button | Cancel | Annuleren |
| Nothing new | Nothing new to import. | Er is niets nieuws om te importeren. |
| Result (plural on the added count) | %1$d added, %2$d skipped | %1$d toegevoegd, %2$d overgeslagen |
| Refused: no readings or not a FHIR file | No readings found in this file. | Geen metingen gevonden in dit bestand. |
| Refused: cannot be read (invalid or cut off) | This file could not be read. | Dit bestand kan niet worden gelezen. |
| Refused: too large | This file is too large to import. | Dit bestand is te groot om te importeren. |
| Refused: too many entries | This file holds too many entries to import. | Dit bestand bevat te veel metingen om te importeren. |
| Refused: own file from an unknown version | This file was made by a version of the app that this version cannot read. | Dit bestand is gemaakt met een versie van de app die deze versie niet kan lezen. |

The preview is plain text with the numbers in the sentence, so a screen reader reads each line whole; the confirm button repeats the number to import. With nothing new the confirm button is not shown.

## Verification plan
1. JVM tests: round trip (export then import into an empty store gives equal entries), importing twice adds nothing, existing entries are not overwritten, a wrong unit or out-of-range value is skipped, a Bundle from another tool with the three readings is read and a second import adds nothing (time and value match), a time match with a different value is added, a file that is not a Bundle or holds no readings is refused, an oversized file is refused, a `Patient` with a name leaves no trace in the database.
2. A fuzz-style test with truncated and malformed JSON: nothing is written and no crash.
3. Emulator pass: English and Dutch, light and dark, screenshots of the import button, preview and result.

## Risks
- **Draft profiles:** the export file may change with a later package version; the reader checks the package version in the source marker and refuses versions it does not know.
- **Foreign files and care use:** reading files from other tools weakens the scope argument against the EHDS EHR system definition. The control is the wording (no named source, no claim of acceptance or interoperability, own-use intent), the document scan test, and a new wording review before the foreign part is built. Reading other resource types stays a separate change.
- **False duplicates and false adds:** two real readings at the same minute with the same value are one reading; a corrected value at the same time is added next to the old one. The preview counts make both visible before saving.
- **Hostile files:** bounded size and entry count, no references followed, validation through existing rules.
