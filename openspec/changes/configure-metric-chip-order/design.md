## Context

The Log screen (`LogMetricScreen.kt`) uses a `PrimaryScrollableTabRow` whose tab sequence is currently hardcoded in the order the `Tab` composables are declared, with `selectedTabIndex` derived from `MetricType.ordinal`. The History screen (`HistoryScreen.kt`) renders filter chips in a plain hardcoded `Row`. Neither surface is configurable by the user.

The app already stores lightweight user preferences via `SharedPreferences` (see `UnitPreference.kt`, `ThemePreference.kt`, etc.) using the same pattern: a class in `app/settings/` wrapping a named `SharedPreferences` file. No ADR conflicts with adding another preference there. The project avoids extra dependencies (ADR-0003), so no third-party drag-to-reorder library will be introduced.

## Goals / Non-Goals

**Goals:**
- Persist a user-defined metric order in SharedPreferences.
- Drive Log tab and History chip sequences from that stored order.
- Provide a drag-to-reorder UI in the Settings screen.
- Default order when no preference is saved: **Weight → Blood Pressure → Glucose → Migraine → Waist Circumference → Activity**.

**Non-Goals:**
- Separate per-screen ordering (Log vs History share one preference).
- Runtime drag reorder directly on the tab bar or chip row.
- Database or Room migration changes.
- Adding external drag-to-reorder libraries.

## Decisions

### D1 — `MetricDisplayOrderPreference` in `:app/settings/`

**Choice**: Add `MetricDisplayOrderPreference.kt` following the existing `UnitPreference` pattern. It wraps a `SharedPreferences` file named `"metric_order_prefs"` and stores the order as a comma-separated string of `MetricType` names (e.g. `"WEIGHT,BLOOD_PRESSURE,GLUCOSE,MIGRAINE,WAIST_CIRCUMFERENCE,ACTIVITY"`).

Reading: split on `","`, map each token to `MetricType` by name, fall back to the default list for any unknown token. If the stored list omits any valid `MetricType` (e.g. after a future metric is added), append missing types at the end in their default relative order.

Default constant:
```kotlin
val DEFAULT_ORDER = listOf(
    MetricType.WEIGHT,
    MetricType.BLOOD_PRESSURE,
    MetricType.GLUCOSE,
    MetricType.MIGRAINE,   // Migraine lives in the shared ordered list
    MetricType.WAIST_CIRCUMFERENCE,
    MetricType.ACTIVITY
)
```

> **Note on Migraine in the ordered list**: Migraine is included in the shared preference list alongside the other metrics, even though its Log tab is handled slightly differently (it triggers a separate composable instead of the metric form). This keeps a single unified ordered list. Both ViewModels treat Migraine as a regular entry in the ordered sequence.

**Alternative considered**: Store the order as a JSON array. Rejected to avoid adding a JSON parsing dependency; comma-separated names are sufficient and readable.

### D2 — ViewModels expose `orderedMetrics: List<MetricType>`

**Choice**: Both `LoggingViewModel` and `HistoryViewModel` read `MetricDisplayOrderPreference` at startup (or on a settings-change event) and expose the ordered list via their `UiState`. `LogMetricScreen` computes `selectedTabIndex` by finding `state.selectedMetric` in `state.orderedMetrics` (index-of), eliminating the `MetricType.ordinal` dependency.

**Alternative considered**: Keep the tab-index calculation ordinal-based and only reorder enum entries. Rejected because ordinal-based ordering is fragile and cannot support a runtime-configurable sequence.

### D3 — Settings screen drag-to-reorder using `ReorderableColumn` (no library)

**Choice**: Implement drag-to-reorder in the Settings UI using Jetpack Compose's built-in `LazyColumn` with `Modifier.pointerInput` for drag detection and manual index-swap logic. The item being dragged is given an elevated `zIndex`; the list shifts items around it. On drag release the new `List<MetricType>` is written to `MetricDisplayOrderPreference`.

**Alternative considered**: Use the `reorderable` Compose library (a popular community library). Rejected per ADR-0003 (dependency minimisation); the interaction is straightforward enough to implement without it.

### D4 — Log and History screens iterate `state.orderedMetrics`

**Choice**: Replace the hardcoded `Tab` / `FilterChip` declarations with a loop over `state.orderedMetrics`. Each `MetricType` in the list maps to one tab/chip using a `when` expression for the label string resource. The Migraine tab/chip is included in the loop since Migraine is part of the unified ordered list.

**Rationale**: Keeps rendering declarative and driven by data rather than hardcoded layout.

## Risks / Trade-offs

- **`MetricType.ordinal` removal** → Any code currently relying on `MetricType.ordinal` for the selected-tab index (see `LogMetricScreen`) must be updated to use `indexOf`. Search all usages before applying. Impact is confined to `:app`. → Mitigation: D2 covers this; tests in task 4.1 verify.
- **Future new metric types** → If a new `MetricType` is added later, `MetricDisplayOrderPreference.read()` appends unknown types at the end, so old saved preferences remain valid. No migration needed.
- **Manual drag implementation complexity** → Without a library, the drag-to-reorder code is more verbose. → Mitigation: scope is a simple 6-item list, not a large dynamic collection; the implementation is proportionate.

## Migration Plan

1. Add `MetricDisplayOrderPreference.kt` in `app/settings/`.
2. Wire the preference into `LoggingViewModel` and `HistoryViewModel` (`loadProfile` or `init`).
3. Update `LogMetricScreen` to loop over `orderedMetrics` for tabs.
4. Update `HistoryScreen` to loop over `orderedMetrics` for chips.
5. Add the reorderable list section to the Settings screen.
6. Add EN and NL string resources.
7. Run existing `ViewModelsTest`; add new tests.

Rollback: revert the changed files; the preference key is never read unless the new code is present, so existing installs are unaffected.

