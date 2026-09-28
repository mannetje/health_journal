## ADDED Requirements

### Requirement: Visualize weight trend
The system SHALL present a line chart of a Profile's weight measurements over a selectable date range (7/30/90 days or All), including a moving-average overlay line.

#### Scenario: Trend chart rendered with history
- **WHEN** a Profile has two or more weight measurements within the selected date range
- **THEN** the system SHALL render a line connecting the measurements in chronological order, plus a moving-average overlay line, on a time axis

#### Scenario: Insufficient data for a trend
- **WHEN** a Profile has fewer than two weight measurements within the selected date range
- **THEN** the system SHALL show a message indicating more entries are needed instead of an empty or misleading chart

### Requirement: Summarize weight trend statistics
The system SHALL display the latest, minimum, and maximum weight values, and the change versus the previous measurement, for the selected date range.

#### Scenario: Statistics computed for a range
- **WHEN** a Profile has at least one weight measurement within the selected date range
- **THEN** the system SHALL display the latest value, the minimum, the maximum, and the signed difference between the latest and the immediately preceding measurement

### Requirement: Show NHG BMI category on latest weight trend point
The system SHALL annotate the latest point on the weight trend chart with its NHG BMI category, using the same category-to-color mapping used on the Log and History screens.

#### Scenario: Latest point annotated
- **WHEN** the latest weight measurement in the selected range has a calculable BMI
- **THEN** the system SHALL show that measurement's NHG BMI category label and color alongside the chart
