# theming Specification

## Purpose
Defines the light and dark visual themes and the brand top bar.

## Requirements

### Requirement: Theme choice
The application SHALL follow the system light/dark setting by default. The Profile screen SHALL offer a Theme setting with System, Light and Dark. The choice SHALL apply immediately without restarting the app, SHALL be remembered on the device, and SHALL also drive category colours and success and info cards, not only the Material surfaces.

#### Scenario: System switches to dark
- **WHEN** the theme is System and the device changes to dark mode
- **THEN** the dark palette is applied

#### Scenario: Forced dark on a light device
- **WHEN** the user picks Dark while the device is in light mode
- **THEN** the dark palette is applied everywhere, including category colours

#### Scenario: Choice is remembered
- **WHEN** the user picks Light and restarts the app
- **THEN** the light palette is applied and Light is shown as selected in Profile

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
