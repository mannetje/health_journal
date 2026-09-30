# Units Presentation (Metric Storage, Selectable Display)

## Why
The app stores and shows everything in metric (kg, cm, mmol/L, mmHg, km). Users with an English device but a Dutch or other setup, and users who think in lb, ft/in or mg/dL, cannot choose how values are shown or typed. Region (date and number formats) is already independent of language (ADR 0013). Units are the remaining part of the request: **metric stays the single source of truth for storage, units only change presentation and input.**

## What Changes
- **Storage:** unchanged. Room stays at version 2, values remain kg, cm, mmol/L, mmHg, km. No migration.
- **Domain:** a small pure conversion component (`UnitConversion`) with `toDisplay` and `fromDisplay` per quantity. It reuses the existing `GlucoseLevel` mmol/L and mg/dL conversion (factor 0.0555) so there is one definition.
- **Preference:** a `UnitSystem` setting (System, Metric, Imperial) next to Language and Regional formats on Profile. Default System: metric unless the device region is US, Liberia or Myanmar. Glucose keeps its own mmol/L or mg/dL choice, because it is regional and not tied to imperial (the US uses mg/dL, the UK uses imperial for body weight but mmol/L).
- **Presentation:** weight (kg or lb), height (cm or ft/in) and distance (km or mi) follow the unit system. Blood pressure stays mmHg everywhere. Applies to Log inputs, the edit dialog, History cards, trend charts and stat chips.
- **Input parsing:** typed values are converted to metric before validation, so the existing domain ranges and NHG classification are unchanged.
- **CSV:** import and export stay metric (Libra compatibility). A converted value is never written back in display units.
- **Localized numbers:** decimals in History cards and inputs use the region's separator (comma for Dutch). Fixes the remaining `toString()` decimals.

## Capabilities
- **Added Capability:** `settings/units-presentation`
- **Modified Capabilities:** `history/list`, `logging/input`, `trends/charts`

## Impact
- Affected code: `domain` (conversion component and tests), `app` (`UnitPreference`, `ProfileScreen`, `LogMetricScreen`, `EntryDialogs`, `HistoryScreen`, chart formatters, both `strings.xml` files).
- Rounding: display rounds to one decimal (lb) and back-conversion is exact within display precision. Editing an entry and saving without touching the value must not change the stored metric value (round-trip test required).
- Documentation: README, a new ADR `0014-units-presentation.md`, and an update to the localization diagram.

## Decisions to confirm
1. Include height in ft/in, or keep height in cm only? **Decided: include ft/in; storage stays cm.**
2. Should glucose unit stay a separate setting on the Log screen (as today) or move to Profile? **Decided: one Profile setting, the Log chips are a shortcut. Activity distance is km or mi, not meters.**
3. Default for System: derive from device region (recommended) or always metric. **Decided: derive from the app locale via ICU, glucose from a region table; the user can override.**
4. Show the unit chosen next to every input label. **Decided: yes, and charts follow the same units.**
