# Design: Medication Adherence

## Context
`add-medication-management` stores only outcomes and derives planned intakes from schedule versions (ADR 0020). Adherence is a pure `:domain` calculation over those two inputs plus "now". The Adherence view is a third view in the pillbox screen, so the bottom bar is untouched.

## Calculation
`AdherenceCalculator(medications, intakes, range, now)` returns `taken`, `due`, `skipped`, `missed`, `percentage` and `streak`.
- **Due** counts planned intakes in the range that are in the past, taken from the schedule version in force on each date. Days before the medication's start date, after its end date or from `archivedFrom` are not due.
- **Taken** counts TAKEN outcomes on planned intakes, whatever the actual amount.
- **Skipped** counts as due and not taken, but is shown separately because skipping on a doctor's advice differs from forgetting. **Missed** is due, no outcome and the 2-hour grace period passed. An intake still inside the grace period is neither missed nor counted yet.
- **As-needed** medications have no due count and no percentage, only the number of doses.
- **Orphan outcomes** (a taken intake whose planned time is no longer planned) are not due and not counted in the percentage, as stated in the schedule-versions rule of ADR 0020.
- **Percentage** is taken divided by due, rounded half up to a whole percent, like the blood pressure averages. No percentage is shown when due is 0.
- **Streak** is the number of consecutive days up to today with at least one planned intake and all of them taken. Days with no planned intake neither extend nor break it.

## The schedule-edit exception, applied
Because planned intakes come from schedule versions, an edit applied from today leaves every earlier day unchanged. If the user deliberately applies a schedule from an earlier date to correct a mistake, past "due" changes with it (the documented exception in ADR 0020), and the Adherence view shows the recalculated figures without any extra text.

## View
Per-medication rows with percentage, taken of due, skipped and missed counts for the chosen range (7, 30, 90 days), an overall row, the streak, and the missed list. Wording is neutral status only. Medication names and doses come from the user's input.

## Alternatives considered
- **Storing planned intakes to count them:** rejected for the reasons in ADR 0020.
- **Counting skipped as not due:** rejected, it would let skipping raise the percentage. It is shown separately instead.
- **A weighted or "good/bad" score:** rejected, it reads as advice (`compliance`).

## Risks
- A percentage can be read as a verdict. Mitigation: neutral labels, no colours that imply good or bad (neutral ramp), no wording about targets.
