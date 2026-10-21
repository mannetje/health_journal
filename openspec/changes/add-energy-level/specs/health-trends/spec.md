## ADDED Requirements

### Requirement: Energy graph view
Under the Energy filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one dot per entry over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a fixed value axis from 0 to 10, in a single neutral hue. It SHALL draw no band, reference line, limit or colour threshold.

#### Scenario: Dots only
- **WHEN** the range holds entries of 8, 5 and 2
- **THEN** three dots are drawn in one colour with no reference line

#### Scenario: Too little data
- **WHEN** the range holds fewer entries than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Energy figures
The graph view SHALL show, for the selected range, the number of entries, the average (one decimal), the lowest and the highest. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds 8, 5 and 2
- **THEN** the count is 3, the average is 5.0, the lowest is 2 and the highest is 8

#### Scenario: Several in one day
- **WHEN** a day holds entries of 7 and 3
- **THEN** both count as separate entries in every figure

#### Scenario: Empty range
- **WHEN** the range holds no entry
- **THEN** the view shows a plain empty text and no figures
