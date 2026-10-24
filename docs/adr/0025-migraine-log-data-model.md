# 25. Migraine Log Data Model

- **Date:** 2026-10-21
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The Migraine log is the first entry type with an optional end time, several multi-choice fields (head areas, symptoms, triggers) and two statistics views that group by weekday and time of day. `ActivitySession` is the nearest precedent but requires an end after the start. The log sits close to the line between a personal log and a medical device ([ADR 0021](0021-medication-compliance-and-privacy.md)): it must not diagnose, predict, count medication days against a limit or advise.

## Decision

- **Two new tables, `migraines` and `migraine_tags`, and domain entries `MigraineEntry` and `MigraineTag`** with a nullable `endTime`. Duration, weekday and time of day are derived in `:domain` and never stored. The old tables are not touched.
- **Room version 7 → 8 through `MIGRATION_7_8`** that only creates the tables. Read-mapping alternative considered and rejected: adding migraine columns to an existing table, or storing the entry as a JSON blob in preferences. A table keeps the DAO queries simple, keeps the data in the encrypted database ([ADR 0023](0023-database-encryption-and-app-lock.md)) and lets an upgrade only add empty tables.
- **Multi-choice fields are sets of stable codes** (enums in `:domain`, stored as a comma-separated text column of the code names). The same codes are used in the CSV and the backup document and are never translated. An unknown code on import rejects the row; an unknown code read from the database is ignored by the mapper so an older app does not crash on newer data.
- **Statistics are pure functions in `:domain`** taking the entries, a range and the locale's first day of the week, returning plain counts. They have no thresholds, no bands and no interpretation. A migraine is counted on its start day and start weekday and time of day.
- **Type is the person's own label** (with aura, without aura, recognized headache, other headache, not sure). The app never derives or suggests it, and the diagnosis codes used on paper diaries are not adopted.
- **Medication is free text with flags**, not a link to the pillbox, so no medication data is created and no reminder or compliance logic applies ([ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0022](0022-medication-reminders.md)). An optional `intensityAfter2Hours` (0 to 10, only with the took-something flag) records the pain score two hours later, as on the Thuisarts diary for seeing whether medication helps. It is a plain stored number: the app shows it next to the first intensity as entered but never computes a difference or derives an effect from it, because that would be an assessment of the medication. The person's own subjective words in the comment are not restricted.
- **Backup keeps `schemaVersion` 1.** The document gains a `migraines` list per profile. Readers already ignore unknown properties and a file without the list restores with none ([ADR 0024](0024-portable-encrypted-backup.md)). The published format documents, JSON schema, reference decoder and test vectors change together.
- **Notes the person adds stay notes.** A woke-up-with-it flag (the start time is an estimate, so such entries are counted aside in the time-of-day grid), pain characters, and relief methods (what the person did besides taking something) are stored as plain fields and codes. None of them is turned by the app into a score or a statement about what worked; the person's own comment is free text.
- **Personal tags for triggers and relief methods** let the person add their own words without a built-in library. Tags live in their own table, per profile, and entries refer to them by id so a rename is consistent; removing a tag archives it so old entries keep it. Alternatives rejected: storing the label text on each entry (a rename or typo fix would not reach old entries and counts would split) and a free-text "other" on each entry (cannot be counted). Tags are free text the app never suggests or interprets, which keeps the log a personal record ([ADR 0021](0021-medication-compliance-and-privacy.md)).
- **"This helped me" ticks** (medication and relief, optional) record the person's own view in the same spirit as the comment. They are stored as two integers and shown back; the app never counts, ranks or compares them and never prompts for them, so they stay a note and not an assessment ([ADR 0021](0021-medication-compliance-and-privacy.md)).
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
