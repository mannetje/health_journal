# Add Graph View and Weight Statistics

## Why
Under the Weight, Blood pressure and Glucose filters the trend chart is the first item above a long entry list, so the entries start far down and the chart cannot be read in one piece. For weight people also want the numbers around the chart: the lowest and highest weight, how much the weight changed over the last week, month or year, and the change per month and per year.

## What Changes
- **List or graph:** when the selected filter has a trend chart (Weight, Blood pressure, Glucose) a toggle in the History title row switches between the entries and the graph view. The choice is remembered per filter while the app runs. Filters without a chart (All, Waist, Activity) show no toggle and the list as today.
- **Graph view:** the chart and its statistics, using the full screen. The range selector stays above both views, because it limits the entries too. The entry list is not shown there. The list view no longer starts with the chart.
- **Weight statistics** in the weight graph view:
  - **Lowest and highest weight** with their dates.
  - **Change table:** the change and the change per week over the last week, 15 days, month, three months, six months and year, counted back from the newest entry. A kg / % toggle shows the change as a percentage of the starting weight. A period the history does not reach is shown as a dash, not as 0.
  - **Change per month:** a bar chart of the weight change of each month of a selected year (year chips choose the year).
  - **Change per year:** a bar chart of the weight change of each year.
- **Neutral wording:** figures only, no target, no good or bad colour. Bars use one neutral colour for gain and loss and are told apart by direction (above or below the zero line) and the printed value.

## Capabilities
- **Modified Capability:** `health-trends` (graph view, weight statistics)

## Impact
- **Database:** none.
- **Affected code:** a pure `WeightStatistics` (extremes, period change, monthly and yearly change) with JVM tests; the toggle and graph view in `HistoryScreen`; the new weight sections and a bar chart in `ui/history/charts`; strings in English and Dutch.
- **Dependencies:** none; the bar chart is drawn on the existing chart primitives.
- **Docs:** README, CHANGELOG, `docs/dev/design.md`, `docs/dev/testing.md`, the `health-trends` spec, screenshots.

## Non-goals
- No new statistics for Blood pressure or Glucose.
- No weight line behind the bars and no zoom or pan on the bar charts.
- No targets, goal weight, forecasts or advice.
- The new weight statistics do not follow the 7/30/90/all range selector: they always use all weight entries (the existing chart keeps following the range).
