## ADDED Requirements

### Requirement: Oxygen saturation graph view
Under the Oxygen filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one dot per reading over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a value axis from the lowest reading minus 2 to 100, in a single neutral hue. It SHALL draw no band, reference line, limit or colour threshold.

#### Scenario: Dots only
- **WHEN** the range holds readings of 98, 95 and 91
- **THEN** three dots are drawn in one colour with no reference line

#### Scenario: Too little data
- **WHEN** the range holds fewer readings than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Oxygen saturation figures
The graph view SHALL show, for the selected range, the lowest, highest and average reading (average with one decimal), the number of readings, and the number of readings per context (readings without a context counted as "no context"). Site and the tick SHALL NOT change any figure or chart. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds 98, 96 and 94
- **THEN** the lowest is 94, the highest is 98, the average is 96.0 and the count is 3

#### Scenario: Empty range
- **WHEN** the range holds no reading
- **THEN** the view shows a plain empty text and no figures

#### Scenario: Per context
- **WHEN** two readings are "at rest", one is "after activity" and one has no context
- **THEN** the counts read 2, 1 and 1
