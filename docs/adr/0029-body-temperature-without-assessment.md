# 29. Body Temperature Without Assessment

- **Date:** 2026-10-21
- **Status:** Proposed
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Body temperature is the metric most tied to a judgement word ("fever", "koorts") and to cut-offs that differ by age, by source and by measurement site. Thuisarts.nl names the anus as the most reliable site and the ear as less reliable; consumer sources add that mouth, armpit and forehead readings differ again. Any threshold would make the app pick one source and look like a monitor. [ADR 0026](0026-oxygen-saturation-without-assessment.md) already settles the pattern: record, do not assess. The Dutch standard (zib Lichaamstemperatuur, Nictiz) names the concepts: date and time, value in degrees Celsius, comment, and the anatomical location of the measurement. The full zib pages could not be read during drafting (503); the concepts above come from the Nictiz FHIR profile on Simplifier.

## Decision

- **No assessment.** Readings are plain numbers: no band, limit line, colour, alert, advice, and no words "fever" or "koorts" or low, high, normal. No age logic. The Profile notice says the app records what is entered and does not assess it. The `compliance` capability states this and a string scan test enforces it.
- **A small table `body_temperatures`** (profile, timestamp, integer tenths of a degree Celsius, optional site code, optional comment) added by one Room migration taking the next free number at implementation time (current database version + 1), so the order in which features are built does not matter. A metric of its own keeps the DAO, CSV and backup list simple; old data is untouched.
- **Integer tenths** for storage, statistics and CSV avoid floating-point drift; entry allows one decimal in 30.0 to 45.0, an input guard against typing mistakes and not a clinical range.
- **Site is a note**, a stable code (EAR, MOUTH, ARMPIT, FOREHEAD, RECTAL), never derived, converted or interpreted. Figures mix sites; no per-site split in this version. Rectal, oral (mouth) and axillary (armpit) correspond to zib value set entries as far as seen; ear and forehead were not seen there and are the app's own.
- **Fahrenheit is a display and entry unit only.** Storage, domain, CSV and backup stay Celsius (integer tenths); conversion happens only at the UI boundary, as `units-presentation` does for kg and lb and for mmol/L and mg/dL. A third device setting "Temperature unit" (System default, Celsius, Fahrenheit) is added; System default is Fahrenheit for US, BS, BZ, KY, PW and LR (from general knowledge) and Celsius otherwise. Entered Fahrenheit is converted to tenths of C with half-up rounding and validated on the converted value (30.0 to 45.0 C equals 86.0 to 113.0 F).
- **zibs are a naming guide**, recorded in [docs/dev/zib-mapping.md](../dev/zib-mapping.md); no claim of conformance, no exchange with care systems.
- **Backup keeps `schemaVersion` 1** with a `bodyTemperatures` list ([ADR 0024](0024-portable-encrypted-backup.md)); **comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)); wording follows [ADR 0021](0021-medication-compliance-and-privacy.md); localized messages [ADR 0015](0015-localized-viewmodel-messages.md); input pickers [ADR 0018](0018-smart-input-pickers.md).

## Consequences

### Positive
- No threshold to defend and no age table to keep current.
- Old databases and backups keep working.

### Negative / Trade-offs
- Users who want to know what a value means must ask their care team.
- Mixed-site figures are not strictly comparable; the site stays visible per row.
- A Fahrenheit value shown back can differ by 0.1 from what was typed, because storage is in tenths of C.
- Another CSV file type and backup list to keep in step.

## Related
- [ADR 0026](0026-oxygen-saturation-without-assessment.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md)
- [OpenSpec change `add-body-temperature`](../../openspec/changes/add-body-temperature/proposal.md)
