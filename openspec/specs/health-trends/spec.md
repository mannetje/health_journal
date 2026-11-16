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
Under a single-metric filter (weight, blood pressure, glucose, waist circumference, oxygen saturation, fluid or migraine) the selected range SHALL apply to both the chart and the entry list. Activity is not range-filtered. Under the ALL filter the full lists are shown.

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
When the selected History filter has a trend chart (Weight, Blood pressure, Glucose, Waist, Migraine, Oxygen saturation, Fluid), the History screen SHALL offer a control in the title row that switches between the entry list and the graph view. The graph view SHALL show the trend chart and its statistics and SHALL NOT show the entry list; the list view SHALL show the entries and SHALL NOT show the chart. The range selector SHALL be shown in both views, because the range limits the chart and the entries alike. The list SHALL be the default view. The chosen view SHALL be kept per filter while the app is open. Filters without a trend chart (All, Activity) SHALL show no control and the list. On a window 900 dp wide or more, where the list and the graph are shown side by side, the control SHALL NOT be shown. The control SHALL be at least 48 dp and its description SHALL name the view it switches to, in the app language.

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

### Requirement: Waist graph view
Under the Waist filter the History tab SHALL offer a switch between the entries and a graph view, kept per filter while the app is open, like the other chart filters. The list view SHALL be unchanged.

#### Scenario: Switch to the graph
- **WHEN** the Waist filter is selected and the graph icon is tapped
- **THEN** the waist graph view replaces the entries and the icon offers the list

### Requirement: Waist trend
The waist graph view SHALL show a line chart of the waist in cm over the selected date range. When the profile has a sex, the chart SHALL show a reference line at each NHG limit (men 94 and 102 cm, women 80 and 88 cm), equal to the limits of the NHG classification, and each point SHALL take the colour of its NHG band using the neutral band colours. Without a sex the chart SHALL show the line only.

#### Scenario: Limits for the profile sex
- **WHEN** the profile is female and the range holds two waist entries
- **THEN** reference lines are drawn at 80 and 88 cm

#### Scenario: No sex in the profile
- **WHEN** the profile has no sex
- **THEN** no reference lines are drawn and a note says the sex can be set in the profile

### Requirement: Waist statistics
The waist graph view SHALL show, over all waist entries and independent of the range, the lowest and highest waist with their dates (on a tie the newest), and the change of the newest waist against the waist at the start of the last month, three months, six months and year, in cm and in percent of the starting waist. When the history does not reach back that far, the change SHALL be a dash. Several entries on one day SHALL count as the last one of that day.

#### Scenario: Change over three months
- **WHEN** the newest waist is 90 cm and the waist three months before was 94 cm
- **THEN** the three-month row shows -4.0 cm and -4.3 %

#### Scenario: Short history
- **WHEN** the oldest entry is five weeks old
- **THEN** the six-month and year rows show a dash

### Requirement: Distance to the NHG limits
When the profile has a sex, the waist graph view SHALL show the newest waist minus the increased-risk limit and minus the high-risk limit in cm, with a plus sign above the limit. The wording SHALL contain figures only.

#### Scenario: Below and above the limits
- **WHEN** the profile is male and the newest waist is 98 cm
- **THEN** the distance to the first limit is +4.0 cm and to the second limit -4.0 cm

### Requirement: Waist-to-height ratio
When the profile has a height, the waist graph view SHALL show the waist divided by the height as a percentage, as a trend line over the selected range with a reference line at 50 %, and the newest value. The reference line SHALL be described as a rule of thumb and not as an NHG limit, and the card SHALL NOT colour values as good or bad. Without a height the card SHALL be replaced by a note that the height can be set in the profile.

#### Scenario: Ratio from height
- **WHEN** the height is 180 cm and the newest waist is 90 cm
- **THEN** the card shows 50.0 %

#### Scenario: No height in the profile
- **WHEN** the profile has no height
- **THEN** the ratio card is replaced by a note about the profile

### Requirement: Waist against weight
The waist graph view SHALL pair every waist entry in the selected range with the weight entry nearest in time within 3 days and show the pairs as a scatter chart with the weight across and the waist up. With at least 5 pairs the chart SHALL include a least-squares trend line and one sentence with the slope as cm of waist per weight unit, in the display unit of the weight. With fewer pairs, or when all weights are equal, it SHALL show a message that more entries with both values are needed.

#### Scenario: Slope sentence
- **WHEN** five pairs lie on a line that rises 0.8 cm of waist per kg
- **THEN** the sentence says about 0.8 cm of waist per kg

#### Scenario: Too few pairs
- **WHEN** only three waist entries have a weight within 3 days
- **THEN** the message asks for more entries with both values

### Requirement: Waist graphs in both languages
Every label, hint, note and content description of the waist graph view SHALL be available in English and Dutch and follow the app language, and SHALL be neutral: no advice, no warning, no good or bad colour.

#### Scenario: Dutch
- **WHEN** the app language is Dutch
- **THEN** the titles and notes of the waist cards are shown in Dutch

### Requirement: Glucose trend with NHG lines
The glucose trend chart SHALL draw fasting and post-meal readings as two lines over the selected date range, give each point the colour of its NHG category using the neutral category colours, and draw reference lines at the low limit (3.9 mmol/L), the fasting normal limit (6.0 mmol/L) and the post-meal normal limit (7.8 mmol/L), shown in the chosen glucose unit.

#### Scenario: Both contexts
- **WHEN** the range holds two fasting and two post-meal readings
- **THEN** two lines and three reference lines are drawn

### Requirement: Time in range per NHG category
The glucose graph view SHALL show, for fasting and for post-meal readings separately and for the selected range, the share and count of readings in each NHG category that has readings.

#### Scenario: Shares
- **WHEN** four fasting readings are normal and one is slightly raised
- **THEN** the fasting bars show 80 % (4) and 20 % (1)

### Requirement: Glucose by time of day
The glucose graph view SHALL group the readings of the selected range by the local hour into night (0-6), morning (6-12), afternoon (12-18) and evening (18-24) and show per group the average, lowest and highest reading and the count; a group without readings SHALL show a dash.

#### Scenario: Morning reading
- **WHEN** a reading was taken at 08:15
- **THEN** it counts in the morning group

### Requirement: Glucose statistics
Over all glucose entries and independent of the range, the glucose graph view SHALL show the lowest and highest reading with their dates (on a tie the newest), the change of the newest fasting reading and of the newest post-meal reading against the reading of the same context at the start of the last month, three months, six months and year (a dash when the history does not reach back that far), and per context the average, the sample standard deviation (a dash with fewer than two readings) and the range. Several readings on one day SHALL count as the last of that day for the change figures.

#### Scenario: Short history
- **WHEN** the oldest fasting reading is five weeks old
- **THEN** the fasting six-month and year changes are a dash

### Requirement: Last 30 days against the 30 before
The glucose graph view SHALL compare, per context, the average and the share of readings in the normal category of the 30 days ending at the newest reading with the 30 days before, with the change of both; a period without readings SHALL show a dash.

#### Scenario: Comparison
- **WHEN** the last 30 days average 6.0 and the 30 days before average 6.5 for fasting
- **THEN** the change shows -0.5

### Requirement: Largest rise between readings
The glucose graph view SHALL show the largest increase between two consecutive readings less than 3 hours apart, with the two values and the date, over all entries; without such a pair it SHALL show a dash.

#### Scenario: Rise
- **WHEN** readings of 5.0 at 12:00 and 8.4 at 13:30 are the largest rise
- **THEN** the card shows +3.4

### Requirement: Glucose graphs in both languages
Every label, hint, note and content description of the new glucose cards SHALL be available in English and Dutch, follow the app language, and be neutral: no advice, no warning, no good or bad colour.

#### Scenario: Dutch
- **WHEN** the app language is Dutch
- **THEN** the titles and notes of the glucose cards are shown in Dutch

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

### Requirement: Migraine graph view
The History graph section SHALL list Migraine next to the other metrics. Its view, range chips, charts, calendar and wording are defined in the `migraine` capability. The shared range rules (calendar-aligned range, empty range handling) and the neutral colour rule SHALL apply to it, and it SHALL use a single hue with no band, limit or comparison-with-previous-period figures.

#### Scenario: Metric picker
- **WHEN** the graph section is opened
- **THEN** Migraine can be chosen like Weight, Blood pressure, Glucose and Waist

#### Scenario: No bands or limits
- **WHEN** the Migraine view is shown
- **THEN** it contains no reference range, no limit line and no previous-period comparison

### Requirement: Oxygen saturation graph view
Under the Oxygen filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one dot per reading over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a value axis from the lowest reading minus 2 to 100, in a single neutral hue. It SHALL draw no band, reference line, limit or colour threshold.

#### Scenario: Dots only
- **WHEN** the range holds readings of 98, 95 and 91
- **THEN** three dots are drawn in one colour with no reference line

#### Scenario: Too little data
- **WHEN** the range holds fewer readings than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Oxygen saturation figures
The graph view SHALL show, for the selected range, the lowest, highest and average reading (average with one decimal), the number of readings, and the number of readings per context (readings without a context counted as "no context"). Site and the tick SHALL NOT change any figure or chart. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds 98, 96 and 94
- **THEN** the lowest is 94, the highest is 98, the average is 96.0 and the count is 3

#### Scenario: Empty range
- **WHEN** the range holds no reading
- **THEN** the view shows a plain empty text and no figures

#### Scenario: Per context
- **WHEN** two readings are "at rest", one is "after activity" and one has no context
- **THEN** the counts read 2, 1 and 1

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
