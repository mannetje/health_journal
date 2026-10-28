## Why

The metric tabs on the Log screen and the filter chips on the History screen follow a fixed order that the user cannot change. People log different things most often, and being able to put those first reduces friction. The new default order is also part of this change, so Migraine moves up for everyone who has not chosen an order yet.

## What Changes

- Add a **user-configurable metric order**, stored in SharedPreferences (`metric_display_order` key in the `metric_order_prefs` file).
- **Profile tab**: a new "Metric order" section lists every metric. The user changes the order by dragging, or with Move up and Move down buttons (so the list also works with TalkBack and without drag gestures). The order is saved as soon as a change is made and survives app restarts.
- **Log screen tabs** (`PrimaryScrollableTabRow` in `LogMetricScreen.kt`): the tab sequence follows the stored order. Migraine is one of the metrics in the list, like the others.
- **History screen filter chips** (`HistoryScreen.kt`): the chips after "All" follow the same stored order.
- Both screens share one stored order. There is no separate setting per screen.
- **New default order** (no preference saved): Weight, Blood Pressure, Glucose, Migraine, Waist, Activity. **This changes the order for existing users who have not picked one**: today Activity and Waist come before Migraine. The change is announced in the changelog.
- The Log screen opens on the first metric of the order, so the user's first choice is what is ready to fill in.
- Out of scope on purpose: the chips in the History import dialog keep their fixed order (they list file formats, not the user's metrics), and the order is not part of the encrypted backup (see design, D7).
- No database changes, no Room migration.

## Capabilities

### New Capabilities

- `metric-display-order`: user-configurable ordering of metrics across the Log and History screens: persistence, default order, the Profile settings section, and the accessible way to reorder.

### Modified Capabilities

- `health-metrics`: Log tab order and History chip order are derived from the stored order instead of being fixed in code.

## Impact

- `app/src/main/java/nl/healthjournal/app/settings/` — new `MetricSlot.kt` (the orderable entries) and `MetricDisplayOrderPreference.kt` (SharedPreferences wrapper that also exposes the order as a `StateFlow`).
- `app/src/main/java/nl/healthjournal/app/HealthJournalApp.kt` — one shared `metricDisplayOrderPreference` instance, like `reminderPreference`.
- `app/src/main/java/nl/healthjournal/app/ui/logging/LoggingViewModel.kt` and `LogMetricScreen.kt` — ordered tabs, tab index from the list, initial selection from the first entry. `MetricType.ordinal` is no longer used for the tab index.
- `app/src/main/java/nl/healthjournal/app/MainActivity.kt` — the hoisted `logMigraineTab` starts from the first entry of the order.
- `app/src/main/java/nl/healthjournal/app/ui/history/HistoryViewModel.kt` and `HistoryScreen.kt` — ordered chips.
- `app/src/main/java/nl/healthjournal/app/ui/profile/` — new `MetricOrderSection.kt`, used by `ProfileScreen.kt`; `ProfileViewModel.kt` saves changes.
- String resources (EN + NL) for the new section and its accessibility texts.
- Docs: `docs/dev/` (design, getting-started file list, `how-to/add-a-metric.md`, testing, backup), README, CHANGELOG, light and dark screenshots of the Profile tab.
- No API, no database, no domain model changes.
