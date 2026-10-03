## ADDED Requirements

### Requirement: Follow system light/dark theme
The system SHALL render every screen using a color scheme that follows the device's system-level light/dark setting.

#### Scenario: Device set to dark mode
- **WHEN** the device's system setting is dark mode
- **THEN** the app SHALL render all screens with the dark color scheme (dark surfaces, light-on-dark text) instead of the light color scheme

#### Scenario: Device set to light mode
- **WHEN** the device's system setting is light mode
- **THEN** the app SHALL render all screens with the existing light color scheme

### Requirement: Preserve NHG category color semantics across themes
The system SHALL use the same category-to-hue mapping for NHG-derived status colors (BMI, blood pressure, glucose categories) in both light and dark theme, adjusted only for contrast/luminance.

#### Scenario: Category color recognizable in both themes
- **WHEN** a reading is classified into a given NHG category
- **THEN** the color shown for that category in dark mode SHALL be a luminance/contrast-adjusted variant of the same hue used in light mode, not a different hue
