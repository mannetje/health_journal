# glucose-trends Specification

## Purpose
Glucose trend chart, in-range bars and summary chips. Builds on health-trends.

## Requirements

### Requirement: Chart
The chart SHALL plot glucose in the user's display unit (mmol/L or mg/dL) as a single-colored line over the range-filtered entries.

#### Scenario: Display unit
- **WHEN** the display unit is mg/dL
- **THEN** the axis labels and points are in mg/dL

### Requirement: In-range bars per context
For FASTING and then POSTPRANDIAL a bar SHALL show the share of in-range versus out-of-range entries. In range means category NORMAL; every other category is out of range. A context with no entries is not shown.

#### Scenario: Half in range
- **WHEN** 2 of 4 fasting entries are NORMAL
- **THEN** the fasting bar shows 50 percent in range

### Requirement: Summary chips
Chips SHALL show: Latest (colored by its category), Average (mean of all entries in display units), Time in Range (percent of all entries with category NORMAL, rounded) and Entries (count).

#### Scenario: Time in range
- **WHEN** 3 of 4 entries are NORMAL
- **THEN** Time in Range shows 75%
