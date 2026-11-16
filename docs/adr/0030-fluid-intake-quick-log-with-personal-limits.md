# 30. Fluid Intake as a Quick Log With Personal Limits

- **Date:** 2026-10-21 (revised 2026-11-14)
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Many water apps centre on a daily goal, a progress ring and reminders. Dutch sources show why the app cannot pick a number. [Voedingscentrum](https://www.voedingscentrum.nl/encyclopedie/vocht.aspx) gives general minimums for adults (at least 1.4 litres a day from drinks, men 1.4 to 1.8, more with exercise, heat or illness). A huisarts or cardiologist sets an individual **maximum** for people who may not drink more because of a condition such as heart failure, commonly 1.5 to 2 litres in hospital leaflets. Other people need a **minimum** intake. [Thuisarts](https://www.thuisarts.nl/uitdroging) names the risks of drinking too little and gives no amount. A general minimum is wrong for someone on a restriction, and a restriction is wrong for everybody else, so any amount chosen by the app would be a recommendation in the app's voice. A number entered by the person is theirs.

The first draft of this decision let the person enter one limit and showed only plain numbers. Two use cases came out of the review: logging the day's intake, and setting a minimum, a maximum or both, with a way to see per day how the intake was. The app's line, set in [ADR 0021](0021-medication-compliance-and-privacy.md) and followed in [ADR 0026](0026-oxygen-saturation-without-assessment.md), is to record and show what is entered and to assess nothing; [ADR 0005](0005-dutch-nhg-guidelines.md) shows how a neutral "name · range" label placed against a published range stays inside it. The Nictiz zib Vochtbalans is a clinical building block for intake and output in care; no zib for a person's own drinks was found (its pages could not be read in full).

## Decision

- **Quick log by drink.** One button per drink (Water, Coffee, Tea, Milk, Juice, Soup, Other) with a count stepper; one tap stores one entry of that drink with count times the drink's own portion, and a custom entry takes the drink, 10 to 5000 ml and an optional comment. People think in "a cup of coffee" or "a glass of water", so the drink is chosen by the button itself at no extra step. The first draft left the type out; it is in version 1 as a **label only**: a fixed list, every drink counts equally (Voedingscentrum: all drinks count), no caffeine, sugar, alcohol or nutrition attribute, no alcohol entry, and a per-drink chart of plain ml. A missing or unknown value (old CSV or backup) reads as Other.
- **Own portions in preferences.** One portion per drink, starting values Water 250, Coffee 125, Tea 125, Milk 200, Juice 150, Soup 200, Other 200 ml (125 ml is the cup Voedingscentrum uses), editable. They are settings, not health entries, so the preferences carry no entry data ([ADR 0023](0023-database-encryption-and-app-lock.md) and the `privacy` capability); they are not in the backup list.
- **Entries are the truth, totals are derived.** Table `fluid_intakes` (profile, timestamp, whole ml, drink, optional comment) and `fluid_settings` (profile, optional minimum, optional maximum) are added by one migration, the next free number at implementation time (current Room version + 1). Totals and positions are computed in `:domain` per local calendar day with an injected zone, so an edit or delete can never leave a stale figure.
- **Personal minimum and maximum, independent, set only by the person** (100 to 10000 ml each, minimum not above maximum). A person may set one, both or neither. The fields are empty until set; the app never suggests, prefills, defaults or hints at a number and has no "use the Voedingscentrum value" button. Stored per profile in the encrypted `fluid_settings` table (preferences are unencrypted); in the backup as `fluidMinMl` and `fluidMaxMl`, not in the CSV. Changing or clearing them never alters entries.
- **A neutral position label, not a verdict.** With a number set, the Today line and each History day-total row carry "Below your minimum · 1400 ml", "Within your limits · 1400 to 2000 ml" or "Above your maximum · 2000 ml", always with the number, in the neutral sequential colour ramp of `range-labels` and never green, yellow, orange or red. No alert, notification, reminder, ring, percentage or advice exists. This is the one listed exception to the "show plain numbers only" stance of the first draft, and it is the point to confirm in the spec review.
- **Consumer-grade look, no coaching.** Large drink tiles with own-drawn icons, a gauge that fills (a glass, or a plain woman or man outline, chosen by the person; only the look differs, the scale and rules are the same), a strip of today's drinks, a polished calendar and charts, as in well-known hydration trackers, but with none of their streaks, badges, mascots, points or motivational text. The glass is scaled to the person's own maximum or minimum and shows no percentage; with no number set it fills in steps of one glass and has no scale. It is the nearest the design comes to the progress ring that the `compliance` capability forbids and is the second point to confirm in the spec review.
- **Reference note, not a target.** An "About these amounts" note states what Voedingscentrum, the huisarts-set maximum and Thuisarts say, as information with the source linked. General amounts appear nowhere else.
- **Graphs and calendar like Migraine.** Under the Fluid filter the graph view has a Graph and Calendar toggle and the shared range chips: figures, daily bars with up to two dashed lines, a weekday chart, a time-of-day chart and a drinks chart, and a month calendar with each day's total, shaded in one hue, with a shape marker for the position and a tap that opens the day in History. They reuse the Migraine chart and calendar parts.
- **Fits the existing architecture.** The code is in the `metrics` context with its own port, `Record` and `Update` commands and handlers ([ADR 0036](0036-application-boundary-commands-and-queries.md), [ADR 0038](0038-bounded-contexts-and-tactical-roles.md), [ADR 0039](0039-use-case-or-handler.md)), registered in `CommandModule`; Fluid is a `MetricSlot` with its own Log tab ([ADR 0034](0034-metric-display-order-preference.md)); History uses the adaptive two-pane layout unchanged ([ADR 0040](0040-adaptive-layouts-without-window-size-library.md)).
- **Backup keeps `schemaVersion` 1** with a `fluidEntries` list ([ADR 0024](0024-portable-encrypted-backup.md)); CSV `timestamp,ml,drink,comment` through `CsvQuoting`.
- **Comment** reuses `EntryComment` ([ADR 0019](0019-entry-comments.md)).

## Consequences

### Positive
- Fastest possible entry; works for a curious drinker, for a person on a restriction (maximum only) and for a person who must keep up their intake (minimum only).
- Per-day overview in the calendar and the charts, in the same style as the migraine view.
- No amount to defend or keep up to date; no reminders or notifications to build.
- Old databases and backups keep working.

### Negative / Trade-offs
- A position label can still feel like a verdict or a goal; mitigated by neutral colours, the number always beside it, the reference note and no alerts.
- The label for the current day is "below the minimum" until the person has drunk enough that day; it says nothing about the day being finished.
- People who like goals and rings will not find them.
- Many small entries make History long; day-total rows, the graphs and the calendar mitigate this.
- Another CSV file type and backup list to keep in step, and a first settings table.

## Related
- [ADR 0005](0005-dutch-nhg-guidelines.md), [ADR 0015](0015-localized-viewmodel-messages.md), [ADR 0018](0018-smart-input-pickers.md), [ADR 0019](0019-entry-comments.md), [ADR 0021](0021-medication-compliance-and-privacy.md), [ADR 0024](0024-portable-encrypted-backup.md), [ADR 0026](0026-oxygen-saturation-without-assessment.md)
- [OpenSpec change `add-fluid-intake`](../../openspec/changes/archive/2026-11-16-add-fluid-intake/proposal.md)
