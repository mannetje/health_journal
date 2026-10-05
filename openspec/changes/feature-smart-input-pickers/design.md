# Design: Smart Pre-fill and Scrolling Number Pickers

## Context
Replacing text fields with scrolling number pickers and horizontal rulers improves user experience and eliminates keyboard dependency. Adding heart rate (pulse) to blood pressure records provides a complete cardiovascular snapshot.

## Goals
- Intuitive, tactile data entry via Compose Canvas rulers and horizontal scrolling number pickers.
- Intelligent pre-fill logic reducing input effort to zero when logging repeat or expected values.
- Seamless state binding between picker snap positions and ViewModel input states.

## Architecture & Components
- **Domain:** Update `BloodPressureEntry` and `BloodPressureReading` (or add pulse field).
- **Data:** Room DB migration (adding `pulse` column to blood pressures table), DAO updates, mapper updates.
- **UI:**
  - `HorizontalRulerPicker`: Reusable Compose component with Canvas ticks and snap behavior.
  - `StackedBpPulsePicker`: Three `LazyRow` components with selection bounding boxes (Red, Blue, Green).
- **ViewModel:** Initial state calculation using the 3-tier fallback chain (Latest entry → Profile-derived default → Standard default).
