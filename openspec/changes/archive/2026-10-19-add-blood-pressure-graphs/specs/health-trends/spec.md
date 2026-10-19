## ADDED Requirements

### Requirement: Blood pressure band markers
The blood pressure line chart SHALL draw each reading as a marker in the colour of its NHG band, using the neutral band colours, outlined in the colour of its line. The colours SHALL NOT carry a good or bad meaning.

#### Scenario: Marker colour follows the band
- **WHEN** one reading is 150/95 (high band) and another 125/78 (normal band)
- **THEN** the markers of the two readings have the colours of the high and the normal band

### Requirement: Blood pressure graph
The blood pressure graph view SHALL show a graph with the diastolic pressure across and the systolic pressure up, the three NHG bands as shaded areas, and one dot for every reading in the selected range. Newer readings SHALL be drawn with more contrast than older ones. The axes SHALL cover at least 40 to 120 diastolic and 80 to 180 systolic and SHALL widen so every reading is inside. The band limits SHALL equal the limits of the NHG classification. The graph SHALL have a content description with the number of readings and the newest reading, in the app language.

#### Scenario: Readings as dots
- **WHEN** the range holds three readings
- **THEN** three dots are shown, the newest the most prominent, on the band areas

#### Scenario: Outlier widens the axes
- **WHEN** a reading has a systolic pressure of 205
- **THEN** the systolic axis extends beyond 205

### Requirement: Reading steadiness
The blood pressure graph view SHALL show the usual change and the biggest jump between two consecutive readings of the selected range, for systolic and diastolic pressure. The usual change SHALL be the median of the absolute changes. With fewer than two readings it SHALL show a message that asks for more readings instead of figures.

#### Scenario: Usual and biggest change
- **WHEN** the systolic pressure of consecutive readings is 130, 133, 131 and 140
- **THEN** the usual systolic change is 3 and the biggest jump is 9

#### Scenario: Not enough readings
- **WHEN** the range holds one reading
- **THEN** the message to add at least two readings is shown

### Requirement: Comparison with the previous period
The blood pressure graph view SHALL show, for systolic pressure, diastolic pressure, pulse, mean arterial pressure (systolic plus twice diastolic, divided by three) and pulse pressure (systolic minus diastolic), the newest value, the lowest and highest value of the selected range, and the change of the average against the period of the same length directly before it, counted back from the newest reading. A change smaller than 2 mmHg (2 bpm for pulse) SHALL be shown as stable. For the range All, or when there is no earlier period, the change SHALL be shown as a dash. Pulse SHALL be omitted when no reading in the range has a pulse. The card SHALL show figures only, with no target, advice or good-or-bad colour.

#### Scenario: Change against the previous period
- **WHEN** the range is 7 days, the average systolic pressure is 132 in the last 7 days and 142 in the 7 days before
- **THEN** the systolic row shows a change of -10.0 mmHg

#### Scenario: Stable
- **WHEN** the average pulse differs by 1.5 bpm from the previous period
- **THEN** the pulse row shows stable

#### Scenario: Range All
- **WHEN** the range is All
- **THEN** the change of every row is a dash

#### Scenario: No pulse entered
- **WHEN** no reading in the range has a pulse
- **THEN** there is no pulse row

### Requirement: Blood pressure graphs in both languages
Every label, hint, message and content description of the blood pressure graph view SHALL be available in English and Dutch and follow the app language.

#### Scenario: Dutch
- **WHEN** the app language is Dutch
- **THEN** the titles of the graph, steadiness and comparison cards are shown in Dutch
