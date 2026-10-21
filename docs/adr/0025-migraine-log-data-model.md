# 25. Migraine Log Data Model

- **Date:** 2026-10-21
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The Migraine log is the first entry type with an optional end time, several multi-choice fields (head areas, symptoms, triggers) and two statistics views that group by weekday and time of day. `ActivitySession` is the nearest precedent but requires an end after the start. The log sits close to the line between a personal log and a medical device ([ADR 0021](0021-medication-compliance-and-privacy.md)): it must not diagnose, predict, count medication days against a limit or advise.

## Decision

- **One new table `migraine_entries`, one new domain entry `MigraineEntry`** with a nullable `endTime`. Duration, weekday and time of day are derived in `:domain` and never stored. The old tables are not touched.
- **Room version 6 → 7 through `MIGRATION_6_7`** that only creates the table. Read-mapping alternative considered and rejected: adding migraine columns to an existing table, or storing the entry as a JSON blob in preferences. A table keeps the DAO queries simple, keeps the data in the encrypted database ([ADR 0023](0023-database-encryption-and-app-lock.md)) and lets an upgrade only add an empty table.
- **Multi-choice fields are sets of stable codes** (enums in `:domain`, stored as a comma-separated text column of the code names). The same codes are used in the CSV and the backup document and are never translated. An unknown code on import rejects the row; an unknown code read from the database is ignored by the mapper so an older app does not crash on newer data.
- **Statistics are pure functions in `:domain`** taking the entries, a range and the locale's first day of the week, returning plain counts. They have no thresholds, no bands and no interpretation. A migraine is counted on its start day and start weekday and time of day.
- **Type is the person's own label** (with aura, without aura, other headache, not sure). The app never derives or suggests it, and the diagnosis codes used on paper diaries are not adopted.
- **Medication is free text with flags**, not a link to the pillbox, so no medication data is created and no reminder or compliance logic applies ([ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0022](0022-medication-reminders.md)).
- **Backup keeps `schemaVersion` 1.** The document gains a `migraines` list per profile. Readers already ignore unknown properties and a file without the list restores with none ([ADR 0024](0024-portable-encrypted-backup.md)). The published format documents, JSON schema, reference decoder and test vectors change together.
- **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)).

## Consequences

### Positive
- Old databases and old backups keep working unchanged.
- The statistics are testable on the JVM with edge cases (empty range, one entry, ongoing entry, midnight boundaries).
- Nothing about the feature needs a new dependency or permission.

### Negative / Trade-offs
- Comma-separated code columns cannot be queried efficiently by single symptom in SQL; the counts are computed in memory over one profile's entries, which is small.
- An older app restoring a newer backup loses the migraines silently apart from the documented note.
- A new CSV file type adds import and export code that must be kept in step with the backup list.

## Related
- [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0023](0023-database-encryption-and-app-lock.md), [ADR 0024](0024-portable-encrypted-backup.md)
- [OpenSpec change `add-migraine-log`](../../openspec/changes/add-migraine-log/proposal.md)
