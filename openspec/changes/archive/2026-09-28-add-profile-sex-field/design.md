# Design: Optional Sex Field on Profile

## Research: does Dutch guidance use sex-differentiated thresholds?

You asked specifically whether Dutch guidelines (NHG / huisartsen) or Voedingscentrum differentiate by sex for weight/BMI, blood pressure, or glucose. Checked against [voedingscentrum.nl](https://www.voedingscentrum.nl/bmi) and NHG-guideline summaries:

| Metric | Sex-dependent in NL guidance? | What actually varies |
|---|---|---|
| **BMI** | **No.** Voedingscentrum: "Een BMI tussen de 18,5 en 25 geeft bij volwassen mannen en vrouwen tussen de 19 en 69 jaar aan dat ze een gezond gewicht hebben" — same range for both sexes. | Age (different ranges for 70+) and ethnicity (different ranges for Asian background) — **not** sex. Neither of those is modeled by this app either. |
| **Waist circumference (buikomvang)** | **Yes**, but this app does not track it. Voedingscentrum: women — healthy 68–80 cm, increased risk 80–88 cm, high risk ≥88 cm; men — healthy 79–94 cm, increased risk 94–102 cm, high risk ≥102 cm. | This is the one place Dutch guidance genuinely needs to know sex. It's a real candidate for a *future* change (`RecordWaistCircumferenceUseCase` + a sex-aware `NhgWaistCircumferenceCategory`), but is explicitly not in scope here — no such metric exists in this app today. |
| **Blood pressure** | **No**, for the diagnostic/target threshold this app models (140/90 mmHg). NHG applies the same target for adult men and women; the only nuance found is an age-based relaxation for men over 80 (not a sex-based threshold difference at the ages this app's `NhgBloodPressureCategory` already covers). | Age, not sex — and age-adjustment isn't modeled by `NhgBloodPressureCategory` today regardless of sex. |
| **Glucose** | **No** sex-differentiated target values found in the NHG Diabetes Mellitus type 2 standard excerpts checked; target values there are individualized by age, diabetes duration, comorbidity, and life expectancy — not sex. | Same conclusion as the proposal's original claim: `NhgGlucoseCategory` needs no sex input. |

**Conclusion**: this confirms the proposal's original stance — `sex` is added as an optional demographic field with **zero effect on any of the app's current calculations** (BMI, blood pressure, glucose). The `Sex is not used in health-metric calculations` requirement in `specs/profile/spec.md` stays as specified. The only Dutch-guidance metric where sex would matter (waist circumference) is out of scope because the app doesn't track that measurement at all — flagged here as a natural, but separate, future change if you want to pursue it.

## Non-goals (unchanged from proposal)
- No waist-circumference tracking or `NhgWaistCircumferenceCategory` in this change.
- No age-adjusted BMI/BP bands in this change (Voedingscentrum's 70+ exception and NHG's 80+ exception both exist, but neither is currently modeled and neither is sex-related, so adding sex doesn't newly require them).
