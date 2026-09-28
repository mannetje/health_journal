## ADDED Requirements

### Requirement: Visualize blood pressure trend
The system SHALL present an overlaid systolic/diastolic line chart of a Profile's blood pressure readings over a selectable date range (7/30/90 days or All), pannable and zoomable across that range.

#### Scenario: Trend chart rendered with history
- **WHEN** a Profile has two or more blood pressure readings within the selected date range
- **THEN** the system SHALL render a systolic line and a diastolic line, each in a distinct color, connected in chronological order on a shared time axis

#### Scenario: Insufficient data for a trend
- **WHEN** a Profile has fewer than two blood pressure readings within the selected date range
- **THEN** the system SHALL show a message indicating more entries are needed instead of an empty or misleading chart

### Requirement: Summarize average blood pressure
The system SHALL display the average systolic and diastolic values for the selected date range, the resulting NHG category of that average, and a gauge visualization showing where the average falls between the NHG category thresholds.

#### Scenario: Average computed for a range
- **WHEN** a Profile has at least one blood pressure reading within the selected date range
- **THEN** the system SHALL display the average systolic/diastolic values, classify that average against the NHG categories, and render a gauge bar per value showing its position relative to the NHG thresholds

### Requirement: Show blood pressure category distribution
The system SHALL display the percentage of readings in the selected date range that fall into each NHG blood pressure category, along with the minimum and maximum readings.

#### Scenario: Distribution computed for a range
- **WHEN** a Profile has at least one blood pressure reading within the selected date range
- **THEN** the system SHALL display, for each NHG category present in that range, the percentage and count of readings in that category, plus the minimum and maximum systolic/diastolic readings
