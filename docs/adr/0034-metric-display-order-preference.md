# 34. User-Configurable Metric Display Order

- **Date:** 2026-10-28
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

The Log and History screens previously hardcoded the order in which metrics (Weight, Blood Pressure, Glucose, Migraine, Waist, Activity) appear as tabs and filter chips. Users have different logging habits and benefit from placing their most-used metrics first. The existing preference infrastructure (`SharedPreferences` wrappers in `app/settings/`) is a proven, dependency-free pattern for persisting lightweight UI preferences.

## Decision

A new `MetricDisplayOrderPreference` class in `:app/settings/` stores the user's chosen metric sequence as a comma-separated string of `MetricType` names in a `SharedPreferences` file named `"metric_order_prefs"`. Both `LoggingViewModel` and `HistoryViewModel` read this preference and expose an `orderedMetrics: List<MetricType>` in their UI state. The Log and History screens iterate this list to render tabs and chips respectively, replacing all hardcoded sequences. A drag-to-reorder list in the Settings screen lets the user change the order; changes are written immediately to the preference.

The default order (when no preference is saved) is: **Weight → Blood Pressure → Glucose → Migraine → Waist Circumference → Activity**.

## Consequences

### Positive
- Metric presentation is personalised to the user's workflow.
- A single preference drives both Log and History surfaces — no duplication.
- The pattern is consistent with existing preference classes (`UnitPreference`, `ThemePreference`).
- No external library dependency; drag-to-reorder is implemented with Compose primitives.
- Forward-compatible: new metric types added in future are appended at the end of any saved order without breaking existing preferences.

### Negative / Trade-offs
- `MetricType.ordinal` can no longer be used to derive the selected tab index; callers must use `indexOf` on the ordered list.
- A manual drag-to-reorder implementation is more verbose than a dedicated library; acceptable given the small fixed list size (6 items).
