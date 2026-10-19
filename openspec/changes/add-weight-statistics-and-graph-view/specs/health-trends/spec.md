## ADDED Requirements

### Requirement: List and graph view
When the selected History filter has a trend chart (Weight, Blood pressure, Glucose), the History screen SHALL offer a control in the title row that switches between the entry list and the graph view. The graph view SHALL show the trend chart with its range selector and statistics and SHALL NOT show the entry list; the list view SHALL show the entries and SHALL NOT show the chart. The list SHALL be the default view. The chosen view SHALL be kept per filter while the app is open. Filters without a trend chart (All, Waist, Activity) SHALL show no control and the list. The control SHALL be at least 48 dp and its description SHALL name the view it switches to, in the app language.

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
