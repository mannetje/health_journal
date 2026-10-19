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

### Requirement: Neutral category colors
Category colors SHALL follow the neutral sequential ramp of `range-labels`, with a lighter variant in dark mode. Palette (light / dark, hex RGB): step 0 546E7A / B0BEC5; step 1 1E6FB5 / 64B5F6; step 2 3949AB / 7986CB; step 3 283593 / 9FA8DA; step 4 1A237E / C5CAE9. A step MAY be adjusted slightly to reach the required text contrast, and the order SHALL stay.
- BMI: UNDERWEIGHT step 0, NORMAL step 1, OVERWEIGHT step 2, OBESE step 3
- Blood pressure: NORMAL step 1, HIGH step 3, SERIOUSLY_RAISED step 4
- Glucose: HYPOGLYCAEMIA step 0, NORMAL step 1, IMPAIRED_FASTING and IMPAIRED_GLUCOSE_TOLERANCE step 2, DIABETES_RANGE step 3

#### Scenario: Highest blood pressure color
- **WHEN** a SERIOUSLY_RAISED reading is shown in dark mode
- **THEN** it uses C5CAE9

### Requirement: Category distribution by band
The blood pressure distribution bar and its legend SHALL group entries by band (three groups at most), show each band's name and range, and wrap long labels.

#### Scenario: Old rows grouped
- **WHEN** a profile has rows stored as OPTIMAL, NORMAL and HIGH_NORMAL
- **THEN** the bar shows one Normal segment with their combined count

### Requirement: List and graph view
When the selected History filter has a trend chart (Weight, Blood pressure, Glucose), the History screen SHALL offer a control in the title row that switches between the entry list and the graph view. The graph view SHALL show the trend chart and its statistics and SHALL NOT show the entry list; the list view SHALL show the entries and SHALL NOT show the chart. The range selector SHALL be shown in both views, because the range limits the chart and the entries alike. The list SHALL be the default view. The chosen view SHALL be kept per filter while the app is open. Filters without a trend chart (All, Waist, Activity) SHALL show no control and the list. The control SHALL be at least 48 dp and its description SHALL name the view it switches to, in the app language.

#### Scenario: Switch to the graph
- **WHEN** the Weight filter is selected and the user taps the graph control
- **THEN** the chart and the weight statistics are shown and the entry list is not

#### Scenario: Switch back
- **WHEN** the graph view is shown and the user taps the control again
- **THEN** the entries are shown and the chart is not

#### Scenario: No chart, no control
- **WHEN** the filter is All, Waist or Activity
- **THEN** no list/graph control is shown and the list is shown

#### Scenario: View is kept per filter
- **WHEN** the user shows the Weight graph, selects Glucose (list), and then selects Weight again
- **THEN** the Weight graph is shown and Glucose is still in the list view

### Requirement: Weight extremes
The weight graph view SHALL show the lowest and the highest recorded weight with the date of each, over all weight entries, in the selected weight unit.

#### Scenario: Lowest and highest
- **WHEN** weight entries range from 98.0 kg on 29 March 2012 to 122.1 kg on 7 November 2021
- **THEN** the lowest weight 98.0 kg (29 March 2012) and the highest 122.1 kg (7 November 2021) are shown

### Requirement: Weight change per period
The weight graph view SHALL show a table with the weight change and the change per week over the last week (7 days), 15 days, month, three months, six months and year, counted back from the newest weight entry. The change of a period SHALL be the newest weight minus the weight of the newest entry on or before the start of the period. A period longer than the recorded history SHALL be shown as a dash, not as zero. A control SHALL switch the change between the weight unit and a percentage of the starting weight. The table SHALL use all weight entries and SHALL NOT follow the range selector. The table SHALL show figures only, with no target, advice or good-or-bad colour.

#### Scenario: Change over a month
- **WHEN** the newest entry is 80.0 kg on 20 October and the newest entry on or before 20 September is 82.0 kg
- **THEN** the month row shows -2.0 kg

#### Scenario: History shorter than the period
- **WHEN** the oldest weight entry is 3 weeks old
- **THEN** the three months, six months and year rows show a dash

#### Scenario: Percentage
- **WHEN** the user selects the percentage control and the weight went from 100 kg to 98 kg
- **THEN** the row shows -2.0 %

### Requirement: Weight change per month and per year
The weight graph view SHALL show a bar chart of the weight change of each month of a selected year and a bar chart of the weight change of each year. The change of a month SHALL be the last weight of that month minus the last weight of the previous month that has entries; the first month with entries has no bar. The change of a year SHALL follow the same rule per year. Year chips SHALL choose the year of the monthly chart, offer only years that have weight entries and default to the newest year. Gain and loss SHALL use the same colour and be shown above and below a zero line with the value printed on the bar. The charts SHALL use all weight entries and SHALL NOT follow the range selector.

#### Scenario: Monthly bars
- **WHEN** the last weight of June is 81.0 kg and the last weight of July is 83.9 kg
- **THEN** the July bar of that year is +2.9 kg, above the zero line

#### Scenario: Year chips
- **WHEN** weight entries exist in 2024, 2025 and 2026
- **THEN** the year chips show 2024, 2025 and 2026, with 2026 selected and its months in the monthly chart

#### Scenario: Not enough data
- **WHEN** only one weight entry exists
- **THEN** the lowest and highest weight are shown and the change table and bar charts show the insufficient-data message
