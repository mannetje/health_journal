# Smart Pre-fill and Scrolling Number Pickers

## Why
Currently, logging health metrics requires manual text input via keyboard, which is prone to friction and typos. Furthermore, entry forms start blank or with hardcoded defaults. By introducing smart pre-fill (falling back from latest entry → personalized profile-based defaults → standard defaults) and interactive scrolling horizontal number pickers (ruler pickers for weight/waist and stacked BP/pulse monitors), logging becomes tactile, instantaneous, and intuitive. This change also extends Blood Pressure tracking to include pulse (heart rate) data.

## What Changes
- **Blood Pressure & Pulse Schema Update:** Add `pulse` (heart rate in bpm) to `BloodPressureEntry`, Room entity/DAO, and database migration.
- **Smart Pre-fill Fallback Chain:**
  1. Latest recorded entry for the metric.
  2. Profile-derived personalized defaults (e.g., Weight based on height for BMI ~22.5, waist based on sex).
  3. Sane standard defaults (75 kg weight, 120/80 BP, 70 bpm pulse, 90 cm waist).
- **Horizontal Ruler Picker (Weight & Waist):** Canvas-based tick marks (long whole numbers, short decimals), fixed center indicator, and smooth snapping via `rememberSnapFlingBehavior`.
- **Stacked BP & Pulse Number Picker:** Three stacked horizontal `LazyRow` components (Systolic in Red, Diastolic in Blue, Pulse in Green) with colored selection bounding boxes and faded unselected states.
- **Instant State Integration:** Pickers auto-scroll to pre-filled values on load and update ViewModel state in real-time as users scroll.

## Capabilities
- **Added Capability:** `health-metrics/smart-input-pickers`
- **Modified Capability:** `health-metrics/blood-pressure`

## Impact
- Affected code: Domain models, Room entities/DAOs/migrations, ViewModel initialization logic, and Jetpack Compose UI components for `LogMetricScreen`.
