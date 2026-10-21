## ADDED Requirements

### Requirement: Body temperature text in English and Dutch
All body temperature labels, sites, captions, chart descriptions, messages from the presentation logic and export or import messages SHALL exist in English and Dutch ("Body temperature" and "Lichaamstemperatuur"), follow the app language and region for dates, times and the decimal separator, and fit narrow phones and long Dutch words without clipping.

#### Scenario: Dutch form
- **WHEN** the app language is Dutch and the form is open
- **THEN** every label and site is Dutch and the value uses a decimal comma

#### Scenario: Temperature unit setting
- **WHEN** the Profile settings are shown in Dutch and in English
- **THEN** the setting reads "Temperatuureenheid" and "Temperature unit", with the choices "Systeemstandaard"/"System default", "Celsius (°C)" and "Fahrenheit (°F)", and its description is in the app language

#### Scenario: Narrow phone
- **WHEN** the screen is 320 dp wide in Dutch
- **THEN** the card, site choices and graph figures stay readable
