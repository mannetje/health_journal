## ADDED Requirements

### Requirement: Fluid intake daily-total graph view
Under the Fluid filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one bar per local calendar day with the day's total in ml over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a value axis starting at 0, in a single neutral hue. Days without entries SHALL show no bar. When the person has set a personal limit it SHALL draw exactly one thin dashed horizontal line at that number in the same hue, with the number as plain text; bars SHALL NOT change colour above or below it. It SHALL draw no other goal line, reference line, band, progress fill or colour threshold, and the current day SHALL be drawn like any other day.

#### Scenario: Limit line
- **WHEN** a limit of 1500 is set and a day totals 1800 ml
- **THEN** one dashed line is drawn at 1500 and the 1800 bar has the same colour as the others

#### Scenario: No limit
- **WHEN** no limit is set
- **THEN** no line is drawn

#### Scenario: Bars only
- **WHEN** the range holds day totals of 1200, 1800 and 900 ml and no limit is set
- **THEN** three bars are drawn in one colour with no reference line

#### Scenario: Day without entries
- **WHEN** one day in the range has no entry
- **THEN** no bar and no zero marker is drawn for that day

#### Scenario: Too little data
- **WHEN** the range holds fewer logged days than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Fluid intake figures
The graph view SHALL show, for the selected range, the average per day, the highest day and the number of days logged. The average SHALL be the sum of all entries divided by the days that have at least one entry (a day without entries is unknown, not zero), rounded to a whole ml; the highest day SHALL show its total and date. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds day totals of 1200, 1800 and 900 ml
- **THEN** the average is 1300 ml, the highest day is 1800 ml and the days logged are 3

#### Scenario: Unlogged days
- **WHEN** a range of seven days holds entries on only two days, 1000 and 2000 ml
- **THEN** the average is 1500 ml and the days logged are 2

#### Scenario: Empty range
- **WHEN** the range holds no entry
- **THEN** the view shows a plain empty text and no figures
