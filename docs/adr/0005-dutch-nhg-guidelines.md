# 5. Dutch NHG Clinical Guidelines for Health Metric Classification

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Users tracking blood pressure, blood glucose, and body weight require meaningful clinical classification. Different regions utilize varying standards and primary units (e.g. mmol/L vs mg/dL for glucose).

## Decision

We standardize on the clinical practice standards established by the **Dutch College of General Practitioners** (*Nederlands Huisartsen Genootschap* / NHG):

1. **Blood Pressure:** Evaluated against NHG thresholds into 3 bands: Normal (below 140/90), High (systolic from 140 or diastolic from 90) and Seriously raised (systolic from 180 or diastolic from 110). The six earlier categories (Optimal, Normal, High Normal, Hypertension Grades 1–3) were merged by the change `reword-range-labels`; stored and imported old names are read as the new bands (OPTIMAL, NORMAL, HIGH_NORMAL to Normal; grades 1 and 2 to High; grade 3 to Seriously raised). Downgrading to an older build is not supported.
2. **Blood Glucose:** Canonical internal and default presentation unit is **mmol/L**. Measurements are classified by context:
   - Fasting: Hypoglycaemia (<3.5), Normal (3.5–6.0), Impaired Fasting (6.1–6.9), Diabetes Range (≥7.0).
   - Postprandial: Hypoglycaemia (<3.5), Normal (<7.8), Impaired Glucose Tolerance (7.8–11.0), Diabetes Range (>11.0).
   - An exact unit conversion helper is provided for mg/dL input ($1\text{ mg/dL} = 0.0555\text{ mmol/L}$).
3. **Body Mass Index (BMI):** Calculated as $\text{weight (kg)} / (\text{height (m)})^2$ and categorized against WHO/NHG cutoffs (Underweight, Normal, Overweight, Obese). BMI, blood pressure, and blood glucose classification are **sex-independent** — none of NHG's thresholds for these three metrics differ by sex, confirmed against NHG-guideline summaries and Voedingscentrum's published BMI ranges (see `openspec/changes/archive/2026-09-28-add-profile-sex-field/design.md`).
4. **Waist Circumference (planned, see `openspec/changes/add-waist-circumference-tracking/`):** Categorized using Voedingscentrum-published, **sex-differentiated** thresholds — Women: Healthy 68–80cm / Increased Risk 80–88cm / High Risk ≥88cm; Men: Healthy 79–94cm / Increased Risk 94–102cm / High Risk ≥102cm. This is the one metric where Dutch guidance requires knowing the Profile's sex, unlike BMI/BP/glucose above.
5. **Source order and wording:** NHG leads for every threshold. Diabetes Fonds, DVN and Hartstichting may be used for plain-language wording only. Labels read "name · range", never name a condition (no diabetes, prediabetes or hypertension), and the source is shown once per screen in an "About these ranges" sheet. The glucose category names stay internal identifiers and are never shown. Verification against secondary Dutch sources (2026-10-03; the NHG standards themselves were not reachable): blood pressure 140/90 as the hypertension limit and 180 systolic as severely raised are confirmed. Glucose fasting normal below 6.1, impaired 6.1 to 6.9, diabetes from 7.0, and 11.1 after a meal agree with the limits used (6.0 and 6.9 fasting, 11.0 after a meal, at one decimal). Still open against the NHG text: the diastolic 110 limit for Seriously raised, the low glucose limit 3.5 (sources give 3.6 as the lower normal edge and 4.0 as the hypo limit), and the BMI class wording (task 5.1 of `reword-range-labels`).
6. **Implementation Location:** All classification rules are implemented as pure functions inside the `:domain` module for offline, deterministic evaluation.

## Consequences

### Positive
- Medically sound, deterministic clinical feedback.
- Pure domain implementation ensures zero framework dependencies and full unit testability.

### Negative / Trade-offs
- Users accustomed to mg/dL glucose require input conversion, which is supported explicitly via helper utilities.
