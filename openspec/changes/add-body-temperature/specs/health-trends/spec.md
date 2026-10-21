## ADDED Requirements

### Requirement: Body temperature graph view
Under the Temperature filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one dot per reading over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a value axis from the lowest reading minus 0.5 to the highest reading plus 0.5 degrees, in a single neutral hue. It SHALL draw no band, reference line, limit or colour threshold.

#### Scenario: Dots only
- **WHEN** the range holds readings of 36.6, 37.4 and 36.9
- **THEN** three dots are drawn in one colour with no reference line

#### Scenario: Too little data
- **WHEN** the range holds fewer readings than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Body temperature figures
The graph view SHALL show, for the selected range, the lowest, highest and average reading (average with one decimal) and the number of readings. Site SHALL NOT change any figure or chart. The figures SHALL be plain numbers in °C with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds 36.4, 36.8 and 37.2
- **THEN** the lowest is 36.4, the highest is 37.2, the average is 36.8 and the count is 3

#### Scenario: Empty range
- **WHEN** the range holds no reading
- **THEN** the view shows a plain empty text and no figures

#### Scenario: Mixed sites
- **WHEN** the range holds readings from the ear and the armpit
- **THEN** they are drawn and counted together without distinction
