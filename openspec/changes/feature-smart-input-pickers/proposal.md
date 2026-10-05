# Smart Pre-fill and Scrolling Number Pickers

## Why
Currently, logging health metrics requires manual text input via keyboard, which is prone to friction and typos. Furthermore, entry forms start blank or with hardcoded defaults. By introducing smart pre-fill (falling back from latest entry → personalized profile-based defaults → standard defaults) and interactive scrolling horizontal number pickers (ruler pickers for weight/waist and stacked BP/pulse monitors), logging becomes tactile, instantaneous, and intuitive. This change also extends Blood Pressure tracking to include pulse (heart rate) data, while strictly adhering to project guidelines including bilingual localization (English/Dutch), offline-first architecture, and Material 3 design standards.

## What Changes
- **Blood Pressure & Pulse Schema Update:** Add `pulse` (heart rate in bpm) to `BloodPressureEntry`, Room entity/DAO, and database migration (version 3 to 4).
- **Smart Pre-fill Fallback Chain:**
  1. Latest recorded entry for the metric.
  2. Profile-derived personalized defaults (e.g., Weight based on height for BMI ~22.5, waist based on sex).
  3. Sane standard defaults (75 kg weight, 120/80 BP, 70 bpm pulse, 90 cm waist).
- **Horizontal Ruler Picker (Weight & Waist):** Canvas-based tick marks (long whole numbers, short decimals), fixed center indicator, and smooth snapping via `rememberSnapFlingBehavior`.
- **Stacked BP & Pulse Number Picker:** Three stacked horizontal `LazyRow` components (Systolic in Red, Diastolic in Blue, Pulse in Green) with colored selection bounding boxes and faded unselected states.
- **Internationalization (i18n & l10n):** English and Dutch localized string resources for all picker labels, units, and descriptions, respecting system/app language override and regional number formats.
- **Project Guidelines Adherence:** Hexagonal architecture isolation (`:domain`, `:data`, `:app`), Material 3 styling, offline-first design, accessibility (TalkBack descriptions, 48dp touch targets), and zero network calls.

## Capabilities
- **Added Capability:** `health-metrics/smart-input-pickers`
- **Modified Capability:** `health-metrics/blood-pressure`
- **Modified Capability:** `localization`

## Impact
- Affected code: Domain models, Room entities/DAOs/migrations, ViewModel initialization logic, string resources in `values/strings.xml` and `values-nl/strings.xml`, and Jetpack Compose UI components for `LogMetricScreen`.
