## REMOVED Requirements

### Requirement: Category colors
**Reason**: The traffic-light palette (green, yellow, orange, red) and the six blood pressure categories it was defined for no longer exist; colours now follow the neutral ramp of `range-labels`.
**Migration**: Use the requirement "Neutral category colors" below.

## ADDED Requirements

### Requirement: Neutral category colors
Category colors SHALL follow the neutral sequential ramp of `range-labels`, with a lighter variant in dark mode. Palette (light / dark, hex RGB): step 0 546E7A / B0BEC5; step 1 1E6FB5 / 64B5F6; step 2 3949AB / 7986CB; step 3 283593 / 9FA8DA; step 4 1A237E / C5CAE9. A step MAY be adjusted slightly to reach the required text contrast, and the order SHALL stay.
- BMI: UNDERWEIGHT step 0, NORMAL step 1, OVERWEIGHT step 2, OBESE step 3
- Blood pressure: NORMAL step 1, HIGH step 3, SERIOUSLY_RAISED step 4
- Glucose: HYPOGLYCAEMIA step 0, NORMAL step 1, IMPAIRED_FASTING and IMPAIRED_GLUCOSE_TOLERANCE step 2, DIABETES_RANGE step 3

#### Scenario: Highest blood pressure color
- **WHEN** a SERIOUSLY_RAISED reading is shown in dark mode
- **THEN** it uses C5CAE9

### Requirement: Category distribution by band
The blood pressure distribution bar and its legend SHALL group entries by band (three groups at most), show each band's name and range, and wrap long labels.

#### Scenario: Old rows grouped
- **WHEN** a profile has rows stored as OPTIMAL, NORMAL and HIGH_NORMAL
- **THEN** the bar shows one Normal segment with their combined count
