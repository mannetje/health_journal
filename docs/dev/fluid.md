# Fluid intake

The Fluid log records what a person drinks, one tap at a time, and shows the day total. The person may set their own minimum and maximum; the app never picks a number. This page is the map of the feature. The reasoning is in [ADR 0030](../adr/0030-fluid-intake-quick-log-with-personal-limits.md), the requirements in the [change proposal](../../openspec/changes/archive/2026-11-16-add-fluid-intake/proposal.md). Back to the [developer docs](README.md).

## What it does

| Where | What the person sees |
|---|---|
| Log, Fluid tab | A tile per drink (Water, Coffee, Tea, Milk, Juice, Soup, Other) with its own portion and a count stepper; one tap stores one entry of count times portion. A gauge (glass, woman or man outline, chosen by the person) fills with today's total. A strip shows today's drinks, with Undo. A custom entry takes the drink, 10 to 5000 ml and a comment. "Your limits" holds the optional minimum and maximum. |
| History, Fluid filter | Entry rows and a row per day with the total and, when limits are set, the position label (below your minimum, within your limits, above your maximum), always next to the number. Edit and delete as for other entries. |
| History, graph view | Figures, a daily bar chart with up to two dashed limit lines, weekday averages, a time-of-day chart and a per-drink chart of plain ml. |
| History, calendar | A month grid with the total printed in each logged day, one hue of shading and a shape marker (▾ below, ● within, ▴ above) when limits are set; a tap opens that day in History. |
| Profile | CSV export and import, and the backup. |

Screenshots (phone and tablet, light and dark, English and Dutch) are in [Screenshots](../screenshots.md).

## Where the code is

| Layer | Files |
|---|---|
| `domain` model | `metrics/model/`: `FluidIntakeEntry`, `FluidMl`, `FluidDrink` and `DrinkPortions`, `FluidLimits` with `fluidPosition`, `FluidDayTotals`, `FluidStatistics` and the weekday, time-of-day and per-drink figures (`FluidCharts.kt`), `FluidCalendarMonth` |
| `domain` application | `metrics/command/FluidCommands.kt`: `RecordFluidIntakeCommand`, `UpdateFluidIntakeCommand`, `SetFluidLimitsCommand`, `ObserveFluidHistoryQuery`, `ObserveFluidLimitsQuery` and their handlers; delete goes through the shared entry gateway. All are registered in `CommandModule` ([ADR 0036](../adr/0036-application-boundary-commands-and-queries.md), [ADR 0039](../adr/0039-use-case-or-handler.md)) |
| `domain` port | `metrics/port/FluidRepositoryPort.kt`; the CSV and backup ports carry fluid entries and limits |
| `data` | `FluidIntakeEntity` and `FluidSettingsEntity`, `FluidIntakeDao`, mapping in `HealthLogMapper`, `RoomHealthLogRepository`, `MIGRATION_8_9`, CSV in `CsvDataExportAdapter` and `CsvDataImportAdapter`, backup in `BackupJson` and `RoomBackupSnapshot` |
| `app` | `ui/fluid/`: `FluidLogScreen`, `FluidLogViewModel`, `FluidGauge`; `ui/history/charts/FluidGraphSection.kt` (graph and calendar); `settings/FluidGaugePreference` (the gauge look) |

The [C4 page](c4.md) has the slice across the three modules, and the [database page](database.md) the two tables.

## Data and rules

- **Entries only.** `fluid_intakes` holds one row per entry. A day total, the position label and every chart figure is derived with an injected time zone, so a change of limit or zone rewrites nothing and day boundaries follow the local calendar day.
- **Limits.** `fluid_settings` has one row per profile with `minMl` and `maxMl`, each null or 100 to 10000, minimum not above maximum. They are set only by the person; nothing fills in, defaults or hints a number. A total equal to a limit is within it.
- **Drinks.** A fixed list of stable lower-case codes. Every drink counts equally (ml is ml); there is no caffeine, sugar or alcohol attribute. A missing or unknown code reads as Other.
- **Portions and gauge look.** The portion per drink (10 to 500 ml, so ten portions fit one 5000 ml entry) and the gauge style are display choices kept in preferences (`DrinkPortionsPreference`, `FluidGaugePreference`), never entries or limits. The person edits the portions in the "Your portions" card; a missing, unknown or out-of-range stored value reads as the starting portion.
- **Gauge scale.** The fill is scaled to the person's own maximum, else their own minimum, with a tick for each number set. With neither it fills in whole glasses and shows no scale. No percentage is shown. Animation is skipped when the system asks to reduce motion.

## Compliance

The feature follows [ADR 0021](../adr/0021-medication-compliance-and-privacy.md) and the `compliance` capability: no assessment, advice, target, reminder, streak, badge, mascot or motivational text. The position label is a plain statement about the person's own numbers in one neutral colour, never red or green. The calendar and charts never rely on colour alone (printed number, shape marker, dashed line with text). The wording scan test over the strings bans advice and verdict words.

## Screen behaviour

The Fluid tab asks for a recount just after each local midnight (`FluidLogViewModel.refreshToday`), so the gauge and total roll over to the new day without a new entry. History observes the limits as a flow, so numbers set on the Fluid tab show there at once.

## Import, export and backup

| Format | Content |
|---|---|
| CSV | `timestamp,ml,drink,comment`; a row outside 10 to 5000 ml is rejected, an unknown drink reads as `other`, a file without the drink column imports as `other`, and a row that matches a stored entry (same time, amount and drink) is skipped and listed, so importing a file twice adds nothing. Limits are not in the CSV. |
| Backup | per profile `fluidEntries` (`id`, `timestamp`, `ml`, `drink`, `comment`) plus `fluidMinMl` and `fluidMaxMl`, written only when present; `schemaVersion` stays 1 and an older reader ignores them. A merge adds entries by `id` and applies limits only when the profile has none. An entry outside 10 to 5000 ml is left out when reading, so one bad value never blocks a restore. See [backup format](../backup-format/README.md). |

## Tests

`FluidModelTest` (domain: ranges, drink parsing, day totals, position, statistics, calendar), `FluidLogViewModelTest` (taps, counts, position, limits, midnight recount, undo, History limits), `FluidGaugePreferenceTest`, `DrinkPortionsPreferenceTest`, `FluidWordingTest` (no coaching, goal or verdict words, position texts carry the number, same strings and placeholders in English and Dutch, no streak or badge in the code), the fluid cases in `CsvAdaptersTest` (round trip, duplicates, bad rows) and `BackupFormatTest` (round trip, unknown drink, out-of-range amount, invalid limits), and the mapper, migration 8 to 9, `HandlerRegistryTest` and `MetricSlotTest`. See [Testing](testing.md).

## Known gaps

The Profile notice and the final English and Dutch wording review are open items in the change's `tasks.md`. The lower-case drink codes in CSV and backup shipped in 1.7.3 and are now part of the format.
