# Add Medication Adherence

## Why
With a pillbox and an intake log in place (`add-medication-management`), people want to see how consistently they take their medication. This change adds the figures and an Adherence view next to Today and Medications, using only data already stored.

## What Changes
- **Adherence figures:** per medication and overall, taken divided by due, for the last 7, 30 and 90 days, rounded to a whole percent (half up).
- **Streak:** the current number of days in a row with all planned intakes taken.
- **Counts:** skipped and missed counts shown separately from the percentage. As-needed medications show a number of doses and no percentage.
- **Missed list:** a plain list of missed intakes with medication and planned time, status only, no advice.
- **Adherence view:** a third view in the pillbox screen (Today, Medications, Adherence), no bottom tab.
- **Schedule versions:** "due" for a day uses the schedule version in force on that day, so editing a schedule from today does not change earlier days.

## Capabilities
- **Modified Capability:** `medication` (adherence tracking, Adherence view)


## Impact
- **Database:** none. Everything is derived from schedule versions and intakes.
- **Affected code:** `domain` gets `AdherenceCalculator` and a `GetAdherence` use case; `app` gets the Adherence view, strings in English and Dutch.
- **Dependencies:** none.
- **Docs:** README, CHANGELOG, specs. No ADR (a tactical addition on the model of ADR 0020); the rounding and "skipped counts as due" decisions are recorded in the spec.

## Depends on
`add-medication-management`. Does not depend on reminders.

## Deferred
The adherence overlay next to the trend charts (was task 3.3 of the original proposal) is not part of this change. It needs chart design work of its own and will be proposed separately if wanted.

## Non-goals
- No judgement of the figures (no "good", "bad", targets, encouragement or warnings, see `compliance`).
- No export of adherence figures (the CSV already carries the log they come from).
