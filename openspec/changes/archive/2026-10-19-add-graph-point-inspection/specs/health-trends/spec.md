## ADDED Requirements

### Requirement: Select an entry in a trend chart
A trend chart SHALL let the user select an entry by tapping it. The selection SHALL snap to the entry nearest in time among the selectable entries of the chart, SHALL be marked by a vertical line, and SHALL show the date, time and value of that entry with its unit. Reference lines, moving averages and positions between entries SHALL NOT be selectable. Panning and zooming SHALL keep working.

#### Scenario: Tap between two entries
- **WHEN** the user taps between two weight entries, closer to the later one
- **THEN** the later entry is selected and its date, time and value are shown

#### Scenario: Reference line
- **WHEN** the user taps on an NHG limit line away from any entry
- **THEN** the nearest entry of a data series is selected, never the limit line

### Requirement: Compare two entries
With one entry selected, tapping another entry SHALL show both and the difference: the change in value from the earlier to the later entry, the change in percent of the earlier entry and the time between them. Tapping a further entry SHALL start a new selection with that entry, and tapping the selected entry again SHALL clear the selection. The difference SHALL be shown in the chosen unit, as figures only.

#### Scenario: Two weights
- **WHEN** the user selects 80.0 kg and then 78.0 kg taken 10 days later
- **THEN** the difference shows -2.0 kg, -2.5 % and 10 days

#### Scenario: Clear
- **WHEN** the user taps the selected entry again
- **THEN** the marker disappears

### Requirement: Point inspection in both languages
Every text of the point inspection SHALL be available in English and Dutch.

#### Scenario: Dutch
- **WHEN** the app language is Dutch
- **THEN** the labels and the difference text are Dutch
