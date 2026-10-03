# Reword Range Labels (NHG leading)

## Why
The app shows a label next to every BMI, blood pressure and glucose value. Today those labels read as medical verdicts ("Hypertension Grade 2", "Diabetes Range", "Impaired Fasting Glucose") and the colours go from green to severe red. That conflicts with the intended purpose of the app: a personal logging tool that is not a medical device and does not diagnose (see `add-medication-management`, compliance).

The labels also claim NHG as source for six blood pressure bands (Optimal, Normal, High Normal, Grade 1 to 3). Those six bands come from the older European scheme. The NHG standard (Cardiovascular risk management) only draws the limits at 140 (high) and 180 (seriously raised) systolic. A label must not credit NHG for something NHG does not say.

This change rewords the labels before any medication code is written, so the new screens inherit a clean rule.

## Source order
1. **NHG** (richtlijnen.nhg.org and the patient site Thuisarts) leads. Where another source differs, the NHG term or threshold wins.
2. **Diabetes Fonds, DVN and Hartstichting** are also used, for plain-language wording. They never override an NHG threshold.

## What Changes
- **Rule:** a label names a measurement band, always together with its range and the source, and never names a medical condition. A not-a-diagnosis note and the source links are reachable from every screen that shows a label.
- **BMI (names stay, range added):** Underweight, Normal, Overweight, Obese, each followed by its BMI range. Thresholds unchanged.
- **Blood pressure (six bands become three, NHG limits):** Normal (below 140/90), High (from 140/90), Seriously raised (from 180 systolic or 110 diastolic). Wording follows Hartstichting. The "Optimal", "High normal" and "Grade 1 to 3" names disappear. No low band.
- **Glucose (wording only, thresholds unchanged):** Low, Normal, Slightly raised and High blood glucose, using Diabetes Fonds wording ("iets hogere bloedsuiker") with the NHG limits. The terms diabetes, prediabetes, impaired and "gestoord" are never shown. Only the range for the entry's context (fasting or after a meal) is shown, in the display unit of the user (mmol/L or mg/dL).
- **Colours:** the green, yellow, orange and red traffic-light palette is replaced by one neutral sequential ramp, so no value looks like a warning.
- **Stored data:** stored enum names stay for BMI and glucose. For blood pressure the six stored names collapse into three. Old rows are read through a legacy-name mapping and keep their values, so no Room migration (database stays at version 2, or 3 when the medication change lands first). The CSV `classification` column writes the new names and import already recomputes it, so old files still import.

## Capabilities
- **Added Capability:** `health-metrics/range-labels`
- **Modified Capabilities:** `health-metrics/blood-pressure`, `health-metrics/glucose`, `health-metrics/weight`, `health-trends`, `data-export`

## Impact
- Code: `domain` (`NhgBloodPressureCategory` three values, `fromStoredName`), `data` (`HealthLogMapper` reads legacy names), `app` (`NhgLabels`, `NhgColors`, `Color.kt`, log preview, history cards, trend summary and distribution bar, a source and note sheet), both `strings.xml` files.
- Tests: classifier boundaries, legacy-name mapping, label and range strings in both languages, mg/dL range display, CSV export names.
- Docs: README (BP bands), ADR 0005 (add a note and a source-order section), ADR 0006 (colours), `openspec/specs/README.md` glossary, the `add-medication-management` compliance delta (range wording).
- Release: user-visible wording and colours, so a minor version bump (suggested 1.5.0). The release is made only when asked.

## Verification gap
The NHG standards could not be read in full while writing this (the site returned 401). The 140 and 180 limits come from NHG search summaries and Thuisarts, and the glucose and BMI thresholds match the existing code. Before release, each band is checked against the NHG text (task 5.1). If NHG differs, NHG wins and the spec is corrected.

## Non-goals
- No new health logic, no alerts, no advice, no low blood pressure band, no age-specific limits.
- No change to stored measurement values, ranges of valid input, or the CSV measurement columns.
- No NHG clinical terms as a second line (they would read as diagnoses).
