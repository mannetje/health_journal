## ADDED Requirements

### Requirement: Visualize glucose trend
The system SHALL present a line chart of a Profile's blood glucose readings over a selectable date range (7/30/90 days or All), with each point or line segment colored by its NHG category.

#### Scenario: Trend chart rendered with history
- **WHEN** a Profile has two or more glucose readings within the selected date range
- **THEN** the system SHALL render the readings in chronological order on a time axis, coloring each point or segment by the reading's NHG category

#### Scenario: Insufficient data for a trend
- **WHEN** a Profile has fewer than two glucose readings within the selected date range
- **THEN** the system SHALL show a message indicating more entries are needed instead of an empty or misleading chart

### Requirement: Summarize glucose time-in-range
The system SHALL display, per glucose context (fasting or postprandial), the percentage of readings within the selected date range classified as NHG NORMAL ("in range") versus any other category ("out of range").

#### Scenario: Time-in-range computed for a range
- **WHEN** a Profile has at least one glucose reading in a given context within the selected date range
- **THEN** the system SHALL display the percentage and count of readings in that context classified as NORMAL, separately from the percentage and count of all other categories combined

### Requirement: Summarize glucose trend statistics
The system SHALL display the latest and average glucose values, and the total number of readings, for the selected date range.

#### Scenario: Statistics computed for a range
- **WHEN** a Profile has at least one glucose reading within the selected date range
- **THEN** the system SHALL display the latest value, the average value, and the count of readings in that range
