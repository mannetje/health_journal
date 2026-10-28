## Why

The metric tabs in the Log screen and the filter chips in the History screen currently follow a fixed, hardcoded order that the user cannot change. Users have different habits and may want their most-used metrics to appear first. Allowing the user to configure the display order of metrics reduces friction and makes the app feel personalised.

## What Changes

- Add a **user-configurable metric order** stored in SharedPreferences (`metric_display_order` key).
- **Settings screen**: new drag-to-reorder list for metrics; user drags items to set their preferred order. The saved order persists across app restarts.
- **Log screen tabs** (`PrimaryScrollableTabRow` in `LogMetricScreen.kt`): tab sequence is driven by the persisted preference, not hardcoded. Migraine is always appended as the last tab and is not part of the draggable list.
- **History screen filter chips** (`HistoryScreen.kt`): chip sequence (after "All") is driven by the same persisted preference.
- Both screens share a single stored order; there is no separate per-screen configuration.
- **Default order** (when no preference is saved): Weight → Blood Pressure → Glucose → Migraine → Waist → Activity.
- No database changes, no Room migration needed.

## Capabilities

### New Capabilities

- `metric-display-order`: Defines user-configurable ordering of metrics across the Log and History screens, including persistence, default order, and Settings UI.

### Modified Capabilities

- `health-metrics`: Log tab order and History chip order are no longer hardcoded; both are derived from the user preference.

## Impact

- `app/src/main/java/nl/healthjournal/app/settings/` — new `MetricDisplayOrderPreference.kt` (SharedPreferences wrapper).
- `app/src/main/java/nl/healthjournal/app/ui/settings/` — Settings screen gains a reorderable metric list section.
- `app/src/main/java/nl/healthjournal/app/ui/logging/LoggingViewModel.kt` — exposes ordered metric list from preference; `MetricType` enum declaration order is no longer load-bearing for tab index.
- `app/src/main/java/nl/healthjournal/app/ui/logging/LogMetricScreen.kt` — tabs rendered by iterating the ordered list.
- `app/src/main/java/nl/healthjournal/app/ui/history/HistoryViewModel.kt` — exposes ordered metric list from preference.
- `app/src/main/java/nl/healthjournal/app/ui/history/HistoryScreen.kt` — chips rendered by iterating the ordered list.
- String resources (EN + NL) for the new Settings section.
- No API, no database, no domain model changes.
