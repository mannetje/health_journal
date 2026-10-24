## ADDED Requirements

### Requirement: Mood graph view
Under the Mood filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The chart SHALL show one dot per entry over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends) on a fixed value axis from 1 to 5 labelled with the step numbers, in a single neutral hue. It MAY join the daily means with a thin line, which the person can switch off. A Mood/Stress switch SHALL show the same chart for the stress levels (over the entries that have one), with the same axis and hue and a different marker shape, never a different colour. It SHALL draw no band, reference line, limit, comparison with a previous period, or colour that varies by step.

#### Scenario: Dots only
- **WHEN** the range holds entries of 4, 3 and 2
- **THEN** three dots are drawn in one colour with no reference line

#### Scenario: Several in a day
- **WHEN** a day holds entries of 2 and 4
- **THEN** two dots are drawn that day and the optional line passes through the mean 3.0

#### Scenario: Too little data
- **WHEN** the range holds fewer entries than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Mood figures
The graph view SHALL show, for the selected range, the number of entries, the number of days with at least one entry, the average step (one decimal, over all entries) and the number of entries per step 1 to 5 (steps with none shown as 0); under the Stress switch the same figures over the entries that have a stress level. The two sets SHALL NOT be combined, compared or correlated. Feeling words SHALL NOT change any figure or chart. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds steps 4, 4 and 1 on two days
- **THEN** entries is 3, days is 2, the average is 3.0 and the counts per step are 1 for step 1, 0 for step 2, 0 for step 3, 2 for step 4 and 0 for step 5

#### Scenario: Stress view
- **WHEN** the range holds stress levels 2, 2 and 5 and the Stress switch is on
- **THEN** three dots are drawn with the stress marker shape in the same hue, entries is 3, the average is 3.0 and the counts per step are 0, 2, 0, 0 and 1

#### Scenario: Empty range
- **WHEN** the range holds no entry
- **THEN** the view shows a plain empty text and no figures
