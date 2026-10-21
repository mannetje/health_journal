# 27. Sleep Log Without Assessment

- **Date:** 2026-10-21
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Dutch public health information (Thuisarts) describes a paper sleep diary in which the person notes times and works out how long they slept. Sources also quote hour ranges and advice, which differ by age and person and were not verified for this app. The Dutch health information standard has a zib Slaapfunctie, a clinical model for sleep problems that could not be read in full when this was written. Showing a target or a verdict would make the app choose a standard and look like a monitor ([ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)).

## Decision

- **No assessment.** Entries are stored and shown as times, durations and plain figures: no target, band, line, score, alert, colour threshold, diagnosis or advice, and no words such as enough or good. The Profile notice says the app records what is entered and does not assess it.
- **A table `sleep_logs`** (profile, bed time, wake time, optional quality 1 to 5, optional times woken 0 to 30, optional comment) added by a Room migration to the next free version (the next free number at implementation time: current database version + 1, `MIGRATION_N_(N+1)`; the change does not depend on the order of other features). Read-mapping alternative considered: columns on an existing table or a preferences blob, rejected as in [ADR 0025](0025-migraine-log-data-model.md). Old data is untouched.
- **Two instants, derived duration.** Bed and wake time are stored as instants; duration is computed in `:domain` and never stored, so edits cannot make them disagree. Wake must be after bed and at most 24 hours later. Daylight-saving nights show their real length.
- **A night belongs to the local date of waking** (device zone) for sorting, ranges and the graph. Several sessions on one date are allowed; a nap is just another entry. The 24-hour maximum is a typo guard, not a clinical limit.
- **Average bed and wake time use a circular mean** of the clock time so values around midnight average correctly; an undefined mean is shown as "-".
- **Quality and times woken are notes.** They change no figure and are never interpreted.
- **Backup keeps `schemaVersion` 1** with an added `sleepLogs` list ([ADR 0024](0024-portable-encrypted-backup.md)); CSV uses ISO-8601 UTC and `CsvQuoting`.
- **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)). Localised messages follow [ADR 0015](0015-localized-viewmodel-messages.md); pickers follow [ADR 0018](0018-smart-input-pickers.md).
- **zib:** no zib for a plain sleep log was found; Slaapfunctie is a clinical function model and is not followed. Recorded in [docs/dev/zib-mapping.md](../dev/zib-mapping.md), to be verified.

## Consequences

### Positive
- No target to defend or keep up to date.
- Old databases and backups keep working.
- Statistics are pure functions testable on the JVM.

### Negative / Trade-offs
- Users who want a reference must ask their care team; the app does not help with that.
- Travelling across time zones can move an old night to another day.
- Another CSV file type and backup list to keep in step.
- The zib and NHG texts were not read, so alignment is not claimed.

## Related
- [ADR 0015](0015-localized-viewmodel-messages.md), [ADR 0018](0018-smart-input-pickers.md), [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0025](0025-migraine-log-data-model.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)
- [OpenSpec change `add-sleep-log`](../../openspec/changes/add-sleep-log/proposal.md)
