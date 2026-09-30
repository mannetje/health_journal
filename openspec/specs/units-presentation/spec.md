# units-presentation Specification

## Purpose
Defines how canonical metric data is presented and entered in the user's preferred units. Storage, domain rules and CSV are always metric; conversion happens only at the UI boundary.

## Requirements

### Requirement: Canonical storage
All persisted and exchanged values SHALL be canonical: weight in kg, height in cm, glucose in mmol/L, distance in meters, blood pressure in mmHg. Display units SHALL never change stored data.

#### Scenario: Imperial entry stored metric
- **WHEN** the user logs 165 lb
- **THEN** 74.84 kg is stored

### Requirement: Conversion constants
Conversions SHALL use: 1 lb = 0.45359237 kg; 1 mi = 1.609344 km; 1 in = 2.54 cm; 12 in = 1 ft; glucose mg/dL = mmol/L / 0.0555. Weight entered in lb is converted to kg and rounded to 2 decimals (half up). Distance is entered in km or mi and stored in meters (km x 1000, miles converted through km).

#### Scenario: Miles stored as meters
- **WHEN** 1 mi is logged
- **THEN** 1609.344 m is stored

### Requirement: Height in feet and inches
Height in imperial mode SHALL be shown as feet and inches: total inches = round(cm / 2.54) (half up), feet = total inches div 12, inches = total inches mod 12. Entered feet and inches convert to cm as round((ft x 12 + in) x 2.54). The conversion applies only when the user edits the height; untouched height is never rewritten.

#### Scenario: 180 cm
- **WHEN** height 180 cm is displayed in imperial mode
- **THEN** it reads 5 ft 11 in (71 inches)

### Requirement: Display precision
Displayed values SHALL be rounded to: weight 1 decimal; distance 2 decimals; glucose 1 decimal in mmol/L and 0 decimals in mg/dL; duration whole minutes (round(seconds / 60)); BMI 1 decimal. Symbols: kg or lb; km or mi; mmol/L or mg/dL.

#### Scenario: Glucose display
- **WHEN** 5.55 mmol/L is displayed in mg/dL
- **THEN** it reads 100 mg/dL

### Requirement: User choices
Two independent settings SHALL exist, persisted per device (not per profile): unit system (System default, Metric, Imperial) and glucose unit (System default, mmol/L, mg/dL). Default for both is System default. Changing either applies to logging, history, trends and editing immediately. The glucose unit chips on the Log screen SHALL write the same setting as the Profile/settings screen.

#### Scenario: Setting shared
- **WHEN** the user selects mg/dL on the Log screen
- **THEN** the same choice is shown in settings

### Requirement: System default resolution
Unit system System default SHALL follow the active region: imperial for US, GB, LR and MM, otherwise metric. Glucose System default SHALL use mg/dL for the regions US, JP, DE, FR, ES, IT, AT, BE, PT, GR, IN, BR, AR, CL, CO, MX, EG, IL, SA, KR, TW, TR and mmol/L for all others. Unknown or absent regions use metric and mmol/L.

#### Scenario: Dutch region
- **WHEN** the region is NL with both settings on System default
- **THEN** metric units and mmol/L are used

#### Scenario: US region
- **WHEN** the region is US
- **THEN** imperial units and mg/dL are used

### Requirement: Decimal input
Numeric input SHALL accept either a comma or a point as decimal separator (input is trimmed and comma replaced by point before parsing). Displayed numbers use the active region's separator.

#### Scenario: Comma accepted
- **WHEN** the user types "72,5" for weight
- **THEN** it is read as 72.5

### Requirement: Logging validation messages
Blank, non-numeric or out-of-range input in the Log screen SHALL show a localized error specific to the field (weight, systolic, diastolic, glucose, duration, distance). Domain range errors that pass through are shown as English text. With no active profile the record buttons are disabled and a message asks to create a profile first.

#### Scenario: No profile
- **WHEN** no active profile exists
- **THEN** recording is disabled and the "create a profile first" message is shown
