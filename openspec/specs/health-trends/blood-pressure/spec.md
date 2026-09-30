# blood-pressure-trends Specification

## Purpose
Blood pressure trend chart, average gauges and category distribution. Builds on health-trends.

## Requirements

### Requirement: Chart
The chart SHALL draw a systolic line and a diastolic line over the range-filtered entries.

#### Scenario: Two lines
- **WHEN** at least two readings are in range
- **THEN** systolic and diastolic lines are drawn

### Requirement: Average and classification
The average systolic and diastolic over the range SHALL be computed and TRUNCATED to whole numbers (not rounded), then classified with the standard blood-pressure rules. The average is shown with its category and category color.

#### Scenario: Truncation
- **WHEN** systolic values average 129.9
- **THEN** 129 is used for classification

### Requirement: Gauges
A gauge for systolic SHALL map the range 80 to 200 and for diastolic 40 to 120; the filled fraction is (value - min) / (max - min) clamped to 0..1, colored by the average category.

#### Scenario: Clamped
- **WHEN** average systolic is 210
- **THEN** the gauge is full

### Requirement: Min and max
Min and Max chips SHALL show the lowest systolic with the lowest diastolic, and the highest systolic with the highest diastolic, each taken independently of the other.

#### Scenario: Independent extremes
- **WHEN** readings are 150/80 and 120/95
- **THEN** Max shows 150/95 and Min shows 120/80

### Requirement: Category distribution
For each category present, the UI SHALL show the fraction count/total as a rounded percent and the count, sorted by count descending.

#### Scenario: Distribution
- **WHEN** 3 of 4 readings are NORMAL
- **THEN** NORMAL shows 75% and 3, and is listed first
