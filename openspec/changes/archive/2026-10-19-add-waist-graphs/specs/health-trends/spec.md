## ADDED Requirements

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
