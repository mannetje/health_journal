# Add Optional Sex Field to Profile

## Why
You asked to add sex (male/female) as a Profile field that is selectable but **not required**, and asked what today's BMI and other calculations actually need it for.

Answer, from reading the current domain code:
- `Profile.calculateBmi()` (`domain/.../model/profile/Profile.kt`) uses only `weight / height²` — no sex input.
- `NhgBmiCategory.classify()`, `NhgBloodPressureCategory.classify()`, and `NhgGlucoseCategory.classify()` (all under `domain/.../model/nhg/`) classify purely from the measured value(s) (BMI number, systolic/diastolic, glucose + context) — none of them branch on sex today.

So **no existing calculation in this app currently requires sex**, and adding it does not change any current formula or threshold. The NHG guidance area where sex actually matters clinically is waist-circumference categorization (different thresholds for men and women) — but this app does not track waist circumference at all, so that's out of scope here. Sex is added now purely as an optional demographic field on Profile, laying groundwork for any future sex-dependent feature (e.g. if waist circumference is ever added) without forcing existing or new users to provide it.

## What Changes
- Add an optional `sex: Sex?` field to the `Profile` domain model, where `Sex` is a small enum `{ MALE, FEMALE }`. `null` means "not specified."
- Extend `CreateProfileUseCase` (and the corresponding update path) to accept an optional sex value.
- Add a sex selector (e.g. a segmented control: "Not specified" / "Male" / "Female") to `ProfileScreen.kt`, clearly optional — no validation error if left unset, consistent with how `height` is already optional today.
- No change to any BMI/blood-pressure/glucose calculation or classification logic — confirmed above that none of them consume sex.
- Persistence: extend whatever the `:data` module's Profile persistence schema already is (Room entity/columns) with a nullable sex column, defaulting existing rows to "not specified" on migration.

## Capabilities
- **Modified Capability:** `profile` (adds an optional field to Profile; existing name/date-of-birth/height requirements are unchanged)

## Impact
- Affected code: `domain/src/main/kotlin/nl/healthjournal/domain/model/profile/Profile.kt` (new `Sex` enum + field), `CreateProfileUseCase`, `:data` module's Profile persistence (new nullable column + migration), `app/.../ui/profile/ProfileScreen.kt` and `ProfileViewModel.kt`.
- No change to `health-metrics/*` calculation specs — this proposal explicitly does not touch BMI/BP/glucose classification, since none of them use sex.
- Should read against `add-localization`'s string resources for the new selector's labels, and `add-dark-theme`'s tokens for its styling, since both are in flight alongside this change.
