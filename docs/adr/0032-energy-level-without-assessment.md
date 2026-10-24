# 32. Energy Level as a 0-10 Rating Without Assessment

- **Date:** 2026-10-21
- **Status:** Proposed
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

People want to note how energetic they feel and look back over time. Dutch information on tiredness (Thuisarts.nl) stresses that tiredness has many causes and links to topics such as sleep problems, stress, burn-out, depression, ME/CVS and fibromyalgia; the NHG-Standaard Moeheid needs a login and was not read. Validated instruments exist (CIS, FSS, Borg) but they are clinical questionnaires with their own scoring and meaning. Showing thresholds, causes or syndrome wording would make the app a screening tool, against [ADR 0021](0021-medication-compliance-and-privacy.md). No zib for fatigue or energy was found; the nearest concept is Symptoom.

## Decision

- **A plain rating from 0 to 10**, whole numbers, direction "0 = no energy, 10 = full of energy". A fatigue-severity direction was rejected because it reads as a symptom instrument and needs the words tired or fatigue. Words appear only on the two ends; the numbers carry none. It is the person's own rating, not a score of any validated instrument, and no such instrument is implemented.
- **No assessment or screening.** No threshold, band, colour scale, alert, reminder, advice, cause, or fatigue-syndrome, CVS/ME or burn-out wording. The Profile notice says the app records what is entered and does not assess it.
- **Table `energy_levels`** (profile, timestamp, level, optional comment) added by a Room migration to the next free version (current database version + 1 at implementation time; no fixed order between features). Several entries per day are allowed.
- **Shared 0-10 picker:** a shared UI component (end texts, labels and accessibility text as parameters) used by Migraine intensity and Energy; whichever is built first creates it. One hue, plain figures (entries, average with one decimal, lowest, highest).
- **No heat map in v1.** A weekday by time-of-day view may come later, like the Migraine one, with no data-model change (time of day is derived from the timestamp).
- **Mood is separate** (different scale meaning). No shared field or combined view; a later optional daily check-in card could save two separate entries, as its own change.
- **Backup keeps `schemaVersion` 1** with an added `energyEntries` list ([ADR 0024](0024-portable-encrypted-backup.md)); CSV through `CsvQuoting`; comment through `EntryComment` ([ADR 0019](0019-entry-comments.md)).
- **zibs as a naming guide only** ([ADR 0026](0026-oxygen-saturation-without-assessment.md)); the mapping notes that no dedicated zib exists.

## Consequences

### Positive
- No threshold or "normal" to defend; no clinical instrument to license or validate.
- Little new code thanks to the shared picker.

### Negative / Trade-offs
- A single number says nothing about cause; people who want that must talk to their care team.
- Another CSV type and backup list to keep in step.

## Related
- [ADR 0015](0015-localized-viewmodel-messages.md), [ADR 0018](0018-smart-input-pickers.md), [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0025](0025-migraine-log-data-model.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)
- [OpenSpec change `add-energy-level`](../../openspec/changes/add-energy-level/proposal.md)
