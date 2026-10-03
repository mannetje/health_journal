# theming Specification

## Purpose
Defines the light and dark visual themes and the brand top bar.

## Requirements

### Requirement: System-driven theme
The application SHALL follow the system light/dark setting. There SHALL be no in-app theme switch.

#### Scenario: System switches to dark
- **WHEN** the device changes to dark mode
- **THEN** the dark palette is applied

### Requirement: Palette
The app SHALL use these colors (hex RGB):
- Light: primary 1E88E5, secondary 00897B, background F8F9FA, surface FFFFFF, text 212121
- Dark: primary 90CAF9, secondary 80CBC4, background 121212, surface 1E1E1E, text ECECEC
- Success container: light E8F5E9, dark 1B3A1E
- Info container: light E3F2FD, dark 152A3D
Category colors are defined in health-trends and keep the same hue identity in both themes.

#### Scenario: Dark surface
- **WHEN** dark mode is active
- **THEN** cards use surface 1E1E1E on background 121212

### Requirement: Brand top bar
The top bar SHALL use brand navy 0B1D3A in both themes with white title text and the app logo at the trailing end. The title names the current tab (Log, History, Profile).

#### Scenario: Top bar in light mode
- **WHEN** light mode is active
- **THEN** the top bar is 0B1D3A with a white title
