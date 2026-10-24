# 28. Mood Log Without Assessment

- **Date:** 2026-10-21
- **Status:** Proposed
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

A mood log is the metric closest to a screening tool. Validated instruments for low mood exist (questionnaires with scored items and cut-offs) and Dutch guidance on somberheid describes when a person should contact the GP. Reproducing any of that would turn a personal log into a monitor and would conflict with the intended purpose in [ADR 0021](0021-medication-compliance-and-privacy.md). At the same time a short daily note is useful to people who want to look back or show a care provider a CSV file. Thuisarts pages on low mood were read and do not describe a mood diary; no zib for mood or feeling was found, though zibs.nl did not answer fully (see the change proposal).

## Decision

- **A record, not an assessment.** The Mood log stores a step 1 to 5, up to three feeling words and an optional comment, and shows them back as plain numbers, dates and the person's own words. No questionnaire, score, threshold, band, streak, alert, reminder, prompt, advice or statement about a condition. The app never reacts to a value. The `compliance` capability and the Profile notice say so.
- **Scale.** Five steps, stored only as the integer 1 to 5; labels are presentation text: "Very unpleasant", "Unpleasant", "Neutral", "Pleasant", "Very pleasant" (Dutch "Zeer onaangenaam" to "Zeer aangenaam"). Pleasant and unpleasant describe how a moment feels, not whether it is good, and are symmetric. Not used: good and bad, happy and sad as scale ends, faces or emoji, a slider, ten steps.
- **Optional stress level.** A second optional five-step row on the same entry (tension or pressure felt right now, "Not at all" to "Very much"), stored as the integer 1 to 5 or nothing in a nullable `stressLevel` column of `moods`. It is the person's own plain number: no questionnaire (PSS and similar rejected as scored instruments), no label, band or advice, no correlation with the mood step. A separate stress metric was rejected as a second card, table and migration for one number. It shows on the Mood graph through a Mood/Stress switch, with a different marker shape in the same hue.
- **Feeling words.** A fixed set of ten with stable codes (CALM, CONTENT, CHEERFUL, ENERGETIC, TIRED, TENSE, RESTLESS, IRRITATED, SAD, WORRIED), at most three per entry, never used in a figure. Free-text tags rejected for v1.
- **Several entries per day**, no uniqueness constraint. The chart line joins the mean of each day's entries. No calendar view in the first version; one may come later without a data-model change.
- **One new table `moods`** (profile, timestamp, level, stress level, feelings as comma-separated code names, comment) through a Room migration that takes the next free number at implementation time (current database version + 1), independent of the order of other metric changes. Read-mapping alternative (columns on an existing table or a JSON blob) rejected for the same reasons as in [ADR 0025](0025-migraine-log-data-model.md) and [ADR 0026](0026-oxygen-saturation-without-assessment.md). Unknown codes read from the database are ignored.
- **Stable codes in CSV and backup.** CSV `timestamp,mood_level,stress_level,feelings,comment` with `CsvQuoting.cell` and codes joined by `|`; backup list `moodEntries`, `schemaVersion` stays 1 ([ADR 0024](0024-portable-encrypted-backup.md)).
- **Statistics are pure functions in `:domain`**, with no clock and no interpretation.
- **Privacy.** Same encrypted storage, no logs, no notification or widget text containing a mood value ([ADR 0023](0023-database-encryption-and-app-lock.md)).
- **Mood and Energy level stay separate** metrics with their own tables, scales and charts, no shared field and no correlation; a later optional daily check-in card could save two entries with one timestamp, as a separate change.
- **Mindfulness.** Related in purpose, separate in data: no link, shortcut or suggestion in v1. A link would be a suggestion and so advice.
- **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)); it is never analysed.
- **Open, not decided here:** whether a fixed, value-independent pointer to the GP or a crisis line belongs in the notice. Sources: Thuisarts and 113 name 113 (also 0800-0113) day and night and the GP. Recorded default: no fixed line, still open for the final wording review; only the existing pointer to doctor, pharmacist and Thuisarts applies. This ADR moves to Accepted when that decision and the final wording are recorded.

## Consequences

### Positive
- No instrument, threshold or verdict to defend or keep up to date.
- Old databases and backups keep working; only an empty table is added.
- Figures are testable on the JVM; labels can change without a data change.

### Negative / Trade-offs
- People who want insight or a prompt when entries run low get none; the app does not help with that and says so.
- Five steps lose resolution compared with a ten-point scale.
- A fixed word set will not fit everyone; it can be extended later because words are codes and outside all figures.
- Another CSV file type and backup list to keep in step.
- Sources for the Dutch clinical context were only partly readable (NHG-Standaard behind a login, zibs.nl unavailable).

## Related
- [ADR 0015](0015-localized-viewmodel-messages.md), [ADR 0018](0018-smart-input-pickers.md), [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0023](0023-database-encryption-and-app-lock.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0025](0025-migraine-log-data-model.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)
- [OpenSpec change `add-mood-log`](../../openspec/changes/add-mood-log/proposal.md)
