# 34. User-Configurable Metric Display Order

- **Date:** 2026-10-28
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The Log and History screens fixed the order in which metrics appear as tabs and filter chips. Users have different logging habits and benefit from placing the metrics they use most first. Migraine is not a `MetricType` (it has its own flow and ViewModel), settings live on the Profile tab, the Log pickers are accessible to TalkBack, and the project avoids extra dependencies (ADR-0003). `SharedPreferences` wrappers in `app/settings/` are the existing way to keep small UI preferences.

## Decision

A new `MetricSlot` enum names the orderable entries (Weight, Blood Pressure, Glucose, Migraine, Waist, Activity) and maps each to the existing `MetricType` (none for Migraine) and `HistoryFilter`. `MetricDisplayOrderPreference` stores the order as a comma-separated string of slot names in the `metric_order_prefs` file and exposes it as a `StateFlow`; one shared instance lives in `HealthJournalApp`. `LoggingViewModel` and `HistoryViewModel` collect the flow into `orderedMetrics`, and the Log tabs and History chips iterate that list. Reading drops unknown and repeated names and appends slots the stored value lacks, so later metrics need no migration.

The reorder list is a section on the Profile tab. It offers Move up and Move down buttons next to a drag handle, so reordering works without a drag gesture, and every completed move is saved at once.

The default order is **Weight, Blood Pressure, Glucose, Migraine, Waist, Activity**. This differs from the earlier fixed order (Activity and Waist before Migraine) and applies to every user who has not chosen an order; the changelog says so.

The order is not part of the encrypted backup. Adding it would change the backup format and is left for a separate change.

## Consequences

### Positive
- The metric order follows the user's habits, and one preference drives both screens.
- It matches the existing preference pattern and adds no dependency.
- A change on the Profile tab shows on the Log and History screens at once, with no reload.
- New metrics are appended to saved orders without breaking them.
- Reordering works with TalkBack and without drag gestures.

### Negative / Trade-offs
- `MetricType.ordinal` can no longer give the selected tab index; the index comes from the ordered list.
- Existing users who never open the setting still see Migraine move up, because of the new default.
- The order is not restored from a backup; a restored phone starts on the default order.
- The manual drag code is more verbose than a library; the buttons keep the list usable regardless.
