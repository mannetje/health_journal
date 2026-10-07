## ADDED Requirements

### Requirement: Adherence tracking
The system SHALL compute per-medication and overall adherence for the last 7, 30 and 90 days as taken divided by due, rounded to a whole percent (half up), and the current streak of days with all planned intakes taken. Due SHALL use the schedule version in force on each date.

#### Scenario: Adherence calculated
- **WHEN** 28 intakes were due in the last 30 days and 26 were taken
- **THEN** adherence SHALL be 93%

#### Scenario: Skipped shown separately
- **WHEN** an intake was skipped
- **THEN** it SHALL count as due and not taken, and the report SHALL show skipped and missed counts separately

#### Scenario: Still inside the grace period
- **WHEN** a planned intake has no outcome and the 2-hour grace period has not passed
- **THEN** it SHALL NOT count as missed

#### Scenario: As-needed medication
- **WHEN** a medication is as-needed
- **THEN** the report SHALL show the number of doses and no percentage

#### Scenario: Not due before the start date
- **WHEN** a medication starts on 2026-10-10
- **THEN** earlier dates SHALL NOT count as due

#### Scenario: Nothing due
- **WHEN** no intake was due in the chosen range
- **THEN** the report SHALL show no percentage

#### Scenario: Schedule edited from today
- **WHEN** a schedule is changed from today
- **THEN** adherence for earlier days SHALL be unchanged

#### Scenario: Schedule corrected from an earlier date
- **WHEN** the user applies a schedule from an earlier date to correct a mistake
- **THEN** adherence for the days from that date SHALL be recalculated from the corrected schedule and recorded outcomes SHALL stay unchanged

#### Scenario: Outcome without a plan
- **WHEN** an intake outcome exists for a planned time that the schedule no longer produces
- **THEN** it SHALL NOT count as due and SHALL NOT change the percentage

#### Scenario: Correcting a past intake
- **WHEN** the user changes the outcome of a past intake
- **THEN** adherence SHALL be recalculated

### Requirement: Adherence view in the pillbox screen
The system SHALL show adherence as a view within the pillbox screen next to Today and Medications, without adding a bottom tab, with neutral wording only.

#### Scenario: Open the view
- **WHEN** the user opens the Adherence view
- **THEN** the system shows the overall and per-medication figures for the chosen range, the streak, skipped and missed counts and the list of missed intakes with medication and planned time

#### Scenario: No judgement
- **WHEN** the figures are shown
- **THEN** the system SHALL NOT label them good or bad, show targets, encouragement or advice, or use colours that imply either
