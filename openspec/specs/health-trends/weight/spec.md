# weight-trends Specification

## Purpose
Weight trend chart, statistics and moving average on the History screen. Builds on health-trends (range, axes, zoom).

## Requirements

### Requirement: Weight statistics
Statistics SHALL be computed in the user's display unit over the range-filtered entries sorted by time: Latest (last entry), Change (last minus first entry of the range, 0 with fewer than 2 entries; signed with "+" only when positive; 1 decimal), Min and Max, and the latest BMI with its category and color when the latest entry has a BMI.

#### Scenario: Change over range
- **WHEN** entries in range are 80.0, 79.0 and 78.4 kg
- **THEN** Latest is 78.4, Change is -1.6, Min 78.4, Max 80.0

#### Scenario: Positive change
- **WHEN** first is 70.0 and last is 71.2
- **THEN** Change is shown as +1.2

### Requirement: Moving average
The user SHALL be able to choose a moving-average window of 3, 5, 7 or 10 points; default 5. The average at each point is the mean of the up to N points ending at that point (index based, not calendar days), using fewer points at the start of the series. A window of 1 or less returns the raw points. The average line is drawn over the weight line.

#### Scenario: Start of series
- **WHEN** window is 5 and the series has 3 points
- **THEN** the third average is the mean of the first three points

### Requirement: Window label
The window counts entries, not days, and the UI label SHALL say so ("N-entry avg"; Dutch "N-metingen gem.").

#### Scenario: Label
- **WHEN** window 5 is selected
- **THEN** the label communicates 5 measurements
