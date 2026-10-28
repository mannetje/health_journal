## Context

The Log screen (`LogMetricScreen.kt`) has a `PrimaryScrollableTabRow` whose tabs are declared one by one. Five of them are `MetricType` values; Migraine is a separate flow: `migraineTabSelected` is hoisted in `MainActivity` (`logMigraineTab`), the form has its own `MigraineLogViewModel`, and the tab index is `if (migraineTabSelected) MetricType.entries.size else selectedMetric.ordinal`. The History screen has its own hardcoded chip row driven by `HistoryFilter` (which does contain `MIGRAINE`). The History import dialog has a third chip row for file formats.

Settings do not have a screen of their own: they sit on the Profile tab (`ui/profile/`: `ReminderSettings`, `SecuritySettingsSection`), and that screen scrolls vertically.

Preferences are small classes in `app/settings/` wrapping a named `SharedPreferences` file (`ThemePreference`, `UnitPreference`). Shared instances live in `HealthJournalApp` (`reminderPreference`). ADR-0003 keeps dependencies to a minimum, so no drag-and-drop library is added. The Log pickers are accessible to TalkBack, so a control that works only with a drag gesture would be a step back.

## Goals / Non-Goals

**Goals:**
- Persist one user-defined metric order in SharedPreferences.
- Drive the Log tabs and the History chips from it, updating both as soon as the order changes.
- Provide a reorder list on the Profile tab that works with a finger drag and without one (TalkBack, switch access).
- New default order: **Weight, Blood Pressure, Glucose, Migraine, Waist, Activity**.

**Non-Goals:**
- A separate order per screen.
- Reordering directly on the tab bar or chip row.
- Database or Room changes.
- An external drag-and-drop library.
- Putting the order in the encrypted backup (D7).
- Reordering the file-format chips of the History import dialog.

## Decisions

### D1 — `MetricSlot` is the orderable entry; it maps to the existing types

`MetricType` has no Migraine entry and adding one would touch every `when (state.selectedMetric)` and the logging use cases. Instead a small enum in `app/settings/MetricSlot.kt` names the six orderable entries:

```kotlin
enum class MetricSlot { WEIGHT, BLOOD_PRESSURE, GLUCOSE, MIGRAINE, WAIST_CIRCUMFERENCE, ACTIVITY }
```

Each slot maps to the existing code: `metricType: MetricType?` (null for `MIGRAINE`) for the Log screen, and `historyFilter: HistoryFilter` for the History screen. The label string resource per screen is chosen with a `when` over the slot next to the composable that shows it. `MetricType`, `HistoryFilter` and the migraine flow stay as they are.

The default is `MetricSlot.DEFAULT_ORDER` in the same file (the order above).

### D2 — `MetricDisplayOrderPreference`: storage, parsing and a shared `StateFlow`

It wraps the file `metric_order_prefs`, key `metric_display_order`, and stores the order as comma-separated `MetricSlot` names. There is no JSON, so no parsing dependency.

`read()` rules (one place, unit-tested):
1. Split on `,`, trim, map tokens to `MetricSlot` by name.
2. **Drop** unknown tokens and duplicates (first occurrence wins). One bad token never resets the rest of the list.
3. **Append** every slot that is missing, in its position from the default order.
4. Nothing stored, or an empty string, gives the default order.

The class exposes `val order: StateFlow<List<MetricSlot>>` and `fun save(order: List<MetricSlot>)`. `save` normalises the list with the same rules, writes it, and emits it. `HealthJournalApp` holds one lazy instance (`metricDisplayOrderPreference`, next to `reminderPreference`), so the Profile tab, the Log screen and the History screen all see the same flow.

### D3 — ViewModels combine the flow into their UI state

`LoggingViewModel` and `HistoryViewModel` get the preference through their factories and collect `order` into `orderedMetrics: List<MetricSlot>` in their `UiState`. A change made on the Profile tab therefore reaches both screens without reloading anything. `ProfileViewModel` calls `save()`.

### D4 — Log tabs, tab index and the starting selection

`LogMetricScreen` loops over `state.orderedMetrics` for the tabs. The selected index is `orderedMetrics.indexOf(selectedSlot)`, where the selected slot is `MIGRAINE` when `migraineTabSelected` is true and otherwise the slot of `state.selectedMetric`. The `ordinal` and `entries.size` arithmetic goes away.

Starting selection: on a fresh launch the first slot of the order is selected. `LoggingUiState.selectedMetric` starts from the first slot that has a `MetricType`, and `MainActivity` starts `logMigraineTab` as `true` when the first slot is `MIGRAINE`. After that the selection is the user's: changing the order does not move the selected tab (its index just changes).

### D5 — History chips

After the "All" chip, `HistoryScreen` loops over `state.orderedMetrics` and shows the chip for each slot's `historyFilter`. `selectedFilter` still starts as `ALL`.

### D6 — Reorder UI on the Profile tab

`MetricOrderSection.kt` shows the six metrics in a plain `Column` (not a `LazyColumn`: the Profile screen already scrolls vertically, and a lazy list inside it fails with unbounded height; six rows do not need laziness). Each row has:
- the metric name,
- Move up and Move down icon buttons (48 dp touch targets, disabled at the ends, content descriptions such as "Move Glucose up"), and
- a drag handle that reorders with `detectDragGesturesAfterLongPress` and manual index swapping, with an elevated `zIndex` and a tint on the dragged row.

The buttons are the accessible path. They give TalkBack users the same result without custom gesture handling, and after a move the new position is announced through a polite live region ("Glucose, position 2 of 6"). The order is saved on every completed move (button press or drag release), not when the screen is left.

The texts name positions only. They do not say which order is better or recommended.

### D7 — The order is not in the encrypted backup

The backup document has a fixed set of display preferences (`BackupPreferences`: language, region, units, theme, reminder snooze) and a documented format (`docs/backup-format`). Adding the order means a format change and a restore path for older files. That is a separate change; until then a restored phone starts on the default order. This is recorded here and in the backup docs as a known limit.

### D8 — Metrics added later

New metrics (oxygen saturation, mood, sleep, temperature, fluid, energy) each add a `MetricSlot`. `read()` appends slots missing from an older saved order, so nothing breaks and nothing needs a migration. The requirements say "every metric", not "six", and `docs/dev/how-to/add-a-metric.md` gets a step for adding the slot, its `when` mappings and its labels.

## Risks / Trade-offs

- **The new default order moves Migraine up for existing users.** Accepted on purpose. → Mitigation: a "Changed" line in the changelog, and the Profile section lets anyone put it back.
- **`MetricType.ordinal` removal** → every use for the tab index must be replaced. The only use is in `LogMetricScreen` (checked with a search); task 3.1 repeats the search.
- **Manual drag code is verbose.** → The buttons carry the accessibility; the drag is an extra for sighted users, and the list has a handful of rows.
- **Two sources for the selected Log tab** (`logMigraineTab` and `selectedMetric`) already exist. → This change does not merge them; D4 only reads both through one `selectedSlot` helper.

## Migration Plan

1. `MetricSlot` and `MetricDisplayOrderPreference`, plus the shared instance in `HealthJournalApp`.
2. Pass the preference to `LoggingViewModel`, `HistoryViewModel` and `ProfileViewModel` through their factories.
3. Log screen: ordered tabs, index from the list, initial selection.
4. History screen: ordered chips.
5. Profile tab: `MetricOrderSection`.
6. Strings (EN and NL), tests, docs, screenshots.

Rollback: revert the changed files. The preference key is only read by the new code, so existing installs are unaffected.
