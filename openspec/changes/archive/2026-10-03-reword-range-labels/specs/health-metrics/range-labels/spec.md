## ADDED Requirements

### Requirement: Label rule
A label shown next to a BMI, blood pressure or glucose value SHALL name the measurement band together with its range, in the form "name · range". It SHALL NOT name a medical condition (for example diabetes, prediabetes, hypertension, impaired, hypoglycaemia) and SHALL NOT contain advice, a warning or an instruction. The BMI class names Underweight, Normal, Overweight and Obese are allowed, always together with the BMI range.

#### Scenario: Blood pressure label
- **WHEN** a reading of 150/95 is shown in English
- **THEN** the label is "High blood pressure · from 140/90"

#### Scenario: Blood pressure label in Dutch
- **WHEN** a reading of 150/95 is shown in Dutch
- **THEN** the label is "Hoge bloeddruk · vanaf 140/90"

#### Scenario: BMI label with decimal comma
- **WHEN** a BMI of 27.0 is shown in Dutch
- **THEN** the label is "Overgewicht · BMI 25,0 tot 29,9"

#### Scenario: No condition names
- **WHEN** any label string in English or Dutch is reviewed
- **THEN** it contains none of: diabetes, prediabetes, hypertension, hypertensie, impaired, gestoord, hypoglycaemia, hypoglykemie

### Requirement: Source order
Thresholds SHALL follow NHG. Where Diabetes Fonds, DVN or Hartstichting differ from NHG, the NHG threshold SHALL be used. Those three sources MAY be used for plain-language wording. A source line SHALL credit NHG only for limits NHG states.

#### Scenario: Conflicting low glucose limit
- **WHEN** another source gives a different low glucose limit than the one in use and no NHG text supports a change
- **THEN** the limit stays at 3.5 mmol/L

### Requirement: Blood pressure bands
The blood pressure band SHALL be: SERIOUSLY_RAISED when systolic is at least 180 or diastolic is at least 110; else HIGH when systolic is at least 140 or diastolic is at least 90; else NORMAL. No other band SHALL exist.

#### Scenario: Boundaries
- **WHEN** readings are 139/89, 140/80, 120/90, 179/109, 180/80 and 120/110
- **THEN** the bands are NORMAL, HIGH, HIGH, HIGH, SERIOUSLY_RAISED and SERIOUSLY_RAISED

#### Scenario: Low reading
- **WHEN** a reading of 85/55 is shown
- **THEN** the band is NORMAL and no low band or alert is shown

### Requirement: Glucose bands and ranges
Glucose labels SHALL use the names Low, Normal, Slightly raised and High blood glucose (Dutch: Lage, Normale, Iets hogere and Hoge bloedsuiker). The label SHALL show only the range for the entry's context: fasting or after a meal ("na een maaltijd"). Ranges SHALL be shown in the display unit of the user, converted from the stored mmol/L value for display only and rounded to a whole number in mg/dL, while the band is always decided on the stored mmol/L value.

#### Scenario: Fasting label
- **WHEN** a fasting value of 6.5 mmol/L is shown in English with mmol/L
- **THEN** the label is "Slightly raised blood glucose · fasting above 6.0 to 6.9 mmol/L"

#### Scenario: After a meal label in mg/dL
- **WHEN** a value of 9.0 mmol/L after a meal is shown in mg/dL
- **THEN** the label is "Slightly raised blood glucose · after a meal 141 to 198 mg/dL"

#### Scenario: Band decided on stored value
- **WHEN** a fasting value of 6.95 mmol/L is stored and shown in mg/dL
- **THEN** the band is High, whatever the rounded mg/dL figure is

### Requirement: Neutral colours
Band colours SHALL come from one sequential ramp (lowest band lightest, highest darkest, with a lighter variant in dark mode), SHALL NOT use green, yellow, orange or red traffic-light hues, and SHALL meet a contrast of at least 4.5:1 for label text. The label text SHALL always be present, so colour is never the only signal.

#### Scenario: Highest blood pressure band
- **WHEN** a seriously raised reading is shown in either theme
- **THEN** it uses the darkest ramp step of that theme, no red, and its text is readable

### Requirement: Source and note reachable
Every screen that shows a label SHALL offer an "About these ranges" note that states the ranges follow published guidelines (mainly NHG, with wording also used by Diabetes Fonds, DVN and Hartstichting), that a label is not a diagnosis and the app is not a medical device, and that questions go to the doctor or pharmacist, with links to Thuisarts and the named sources. The note SHALL claim no endorsement and use no logos.

#### Scenario: Open the note
- **WHEN** the user taps the info control next to a label
- **THEN** the note is shown with the source links

### Requirement: Label layout
A label SHALL wrap to a second line instead of being truncated, at the default and large font sizes, in English and Dutch.

#### Scenario: Large font
- **WHEN** the system font scale is 1.3 and the language is Dutch
- **THEN** the full label is visible
