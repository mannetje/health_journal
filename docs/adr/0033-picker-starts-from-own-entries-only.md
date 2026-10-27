# 33. Pickers Start from the Person's Own Entries Only

- **Date:** 2026-10-27
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

[ADR 0018](0018-smart-input-pickers.md) let the Log pickers start at the latest entry, else at a value derived from the profile, else at a fixed value. The derived values were an "ideal": the weight for a BMI of 22.5 at the person's height, and a waist figure chosen by sex. The fixed 120/80 and 5.5 mmol/L are what the NHG tables call normal. The app's line, set in [ADR 0021](0021-medication-compliance-and-privacy.md) and followed in [ADR 0026](0026-oxygen-saturation-without-assessment.md), is to record and show what is entered and to assess nothing. A starting position that sits on an ideal reads as a target, and a Save button that records an untouched picker can store a number the person never chose.

## Decision

- **Start from the person's own latest entry.** Nothing is derived from the profile any more: no BMI-based weight and no waist by sex.
- **Without an entry the picker only shows a starting position** (weight 75.0 kg, blood pressure 120/80, pulse 70 bpm, waist 90 cm). These are scroll positions, not targets. Glucose has no starting position: its field is empty.
- **A starting position is not a value.** Until the person moves a picker or types, that value is not chosen: no BMI, blood pressure or waist category is shown for it, and it is never recorded. Save is off, with a short hint, until weight, waist, or both blood pressure numbers are set. Pulse is optional: untouched, the entry is saved without a pulse.
- **An entry the person made earlier counts as chosen**, so repeating the last value stays one tap.

## Consequences

### Positive
- No number in the app's voice is offered as the one to aim for, and no value is recorded by accident.
- Previews appear only for numbers the person set.

### Negative / Trade-offs
- A first entry at exactly the starting position needs a small scroll away and back.
- One more rule in the ViewModel (`LoggingUiState.chosen`) to keep in step with new pickers.
