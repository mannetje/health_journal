## ADDED Requirements

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
