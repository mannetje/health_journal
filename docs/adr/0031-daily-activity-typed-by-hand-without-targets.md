# 31. Daily Activity Typed by Hand, Without Targets or Sensors

- **Date:** 2026-10-21
- **Status:** Proposed
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

People want to note steps or active minutes next to the other metrics. Dutch guidance exists (Gezondheidsraad, Beweegrichtlijnen 2017: about 150 minutes of moderate activity per week, strengthening twice a week, sit less) and most activity apps show goals, rings and streaks. The app is a personal log that does not assess or advise ([ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)). Reading sensors or Health Connect would add permissions and a data path the app has avoided: it has no INTERNET permission and a privacy test enforces permission and logging limits. No zib for physical activity was found on zibs.nl (closest: Mobiliteit, a functional description), though the index page could not be read in full.

## Decision

- **Typed by hand.** The person enters steps and/or active minutes. No sensor, step counting, Health Connect, `ACTIVITY_RECOGNITION` or network. No new permission; `PrivacyChecksTest` is unchanged.
- **No target of any kind.** No goal, norm, line, ring, streak, verdict or advice; the Beweegrichtlijn is cited only as a source that is deliberately not turned into a value. A personal goal set by the person is decided against for now; it would be its own later change with a compliance review.
- **One entry per day, a second save updates.** Day totals typed from a watch are running totals, so replace beats sum; one value per day keeps the graph and figures simple; a unique index on (profile, date) enforces it.
- **Fixed activity type list** (WALKING, CYCLING, SWIMMING, SPORT, HOUSEWORK_GARDEN, OTHER) as optional notes; free text stays in the comment; free text for the type is a possible later change only.
- **Table `daily_activities`** (profile, local date, nullable steps, nullable minutes, nullable type, nullable comment) added by a Room migration numbered at implementation time (current database version + 1; build order is the user's choice). Old data is untouched.
- **Figures** are plain: average per day logged, highest day, days logged, total; gaps are not zero.
- **Backup** keeps `schemaVersion` 1 with an added `activityDays` list ([ADR 0024](0024-portable-encrypted-backup.md)). **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)).

## Consequences

### Positive
- No norm to defend or keep up to date; no new permission or data path.
- Old databases and backups keep working.

### Negative / Trade-offs
- Typing by hand is slower than syncing; users who want sync are not served.
- Two walks in a day must be typed as one total.
- Some users will miss a goal; they must set one elsewhere.

## Related
- [ADR 0015](0015-localized-viewmodel-messages.md), [ADR 0018](0018-smart-input-pickers.md), [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)
- [OpenSpec change `add-daily-activity`](../../openspec/changes/add-daily-activity/proposal.md)
