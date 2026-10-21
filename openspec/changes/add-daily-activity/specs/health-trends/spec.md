## ADDED Requirements

### Requirement: Daily activity graph view
Under the Activity filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one bar per day with an entry over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), with a switch between Steps and Active minutes so that one measure is drawn at a time. The value axis SHALL start at 0, bars SHALL use a single neutral hue, and days without an entry for the shown measure SHALL have no bar (not a zero bar). It SHALL draw no goal line, reference line, progress indicator or colour threshold.

#### Scenario: Bars only
- **WHEN** the range holds entries of 9000, 4000 and 6500 steps
- **THEN** three bars are drawn in one colour with no reference line

#### Scenario: Measure switch
- **WHEN** Active minutes is selected and one entry has only steps
- **THEN** that day has no bar

#### Scenario: Too little data
- **WHEN** the range holds fewer days than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Daily activity figures
The graph view SHALL show, for the selected range and measure, the average per day logged (rounded to a whole number), the highest day with its date, the number of days logged and the total. Days without a value for the measure SHALL NOT count as zero in any figure. The activity type SHALL NOT change any figure or chart. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds 9000, 4000 and 5000 steps
- **THEN** the average is 6000, the highest is 9000 with its date, days logged is 3 and the total is 18000

#### Scenario: Gaps do not count as zero
- **WHEN** a 7-day range holds entries on two days only
- **THEN** the average is over those two days

#### Scenario: Empty range
- **WHEN** the range holds no entry
- **THEN** the view shows a plain empty text and no figures
