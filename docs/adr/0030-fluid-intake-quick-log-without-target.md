# 30. Fluid Intake as a Quick Log Without Target

- **Date:** 2026-10-21
- **Status:** Proposed
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Many water apps centre on a daily goal, a progress ring and reminders. Dutch sources give amounts for the general adult (Voedingscentrum quotes 1.5 to 2 litres a day, with extra for heat, illness or age) and care teams set a personal maximum for people with a fluid restriction, for example in heart failure; the figures differ by person and are set by their doctor. Any amount chosen by the app would be a recommendation in the app's voice and wrong for someone on a restriction, while a number entered by the person is theirs. The app's line, set in [ADR 0021](0021-medication-compliance-and-privacy.md) and followed in [ADR 0026](0026-oxygen-saturation-without-assessment.md), is to record and show what is entered and to assess nothing. The Nictiz zib Vochtbalans is a clinical building block for intake and output in care; no zib for a person's own drinks was found (its pages could not be read in full).

## Decision

- **Quick log, volume only.** One tap stores one entry of the person's own size; a custom entry takes 10 to 5000 ml and an optional comment. Type of drink is not in version 1; it is planned later as an optional type (default water) via a later migration with a nullable column read as water when empty, without nutrition framing.
- **Own sizes in preferences.** Three quick-add sizes and names, defaults 150, 250 and 500 ml, editable. They are settings, not health entries, so the preferences carry no entry data ([ADR 0023](0023-database-encryption-and-app-lock.md) and the `privacy` capability); they are not in the backup list.
- **Entries are the truth, totals are derived.** Table `fluid_intakes` (profile, timestamp, whole ml, optional comment) and `fluid_settings` (profile, optional daily limit) are added by one migration, the next free number at implementation time (current Room version + 1), so the decision does not depend on build order. The daily total is computed in `:domain` per local calendar day with an injected zone, so an edit or delete can never leave a stale total.
- **No target set by the app, no ring, reminder, verdict or advice.** The Today line, day-total rows and graph show plain numbers; the graph starts at 0; days without entries show nothing and are not counted as zero in the average. The `compliance` capability states this and a string scan test guards it.
- **Optional personal limit in v1, set only by the person** (100 to 10000 ml) for a fluid restriction from their care team. The field is empty until set; the app never suggests, prefills or recommends a number. It is stored per profile in the encrypted `fluid_settings` table (preferences are unencrypted), is in the backup as `fluidDailyLimitMl` and is not in the CSV. It shows as "Today: 900 of 1500 ml" and one thin dashed line in the graph's hue; above the limit nothing changes but the numbers: no colour, alert, notification, ring or wording such as over, exceeded, remaining or left. Clearing or changing it never alters entries. The line is the person's own number, not the app's recommendation.
- **Backup keeps `schemaVersion` 1** with a `fluidEntries` list ([ADR 0024](0024-portable-encrypted-backup.md)); CSV `timestamp,ml,comment` through `CsvQuoting`.
- **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)).

## Consequences

### Positive
- Fastest possible entry; works for a curious drinker and for a person on a restriction alike.
- No amount to defend or keep up to date; no reminders or notifications to build.
- Old databases and backups keep working.

### Negative / Trade-offs
- People who like goals and rings will not find them.
- No "left today" figure or warning for a person on a fluid restriction, by design; a plain number can still feel like a verdict.
- Many small entries make History long; day-total rows and the graph mitigate this.
- Another CSV file type and backup list to keep in step.

## Related
- [ADR 0015](0015-localized-viewmodel-messages.md), [ADR 0018](0018-smart-input-pickers.md), [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)
- [OpenSpec change `add-fluid-intake`](../../openspec/changes/add-fluid-intake/proposal.md)
