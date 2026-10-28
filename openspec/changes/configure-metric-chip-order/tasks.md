## 1. Preference storage

- [ ] 1.1 Create `app/src/main/java/nl/healthjournal/app/settings/MetricSlot.kt` with the six slots, `DEFAULT_ORDER` (`WEIGHT, BLOOD_PRESSURE, GLUCOSE, MIGRAINE, WAIST_CIRCUMFERENCE, ACTIVITY`) and the mappings `metricType: MetricType?` and `historyFilter: HistoryFilter`
- [ ] 1.2 Create `MetricDisplayOrderPreference.kt` (`metric_order_prefs` file, `metric_display_order` key, comma-separated slot names) with `order: StateFlow<List<MetricSlot>>` and `save(order)`
- [ ] 1.3 Implement the read/normalise rules in one place: drop unknown tokens and duplicates (first wins), append missing slots in default relative order, empty or absent value gives the default
- [ ] 1.4 Add one shared lazy `metricDisplayOrderPreference` to `HealthJournalApp.kt`

## 2. ViewModel wiring

- [ ] 2.1 Pass the preference to `LoggingViewModel`, `HistoryViewModel` and `ProfileViewModel` through their factories
- [ ] 2.2 Add `orderedMetrics: List<MetricSlot>` to `LoggingUiState` and `HistoryUiState`, collected from the preference's flow so a change reaches both screens without a reload
- [ ] 2.3 `LoggingUiState.selectedMetric` starts from the first slot that has a `MetricType`; `MainActivity` starts `logMigraineTab` as `true` when the first slot is `MIGRAINE`
- [ ] 2.4 `ProfileViewModel` exposes the order and `moveMetric(from, to)` that calls `save()`

## 3. Log screen — ordered tabs

- [ ] 3.1 Search for every use of `MetricType.ordinal` and `MetricType.entries.size` as a tab index (expected: only `LogMetricScreen.kt:104`) and replace it with `orderedMetrics.indexOf(selectedSlot)`, where `selectedSlot` is `MIGRAINE` when `migraineTabSelected` is true
- [ ] 3.2 Replace the hardcoded `Tab` composables with a loop over `state.orderedMetrics`; each slot gets its label string and its click handler (Migraine keeps `onMigraineTabSelected(true)`)
- [ ] 3.3 Changing the order does not change which metric is selected

## 4. History screen — ordered chips

- [ ] 4.1 Replace the hardcoded `FilterChip` composables after "All" in `HistoryScreen.kt` with a loop over `state.orderedMetrics` using each slot's `historyFilter`
- [ ] 4.2 Leave the file-format chips of the import dialog (`HistoryScreen.kt`, `importMetricType`) as they are

## 5. Profile tab — reorder UI

- [ ] 5.1 Create `ui/profile/MetricOrderSection.kt`: a plain `Column` (no `LazyColumn`) with one row per metric: name, Move up and Move down buttons (48 dp, disabled at the ends), and a drag handle
- [ ] 5.2 Drag: `detectDragGesturesAfterLongPress` with manual index swapping, elevated `zIndex` and a tint on the dragged row
- [ ] 5.3 Accessibility: content descriptions that name the metric ("Move Glucose up"), and a polite live-region announcement of the new position after a move
- [ ] 5.4 Save on every completed move (button press or drag release) through `ProfileViewModel`
- [ ] 5.5 Show the section in `ProfileScreen.kt`; the section text names positions only and never recommends an order

## 6. String resources

- [ ] 6.1 EN strings in `values/strings.xml`: section title, short explanation, Move up/down descriptions, drag handle description, position announcement
- [ ] 6.2 Matching NL strings in `values-nl/strings.xml` (informal "je")

## 7. Tests

- [ ] 7.1 Unit-test `MetricDisplayOrderPreference`: round trip, default when empty or absent, unknown token dropped without resetting the rest, duplicates dropped, missing slots appended in default relative order
- [ ] 7.2 Unit-test `MetricSlot` mappings (every slot maps to a `HistoryFilter`; exactly `MIGRAINE` has no `MetricType`)
- [ ] 7.3 Update `ViewModelsTest`: `LoggingViewModel` and `HistoryViewModel` expose the default `orderedMetrics` when nothing is saved, and expose the new order after `save()` without a reload
- [ ] 7.4 Test the initial Log selection with a saved order that starts with Glucose, and with one that starts with Migraine
- [ ] 7.5 Compose test of `MetricOrderSection`: Move up and Move down change the order, are disabled at the ends, and the announcement names the new position
- [ ] 7.6 Guardrail check: no `LazyColumn` in the scrolling content of `ProfileScreen.kt`, and the new strings pass the banned-words guardrail

## 8. Docs and release notes

- [ ] 8.1 `CHANGELOG.md` under `[Unreleased]`: an "Added" line for the configurable order, and a "Changed" line that the default order now puts Migraine before Waist and Activity
- [ ] 8.2 `docs/dev/`: `design.md` (accessibility row for the reorder section), `getting-started.md` (new files), `how-to/add-a-metric.md` (add the `MetricSlot`, its mappings and labels), `testing.md` (new test rows), and `backup.md` / `docs/backup-format` (the order is not part of the backup)
- [ ] 8.3 README feature list: one line about choosing the metric order
- [ ] 8.4 Emulator pass on the Profile tab, Log tabs and History chips with a changed order; new light and dark screenshots in `docs/screenshots/` and the entries in `docs/screenshots.md`
- [ ] 8.5 Run `scripts/check-docs.sh` and fix any failure
