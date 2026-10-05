# Design: Waist Circumference Tracking

## Context
This app already has three "measure → classify against NHG/Voedingscentrum thresholds" metrics (weight/BMI, blood pressure, glucose), each following the same shape: a value object (`WeightKg`, `BloodPressureReading`, `GlucoseLevel`), a `Record*UseCase`, and a stateless `Nhg*Category.classify(...)` companion function. Waist circumference fits the same shape, with one difference: its classification needs `Profile.sex`, whereas BMI only needs `Profile.height` and BP/glucose need no Profile data at all.

## Goals
- Reuse the existing metric pattern exactly (value object + use case + classification enum) rather than inventing a new shape.
- Classification thresholds sourced directly from Voedingscentrum (cited in `add-profile-sex-field/design.md`), since that is the concrete Dutch source you pointed to for weight-related guidance.
- Graceful degradation when `sex` is unset — consistent with how BMI already degrades gracefully (returns null) when `height` is unset, rather than blocking measurement recording.
- Support logging waist circumference standalone OR as an optional secondary field when logging weight on the Weight tab.

## UI & Logging Flow
- **Weight Tab (`LogMetricScreen`)**: Includes the weight input field, and an optional "Waist circumference (optional)" field below it. When filled in with a valid value (40–200 cm), logging weight invokes both `RecordWeightUseCase` and `RecordWaistCircumferenceUseCase` with the same timestamp.
- **Waist Circumference Tab (`LogMetricScreen`)**: Allows logging waist circumference standalone.
- **History Screen**: Displays waist circumference entries as separate history items/cards, with edit and delete capabilities like other metrics.

## Non-goals
- Not required for any other in-flight change to land. `add-health-trend-visualizations` does not need to add a fourth chart type for this in the same change — a trend chart for waist circumference can be a small, separate follow-up once this metric exists and has real usage data.
- No automatic derivation of waist circumference from any other measurement — it's a direct manual entry, like weight.
- No change to BMI, blood pressure, or glucose classification — confirmed in `add-profile-sex-field/design.md` that none of them use sex; this change doesn't revisit that.

## Domain model
```kotlin
@JvmInline
value class WaistCircumferenceCm(val value: Int) {
    init {
        require(value in MIN_CM..MAX_CM) { "Waist circumference must be between $MIN_CM and $MAX_CM cm" }
    }
    companion object { const val MIN_CM = 40; const val MAX_CM = 200 }
}

enum class NhgWaistCircumferenceCategory {
    HEALTHY, INCREASED_RISK, HIGH_RISK;

    companion object {
        fun classify(waist: WaistCircumferenceCm, sex: Sex): NhgWaistCircumferenceCategory? {
            val v = waist.value
            return when (sex) {
                Sex.FEMALE -> when { v < 80 -> HEALTHY; v < 88 -> INCREASED_RISK; else -> HIGH_RISK }
                Sex.MALE   -> when { v < 94 -> HEALTHY; v < 102 -> INCREASED_RISK; else -> HIGH_RISK }
            }
        }
    }
}
```
(Illustrative — exact boundary inclusivity to be finalized against Voedingscentrum's stated ranges during implementation, mirroring how the existing `Nhg*Category` classes already handle boundary edges.)

`classify` takes a non-null `Sex` — the *use case* is what handles "Profile has no sex set," returning a null category, exactly as `RecordWeightUseCase` already does today for BMI when height is absent:
```kotlin
val category = profile?.sex?.let { NhgWaistCircumferenceCategory.classify(waist, it) }
```

## ADR update
`docs/adr/0005-dutch-nhg-guidelines.md`'s "Decision" list gains a new numbered point (item 5, alongside the existing four) documenting:
> **Waist Circumference:** Categorized using Voedingscentrum-published, sex-differentiated thresholds (Women: Healthy 68–80cm / Increased risk 80–88cm / High risk ≥88cm; Men: Healthy 79–94cm / Increased risk 94–102cm / High risk ≥102cm), since this is the one metric in the app where Dutch guidance differentiates by sex — unlike BMI, blood pressure, and glucose, which this ADR already documents as sex-independent.

This is additive to the existing ADR (same "Accepted" decision, new bullet), not a new ADR — the decision to standardize on Dutch/NHG-family sources is unchanged, this just extends its scope to a new metric.

## README update
Add one bullet under "Key Features," matching the existing bullet style:
> **Waist Circumference (optional):** Record waist circumference in centimetres; categorized against Voedingscentrum's sex-specific healthy-range thresholds when Profile sex is set.

## Alternatives considered
- **Blocking measurement entry until `sex` is set**: rejected — inconsistent with the app's existing "optional prerequisite, graceful degradation" pattern for BMI/height, and inconsistent with `add-profile-sex-field`'s explicit "not required" stance on sex.
- **Building this as part of `add-profile-sex-field` itself**: rejected — that change is scoped to "add the field, touch nothing else," and bundling a new trackable metric into it would make that change's blast radius bigger than what you asked for there.
