## ADDED Requirements

### Requirement: Fluid intake graph view with Graph and Calendar
Under the Fluid filter the History tab SHALL offer the same entries/graph switch as the other chart filters, and the graph view SHALL offer a Graph and Calendar toggle and the range chips week, month, 3 months, 6 months and year (the same chips and range rules as other trends), as the Migraine view does. All figures SHALL be computed by pure functions in the domain layer for the active profile and range. The Graph view SHALL show, in this order: summary figures; a daily-total chart; a weekday chart; a time-of-day chart; a drinks chart. An empty range SHALL show a plain empty text and no charts. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

**Daily-total chart.** One bar per local calendar day with the day's total in ml over the selected range (calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a value axis starting at 0, in a single neutral hue. Days without entries SHALL show no bar. When the person has set a minimum, a maximum or both it SHALL draw one thin dashed horizontal line per number set, at that number in the same hue, with the number as plain text; bars SHALL NOT change colour above or below a line. It SHALL draw no other goal line, reference line, band, progress fill or colour threshold, and the current day SHALL be drawn like any other day.

**Weekday chart.** Seven bars, one per weekday starting on the locale's first day of the week, each the average total of the logged days of that weekday in the range (whole ml), labelled with those numbers only; a weekday without a logged day shows no bar.

**Drinks chart.** One bar per drink that has at least one entry in the range, showing the total ml of that drink in the range, sorted by amount and then by the fixed order of the list, labelled with those numbers only (no percentage, no share headline). Every bar uses the same single hue. It SHALL NOT rank, praise or warn about a drink.

**Time-of-day chart.** Eight bars for the three-hour blocks of the day (00 to 03 up to 21 to 24), each the average ml drunk in that block per logged day in the range (whole ml), labelled with those numbers only. It shows when entries were made and says nothing about when to drink.

#### Scenario: Maximum line
- **WHEN** a maximum of 1500 is set, no minimum, and a day totals 1800 ml
- **THEN** one dashed line is drawn at 1500 and the 1800 bar has the same colour as the others

#### Scenario: Two lines
- **WHEN** a minimum of 1400 and a maximum of 2000 are set
- **THEN** two dashed lines are drawn, at 1400 and at 2000, each with its number, and all bars keep one colour

#### Scenario: No numbers set
- **WHEN** neither a minimum nor a maximum is set
- **THEN** no line is drawn

#### Scenario: Bars only
- **WHEN** the range holds day totals of 1200, 1800 and 900 ml and no minimum or maximum is set
- **THEN** three bars are drawn in one colour with no reference line

#### Scenario: Day without entries
- **WHEN** one day in the range has no entry
- **THEN** no bar and no zero marker is drawn for that day

#### Scenario: Weekday averages
- **WHEN** the range holds two Mondays with 1000 and 2000 ml
- **THEN** the Monday bar is labelled 1500 ml

#### Scenario: Drinks chart
- **WHEN** the range holds 1000 ml of Water, 750 ml of Coffee and 250 ml of Tea
- **THEN** three bars are drawn in one colour labelled 1000, 750 and 250 ml, with no percentage or comment

#### Scenario: Time of day
- **WHEN** the range holds one logged day with 250 ml at 08:10 and 500 ml at 08:50
- **THEN** the 06 to 09 bar is labelled 750 ml and the others without entries show no bar

#### Scenario: Too little data
- **WHEN** the range holds fewer logged days than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Fluid intake figures
The graph view SHALL show, for the selected range, the average per day, the highest day and the number of days logged. The average SHALL be the sum of all entries divided by the days that have at least one entry (a day without entries is unknown, not zero), rounded to a whole ml; the highest day SHALL show its total and date. When the person has set a minimum, a maximum or both, the figures SHALL also show how many logged days were below the minimum, within the limits and above the maximum, as plain counts of the numbers that are set. The figures SHALL be plain numbers with no caption interpreting them.

#### Scenario: Figures
- **WHEN** the range holds day totals of 1200, 1800 and 900 ml
- **THEN** the average is 1300 ml, the highest day is 1800 ml and the days logged are 3

#### Scenario: Unlogged days
- **WHEN** a range of seven days holds entries on only two days, 1000 and 2000 ml
- **THEN** the average is 1500 ml and the days logged are 2

#### Scenario: Counts against the person's numbers
- **WHEN** the minimum is 1400, the maximum is 2000 and the day totals are 900, 1700 and 2300 ml
- **THEN** the counts read 1 day below the minimum, 1 day within the limits and 1 day above the maximum

#### Scenario: Empty range
- **WHEN** the range holds no entry
- **THEN** the view shows a plain empty text and no figures

### Requirement: Fluid intake calendar
The Calendar toggle SHALL show a month grid, starting on the locale's first day of the week, in which every day with at least one entry shows its total in ml, so the intake of each day can be read at a glance. Day cells SHALL be shaded in a single hue in steps in proportion to the day's total against the highest day of the shown month, with the number always printed, so shade is never the only signal. When the person has set a minimum, a maximum or both, each logged day SHALL also carry one small shape marker for its position (below the minimum, within the limits, above the maximum), with a legend and a text description giving the words; the marker uses the same hue and no warning colour. Days without entries SHALL be empty, not zero. Previous and next buttons SHALL move by month and not past the current month. Tapping a day with entries SHALL open History filtered to Fluid and that day, showing the day-total row and the entries. The current day SHALL be drawn like any other day.

#### Scenario: Month overview
- **WHEN** a month holds entries on 12 days
- **THEN** those 12 days show their totals and the other days are empty

#### Scenario: Shade is not the only signal
- **WHEN** a day with 1800 ml is shown in either theme
- **THEN** the cell shows "1800" and the shade is only an addition

#### Scenario: Position marker
- **WHEN** the minimum is 1400 and a day totals 900 ml
- **THEN** that cell carries the "below the minimum" marker and its description says so in words

#### Scenario: No numbers set
- **WHEN** neither a minimum nor a maximum is set
- **THEN** no position marker and no legend is shown

#### Scenario: Open a day
- **WHEN** a day with three entries is tapped
- **THEN** History opens filtered to Fluid and that day, showing the day-total row and the three entries

#### Scenario: Future months
- **WHEN** the current month is shown
- **THEN** the next button is disabled
