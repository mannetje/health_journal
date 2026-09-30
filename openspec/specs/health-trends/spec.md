# health-trends Specification

## Purpose
Defines the behavior shared by all trend visualizations on the History screen: range selection, chart drawing rules, axes, zoom and category colors. Metric-specific statistics are in health-trends/weight, health-trends/blood-pressure and health-trends/glucose.

## Requirements

### Requirement: Range selection
The user SHALL be able to choose a range of 7 days, 30 days, 90 days or ALL. The default is 30 days. The range filter keeps entries whose timestamp lies within [newest - days, newest], inclusive at both ends, where newest is the timestamp of the newest entry in the list being filtered (NOT the current clock). ALL keeps everything. An empty list yields an empty result. If fewer than 2 entries fall inside the window and the list has at least 2 entries, the window SHALL be widened back to include the second-newest entry so that a trend can be drawn.

#### Scenario: Anchored to newest entry
- **WHEN** the newest weight is 3 months old and 7 days is selected
- **THEN** entries from the 7 days before that newest entry are shown, not an empty chart

#### Scenario: Sparse window widened
- **WHEN** only the newest entry is inside the window and an older one exists
- **THEN** the newest two entries are shown

### Requirement: Range scope
Under a single-metric filter (weight, blood pressure or glucose) the selected range SHALL apply to both the chart and the entry list. Activity is not range-filtered. Under the ALL filter the full lists are shown.

#### Scenario: Range applies to list
- **WHEN** the weight filter and 7 days are selected
- **THEN** the list shows only weight entries within the range

### Requirement: Minimum data for a chart
A chart line SHALL be drawn only with at least 2 points. With fewer points the UI SHALL show a localized message such as "Not enough data yet, log at least two entries to see a trend". Statistics are still shown when at least 1 entry exists.

#### Scenario: One entry
- **WHEN** only one entry is in range
- **THEN** the message is shown, no line is drawn and statistics are shown

### Requirement: Value axis
The y-axis SHALL span the minimum and maximum plotted values padded by 10 percent of (max - min), or by 1.0 when max equals min. It SHALL have 5 labels, formatted with 1 decimal, in the display unit.

#### Scenario: Flat data
- **WHEN** all values equal 70.0
- **THEN** the axis spans 69.0 to 71.0

### Requirement: Calendar-aligned time axis
The x-axis SHALL show at most 7 labels aligned to calendar boundaries in the device time zone. The tick step SHALL be the first of the following whose (visible days / step days) is at most 7, falling back to the last: 6 hours (0.25 d), 12 hours (0.5 d), 1 day, 2 days, 1 week (7 d), 2 weeks (14 d), 1 month (30.4 d), 2 months (60.8 d), 3 months (91.3 d), 6 months (182.6 d), 1 year (365.25 d), 2 years (730.5 d), 5 years (1826.25 d). Week ticks fall on Mondays; month ticks on the first of the month. Label formats: year steps "yyyy"; month steps "MMM yyyy"; day and week steps "d MMM", or "d MMM yyyy" for the first visible label and the first label after the year changes; hour steps "d MMM HH:mm". A bare "1 Jan" label without year SHALL NOT appear. The horizontal position of a point is measured in whole hours since the earliest point.

#### Scenario: Finer ticks on zoom
- **WHEN** the visible span shrinks
- **THEN** a finer tick step is chosen

#### Scenario: Month ticks
- **WHEN** a monthly step is chosen
- **THEN** every tick is on the first of a month and labeled with month and year

### Requirement: Zoom and pan
Charts SHALL support pinch zoom and pan. The initial zoom SHALL fit all plotted content.

#### Scenario: Initial fit
- **WHEN** a chart first appears
- **THEN** all points are visible

### Requirement: Category colors
Category colors SHALL keep their hue identity in both themes, with a lighter variant in dark mode. Palette (light / dark, hex RGB): optimal green 2E7D32 / 66BB6A; normal green 43A047 / 81C784; yellow F9A825 / FFD54F; orange EF6C00 / FF8A50; deep orange D84315 / FF7043; red C62828 / E57373; severe red B71C1C / EF5350.
- BMI: NORMAL optimal green, UNDERWEIGHT yellow, OVERWEIGHT orange, OBESE red
- Blood pressure: OPTIMAL optimal green, NORMAL normal green, HIGH_NORMAL yellow, GRADE_1 orange, GRADE_2 deep orange, GRADE_3 severe red
- Glucose: NORMAL green, IMPAIRED_FASTING and IMPAIRED_GLUCOSE_TOLERANCE orange, HYPOGLYCAEMIA and DIABETES_RANGE red

#### Scenario: Grade 3 color
- **WHEN** a GRADE_3 reading is shown in dark mode
- **THEN** it uses EF5350
