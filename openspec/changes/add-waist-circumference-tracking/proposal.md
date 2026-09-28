# Add Waist Circumference Tracking (Optional)

## Why
Researching `add-profile-sex-field` surfaced that waist circumference (buikomvang) is the one metric where Dutch guidance (Voedingscentrum) genuinely differentiates by sex — unlike BMI, blood pressure, or glucose, which don't. Now that an optional `sex` field exists on Profile, waist circumference becomes a metric this app *could* classify meaningfully. You asked for this to be added as a **future option — not necessary**, i.e. a low-priority, opt-in capability, not something that blocks or is required for `add-profile-sex-field` or any of the other in-flight changes.

## What Changes
- Add a new optional health metric: waist circumference in centimetres, recorded like weight/blood pressure/glucose (a `WaistCircumferenceEntry` with timestamp, value, Profile).
- Add `NhgWaistCircumferenceCategory` classification, sourced from Voedingscentrum's published thresholds:
  - Women: HEALTHY (68–80 cm), INCREASED_RISK (80–88 cm), HIGH_RISK (≥88 cm)
  - Men: HEALTHY (79–94 cm), INCREASED_RISK (94–102 cm), HIGH_RISK (≥102 cm)
- Classification requires the Profile's `sex` to be set (added by `add-profile-sex-field`); if sex is not specified, the measurement is still recorded but shown **without** a category badge (same "can't classify without a prerequisite field" pattern already used for BMI, which needs `height`).
- Add a Waist Circumference tab to `LogMetricScreen` (input in cm) and a corresponding filter/card type on `HistoryScreen`, following the existing Weight/BP/Glucose pattern.
- Include in CSV export/import alongside the other metrics.
- Entirely additive and optional: existing users, existing Profiles without `sex` set, and existing data are unaffected. No other metric's calculation changes.

## Capabilities
- **Added Capability:** `health-metrics/waist-circumference`

## Impact
- Affected code: new `domain/.../model/metrics/WaistCircumferenceCm.kt`, `WaistCircumferenceEntry.kt`, `domain/.../model/nhg/NhgWaistCircumferenceCategory.kt`, `RecordWaistCircumferenceUseCase.kt`; `:data` module Room entity/DAO/migration; `HealthLogRepositoryPort` gains waist-circumference methods; `:app` — `LogMetricScreen.kt`, `HistoryScreen.kt`, CSV export/import mapping, and (per `add-localization`) new string resources rather than hard-coded labels.
- Depends on `add-profile-sex-field` for the `sex` field the classification needs; can be implemented in parallel but should archive after (or together with) it.
- `docs/adr/0005-dutch-nhg-guidelines.md` gets a new numbered point documenting the Voedingscentrum-sourced, sex-differentiated waist-circumference thresholds (see `design.md`).
- `README.md` "Key Features" list gets a new bullet once implemented.
- This proposal intentionally stays low-priority/optional per your request — no urgency implied, and no other change depends on it.
