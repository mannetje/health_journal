# 26. Oxygen Saturation Without Assessment, and zibs as a Naming Guide

- **Date:** 2026-10-21
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Weight, blood pressure, glucose and waist carry labels or lines from NHG and Voedingscentrum guidance ([ADR 0021](0021-medication-compliance-and-privacy.md) sets the neutral wording). For oxygen saturation no NHG band for home measurement was found, and the cut-offs quoted by hospitals and consumer pages differ by source and by person (for example with COPD). Showing any threshold would make the app choose one and look like a monitor. The Dutch health information standard (zib O2Saturatie, Nictiz) offers a shared vocabulary for the concept.

## Decision

- **No assessment.** Readings are stored and shown as plain numbers: no band, limit line, colour threshold, alert or wording such as low or normal. The Profile notice says the app records what is entered and does not assess it. The `compliance` capability states this.
- **A small table `oxygen_saturations`** (profile, timestamp, whole percent 50 to 100, optional pulse, optional context code, optional comment) added by `MIGRATION_6_7`, Room version 6 → 7. Read-mapping alternative considered: a column set on an existing table, rejected because a metric of its own keeps the DAO and the backup list simple. Old data is untouched.
- **Context is a note**, a stable code (AT_REST, AFTER_ACTIVITY, ASLEEP_OR_JUST_WOKE, OTHER) never derived or interpreted.
- **zibs are a naming and value-set guide**, recorded in [docs/dev/zib-mapping.md](../dev/zib-mapping.md). Fields and codes may follow a zib where it adds no function. The app makes no claim of conformance and does not exchange data with care systems; that would be a separate change with its own compliance review ([ADR 0021](0021-medication-compliance-and-privacy.md)).
- **Backup keeps `schemaVersion` 1** with an added `oxygenSaturations` list ([ADR 0024](0024-portable-encrypted-backup.md)).
- **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)).

## Consequences

### Positive
- No threshold to defend, no version of "normal" to keep up to date.
- Old databases and backups keep working.
- The zib mapping gives later metrics one place to check naming.

### Negative / Trade-offs
- Users who want a reference value must ask their care team; the app does not help with that.
- Another CSV file type and backup list to keep in step.
- The zib text was not read in full when this was written; the mapping page marks what is verified.

## Related
- [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0025](0025-migraine-log-data-model.md)
- [OpenSpec change `add-oxygen-saturation`](../../openspec/changes/add-oxygen-saturation/proposal.md)
