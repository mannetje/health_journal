# 5. Dutch NHG Clinical Guidelines for Health Metric Classification

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Users tracking blood pressure, blood glucose, and body weight require meaningful clinical classification. Different regions utilize varying standards and primary units (e.g. mmol/L vs mg/dL for glucose).

## Decision

We standardize on the clinical practice standards established by the **Dutch College of General Practitioners** (*Nederlands Huisartsen Genootschap* / NHG):

1. **Blood Pressure:** Evaluated against NHG thresholds into 6 categories (Optimal, Normal, High Normal, Hypertension Grades 1–3).
2. **Blood Glucose:** Canonical internal and default presentation unit is **mmol/L**. Measurements are classified by context:
   - Fasting: Hypoglycaemia (<3.5), Normal (3.5–6.0), Impaired Fasting (6.1–6.9), Diabetes Range (≥7.0).
   - Postprandial: Hypoglycaemia (<3.5), Normal (<7.8), Impaired Glucose Tolerance (7.8–11.0), Diabetes Range (>11.0).
   - An exact unit conversion helper is provided for mg/dL input ($1\text{ mg/dL} = 0.0555\text{ mmol/L}$).
3. **Body Mass Index (BMI):** Calculated as $\text{weight (kg)} / (\text{height (m)})^2$ and categorized against WHO/NHG cutoffs (Underweight, Normal, Overweight, Obese). BMI, blood pressure, and blood glucose classification are **sex-independent** — none of NHG's thresholds for these three metrics differ by sex, confirmed against NHG-guideline summaries and Voedingscentrum's published BMI ranges (see `openspec/changes/archive/2026-09-28-add-profile-sex-field/design.md`).
4. **Waist Circumference (planned, see `openspec/changes/add-waist-circumference-tracking/`):** Categorized using Voedingscentrum-published, **sex-differentiated** thresholds — Women: Healthy 68–80cm / Increased Risk 80–88cm / High Risk ≥88cm; Men: Healthy 79–94cm / Increased Risk 94–102cm / High Risk ≥102cm. This is the one metric where Dutch guidance requires knowing the Profile's sex, unlike BMI/BP/glucose above.
5. **Implementation Location:** All classification rules are implemented as pure functions inside the `:domain` module for offline, deterministic evaluation.

## Consequences

### Positive
- Medically sound, deterministic clinical feedback.
- Pure domain implementation ensures zero framework dependencies and full unit testability.

### Negative / Trade-offs
- Users accustomed to mg/dL glucose require input conversion, which is supported explicitly via helper utilities.
