## ADDED Requirements

### Requirement: Temperature unit setting
A third device setting SHALL exist, persisted per device (not per profile) like the unit system and glucose unit: Temperature unit (System default, Celsius, Fahrenheit), default System default, shown in the Profile settings next to the other two with a short description. Changing it SHALL apply immediately to logging, history, trends and editing of body temperature and SHALL NOT change any stored data.

#### Scenario: Setting shown
- **WHEN** the Profile settings are opened
- **THEN** "Temperature unit" (Dutch "Temperatuureenheid") offers System default, Celsius (°C) and Fahrenheit (°F) beside the unit system and glucose unit

#### Scenario: Change does not touch data
- **WHEN** the setting changes from Celsius to Fahrenheit
- **THEN** History shows the same readings in °F and the stored values are unchanged

### Requirement: Temperature System default resolution
Temperature unit System default SHALL resolve by the active region: Fahrenheit for US, BS, BZ, KY, PW and LR, otherwise Celsius. Unknown or absent regions use Celsius.

#### Scenario: Dutch region
- **WHEN** the region is NL with the setting on System default
- **THEN** Celsius is used

#### Scenario: US region
- **WHEN** the region is US with the setting on System default
- **THEN** Fahrenheit is used

### Requirement: Temperature canonical storage and conversion
Body temperature SHALL be stored, validated in the domain, exported to CSV and written to backup in degrees Celsius as integer tenths, whatever the display unit. Conversion happens only at the UI boundary: displayed Fahrenheit = tenths / 10 x 9 / 5 + 32, rounded half up to one decimal; entered Fahrenheit is converted to tenths of Celsius as ((F - 32) x 5 / 9 x 10) rounded half up. Fahrenheit input SHALL have at most one decimal and the 30.0 to 45.0 °C range SHALL be applied to the converted value, so the accepted Fahrenheit range is 86.0 to 113.0. Displayed values use one decimal in the displayed unit, the region's decimal separator and the symbol °C or °F; a comma or point is accepted on input.

#### Scenario: Entering Fahrenheit
- **WHEN** the unit is Fahrenheit and 98.6 is entered
- **THEN** 370 tenths of Celsius is stored and it is shown as 98.6 °F

#### Scenario: Displaying in Fahrenheit
- **WHEN** a stored 368 is shown in Fahrenheit
- **THEN** it reads 98.2 °F

#### Scenario: Range on the converted value
- **WHEN** the unit is Fahrenheit and 86.0 and 113.0 are entered
- **THEN** both are accepted (300 and 450 tenths); 85.9 and 113.1 are refused

#### Scenario: Export in a Fahrenheit region
- **WHEN** a reading is exported in a US region with Fahrenheit selected
- **THEN** the CSV cell holds Celsius with one decimal (for example 37.0) and the backup holds 370

#### Scenario: Import unaffected
- **WHEN** a CSV file is imported with Fahrenheit selected
- **THEN** its values are read as Celsius
