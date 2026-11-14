## ADDED Requirements

### Requirement: History shows entries and chart side by side on wide windows
The History tab SHALL show the entry timeline and the chart with its statistics side by side when the window is at least 900 dp wide and a metric with a chart is selected. The range selection SHALL apply to both. In that layout the graph/list toggle SHALL NOT be shown and the History content SHALL be centred in a column no wider than 1240 dp. On narrower windows the History tab SHALL behave as before, and a stored graph or list choice per metric SHALL survive a change between the two layouts.

#### Scenario: Tablet landscape
- **WHEN** the window is 1280 dp wide and the Weight filter is selected
- **THEN** the weight entries are shown on the left and the trend and statistics on the right, with the range chips above both and no graph toggle

#### Scenario: Tablet portrait
- **WHEN** the window is 800 dp wide and the Weight filter is selected
- **THEN** one pane is shown with the graph toggle, as on a phone

#### Scenario: No chart for the filter
- **WHEN** the window is 1280 dp wide and the All filter is selected
- **THEN** one list is shown, centred in a column of at most 840 dp

#### Scenario: Migraine day from the chart
- **WHEN** the window is 1280 dp wide, the Migraine filter is selected and the user taps a day on the chart
- **THEN** the entry list next to the chart shows that day's migraines and the chart stays visible
