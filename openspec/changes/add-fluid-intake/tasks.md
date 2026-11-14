## 0. Decisions
- [x] 0.1 Metric: fluid intake, volume only; quick-add taps plus custom ml entry; each tap is one entry
- [x] 0.2 Range 10 to 5000 ml, whole ml; comment 200 chars; one portion per drink stored in preferences, editable (Water 250, Coffee 125, Tea 125, Milk 200, Juice 150, Soup 200, Other 200)
- [x] 0.3 The app sets no target, reminder, ring or advice; no recommended amount is filled in, defaulted or hinted in any field
- [x] 0.4 Daily total of the local calendar day on the Log card; bars of day totals in the graph; average over logged days
- [x] 0.5 Type of drink IN version 1 as a label only: fixed list (Water, Coffee, Tea, Milk, Juice, Soup, Other), one button per drink with its own portion and a count stepper, every drink counts equally, per-drink chart of plain ml, missing or unknown reads as Other
- [x] 0.6 Two use cases: (1) log daily intake, (2) optional personal minimum and maximum, independent, 100 to 10000 ml each, minimum not above maximum, set only by the person, no default or hint, per profile in `fluid_settings(min_ml, max_ml)`, in the backup as `fluidMinMl` and `fluidMaxMl`, not in the CSV
- [x] 0.7 Neutral position label ("Below your minimum", "Within your limits", "Above your maximum", always with the number) in the style of the range labels; neutral colours, no alert, reminder or advice
- [x] 0.8 Reference note "About these amounts" with Voedingscentrum `vocht.aspx`, Thuisarts and the huisarts line; information with the source named, never prefilled
- [ ] 0.9 Spec review by the user before implementation starts, including the compliance question on the position label (design section 5)
- [ ] 0.9a Read the Antonius leaflet `vochtbeperking-een.pdf` (not readable by the fetch tool) and check it against the reference note
- [x] 0.10 Read the zib Vochtbalans in full (pages answered 503 during drafting) and complete `docs/dev/zib-mapping.md`
- [ ] 0.11 Final English and Dutch wording agreed (card, button names, Today line, limits, position labels, reference note, Undo, notice)
- [x] 0.12 The migration is the next free number at implementation time (current database version + 1); build order is the user's choice

## 1. Domain (`:domain`) and data (`:data`)
- [x] 1.1 `FluidIntakeEntry`, `FluidMl`, `FluidDrink`, `DrinkPortions`, validation; JVM tests (drink parse, unknown reads as Other, portion bounds, count times portion above 5000)
- [x] 1.2 `FluidDayTotals`, `FluidStatistics`, `FluidWeekdayAverages`, `FluidTimeOfDay`, `FluidDrinkTotals` and `FluidCalendarMonth` with injected zone and clock; JVM tests for empty range, one entry, midnight boundary, time zone, rounding, unlogged days, first day of week, three-hour block edges, month edges and positions per day
- [x] 1.3 `FluidLimits` (each 100 to 10000 or null, minimum not above maximum) and `fluidPosition`; JVM tests for no limits, only a minimum, only a maximum, both, a total equal to each limit, total 0, and the invalid combinations
- [x] 1.4 Own port (entries and limits), record and update commands with handlers, `SetFluidLimitsCommand` (set, change, clear either value) and a limits query with handlers, delete branch in `PortEntryGateway`, handlers registered in `CommandModule`; JVM tests that changing or clearing a limit leaves entries unchanged
- [x] 1.5 Entity, DAO, mapper, `fluid_settings(profile_id, min_ml, max_ml)`, one migration from the current version N to N+1, repository; migration test from version N
- [x] 1.6 CSV export and import (`timestamp,ml,drink,comment`; file without the drink column reads as other) with row rejection; round-trip tests
- [x] 1.7 Backup snapshot and restore of `fluidEntries`, `fluidMinMl` and `fluidMaxMl`; older-file and null-value tests
- [x] 1.8 Preferences for the drink portions with starting values and validation; test that no entry data and no limit is stored in preferences

## 2. App (`:app`)
- [x] 2.1 Fluid card: one button per drink with the count stepper, custom entry form with a drink choice, Today line (with and without limits, with the position label), portion settings, "Your limits" fields with clear for each, Undo
- [x] 2.1a Visual design: `DrinkTile` grid with own-drawn icons for the seven drinks (light and dark), `FluidGlass` gauge (scale from the person's own number, steps of one glass without one, ripple, haptic tick, reduced motion), today strip of drink icons; no streaks, badges or motivational text; previews and the wide layout
- [ ] 2.2 Reference note "About these amounts" (sheet, sources and links, never fills a field)
- [x] 2.3 History rows, day-total rows with the position label under the Fluid filter, filter chip, edit dialog, delete
- [x] 2.4 Graph view like the Migraine view: Graph and Calendar toggle and range chips; figures, daily bar chart with up to two dashed lines, weekday chart, time-of-day chart, drinks chart; chart descriptions
- [x] 2.4a Fluid calendar card: month grid with the total in each logged day, one-hue shading, position markers with legend when limits are set, previous and next month, tap a day to open it in History; reuse the Migraine calendar parts
- [x] 2.4b `MetricSlot.FLUID`, `MetricType.FLUID`, Fluid Log tab, History filter chip and day filter; `DomainErrorUiText` strings; `HandlerRegistryTest` and `ArchitectureGuardrailsTest` stay green; check the two-pane History on a tablet
- [ ] 2.5 Position label colours from the neutral ramp in light and dark, contrast at least 4.5:1, label text always present
- [ ] 2.6 Strings in English and Dutch; scan test that bans advice and verdict words while allowing the position label words and the person's own numbers; Profile notice sentence
- [x] 2.7 Export and import entries in Profile with the unencrypted warning
- [ ] 2.8 Accessibility: 48 dp targets, content descriptions with the amount, Undo announced, label read with its number
- [ ] 2.9 Emulator pass (phone, tablet, landscape, animation and reduced motion included): English and Dutch, light and dark, 320 dp, phone and tablet, with no limits, only a minimum, only a maximum and both; screenshots

## 3. Documentation and release
- [x] 3.1 `docs/backup-format/` (README, schema, `hjbackup.py`, vectors, recovery page)
- [x] 3.2 `docs/dev/database.md` (ER diagram, current version, version history and the naming conventions for `fluid_intakes` and `fluid_settings`), range labels page, other `docs/dev/` pages, `docs/dev/how-to/add-a-metric.md` if needed, `docs/dev/zib-mapping.md`, README, CHANGELOG, screenshots
- [ ] 3.3 Archive the change into `openspec/specs/`; release
