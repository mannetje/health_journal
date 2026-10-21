## ADDED Requirements

### Requirement: Sleep graph view
Under the Sleep filter the History tab SHALL offer the same entries/graph switch as the other chart filters. The graph view SHALL show one bar per night, placed on the day of waking, with the hours slept as its height, over the selected range (the same range chips, calendar-aligned axis, zoom and pan and minimum-data rule as other trends), on a value axis starting at 0 hours, in a single neutral hue. It SHALL draw no band, target line, limit or colour threshold. When two entries fall on the same day, their hours SHALL be added into one bar.

#### Scenario: Bars only
- **WHEN** the range holds nights of 6.5, 8 and 7 hours
- **THEN** three bars are drawn in one colour with no reference line

#### Scenario: Two entries on one day
- **WHEN** a night of 6 hours and a nap of 1 hour both wake on the same day
- **THEN** one bar of 7 hours is drawn and the figures count two entries

#### Scenario: Too little data
- **WHEN** the range holds fewer entries than the shared minimum
- **THEN** the usual "not enough data" text appears

### Requirement: Sleep figures
The graph view SHALL show, for the selected range, the average duration, the shortest, the longest, the number of nights logged (entries), the average bed time and the average wake time. Durations SHALL be shown as hours and minutes and the average rounded to the nearest minute. The average bed time and wake time SHALL be the circular mean of the clock times in the device time zone, so that 23:30 and 00:30 average to 00:00, shown as a clock time rounded to the nearest minute; when the clock times cancel out and no mean exists, the figure SHALL be shown as "-". Quality and the number of times woken SHALL NOT change any figure or chart. The figures SHALL be plain numbers with no label, colour or caption interpreting them. Every chart and figure group SHALL have a text description for screen readers with the same numbers.

#### Scenario: Figures
- **WHEN** the range holds nights of 6 h, 7 h and 8 h
- **THEN** the average is 7 h 0 min, the shortest 6 h 0 min, the longest 8 h 0 min and the nights logged 3

#### Scenario: Bed time across midnight
- **WHEN** the bed times are 23:30 and 00:30
- **THEN** the average bed time reads 00:00

#### Scenario: Empty range
- **WHEN** the range holds no entry
- **THEN** the view shows a plain empty text and no figures

#### Scenario: One night
- **WHEN** the range holds one entry
- **THEN** average, shortest and longest show that duration and the nights logged is 1
