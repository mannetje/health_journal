## 1. Preference storage

- [ ] 1.1 Create `app/src/main/java/nl/healthjournal/app/settings/MetricDisplayOrderPreference.kt` with a `DEFAULT_ORDER` constant (`WEIGHT, BLOOD_PRESSURE, GLUCOSE, MIGRAINE, WAIST_CIRCUMFERENCE, ACTIVITY`) and read/write methods that serialize to/from a comma-separated `MetricType` name string in SharedPreferences (`"metric_order_prefs"`)
- [ ] 1.2 Ensure `read()` falls back to `DEFAULT_ORDER` for missing or unknown tokens, and appends any `MetricType` values absent from the stored list at the end in their default relative position

## 2. ViewModel wiring

- [ ] 2.1 Add `orderedMetrics: List<MetricType>` to `LoggingUiState` in `LoggingViewModel.kt`; load it from `MetricDisplayOrderPreference` during `loadProfile()`
- [ ] 2.2 Remove the `MetricType.ordinal`-based tab-index calculation from `LogMetricScreen`; replace with `state.orderedMetrics.indexOf(state.selectedMetric)` (handle `migraineTabSelected` offset)
- [ ] 2.3 Add `orderedMetrics: List<MetricType>` to `HistoryUiState` in `HistoryViewModel.kt`; load it from `MetricDisplayOrderPreference` during `loadHistory()`
- [ ] 2.4 Expose a `saveMetricOrder(order: List<MetricType>)` method (or equivalent) from both ViewModels (or a dedicated SettingsViewModel) so the Settings screen can persist changes

## 3. Log screen — dynamic tabs

- [ ] 3.1 In `LogMetricScreen.kt`, replace the hardcoded `Tab` composables in `PrimaryScrollableTabRow` with a loop over `state.orderedMetrics`, mapping each `MetricType` to its label string resource

## 4. History screen — dynamic chips

- [ ] 4.1 In `HistoryScreen.kt`, replace the hardcoded `FilterChip` composables (after the "All" chip) with a loop over `state.orderedMetrics`, mapping each `MetricType` to its label string resource

## 5. Settings screen — drag-to-reorder UI

- [ ] 5.1 Add a "Metric order" section to the Settings screen with a `LazyColumn` of 6 draggable metric items using Compose `Modifier.pointerInput` (no external library)
- [ ] 5.2 Implement visual drag feedback (elevated `zIndex`, subtle shadow or background tint on the dragged item)
- [ ] 5.3 On drag release, write the new `List<MetricType>` order to `MetricDisplayOrderPreference` via the ViewModel/preference class; immediately update `orderedMetrics` in UI state

## 6. String resources

- [ ] 6.1 Add EN strings (`values/strings.xml`) for the Settings section title and drag handle content description (e.g. `settings_metric_order_title`, `settings_metric_order_drag_handle`)
- [ ] 6.2 Add matching NL strings (`values-nl/strings.xml`)

## 7. Tests

- [ ] 7.1 Unit-test `MetricDisplayOrderPreference`: verify round-trip serialization, default fallback, and unknown-token tolerance
- [ ] 7.2 Update `ViewModelsTest` to verify that `LoggingViewModel` and `HistoryViewModel` expose `orderedMetrics` equal to the default order when no preference is saved
- [ ] 7.3 Verify (unit or UI test) that saving a custom order via the preference class is reflected in the ViewModel's `orderedMetrics` after reload

## 8. Docs

- [ ] 8.1 Add an entry under `[Unreleased]` in `CHANGELOG.md` describing the user-configurable metric order feature
- [ ] 8.2 Run `scripts/check-docs.sh` (if it exists) and fix any doc-sync failures

