## ADDED Requirements

### Requirement: Blood pressure includes heart rate (pulse)
The system SHALL support recording heart rate (pulse in bpm) alongside systolic and diastolic blood pressure readings.

#### Scenario: Blood pressure entry with pulse recorded
- **WHEN** a user records a blood pressure measurement with systolic, diastolic, and pulse values
- **THEN** the system persists all three values and includes pulse in history and CSV export/import.

### Requirement: Smart pre-fill fallback chain
The system SHALL initialize log screen input values using a fallback chain: latest entry -> profile-derived ideal -> standard default.

#### Scenario: Fallback to latest entry
- **WHEN** a user opens the log screen for a metric with previous entries
- **THEN** the initial picker position reflects the most recent measurement.

#### Scenario: Fallback to profile-derived default
- **WHEN** no previous entries exist and user profile data is present (e.g. height for weight, sex for waist)
- **THEN** initial values are calculated using personalized health ideals.

#### Scenario: Fallback to standard default
- **WHEN** neither previous entries nor profile data are available
- **THEN** standard defaults are used (Weight = 75.0 kg, BP = 120/80, Pulse = 70 bpm, Waist = 90 cm).

### Requirement: Interactive visual number pickers
The system SHALL provide horizontal ruler pickers for continuous metrics (weight, waist) and stacked scrolling number pickers for multi-value metrics (BP & pulse).

#### Scenario: Ruler picker interaction
- **WHEN** scrolling the weight or waist ruler picker
- **THEN** canvas ticks dynamically align with a fixed center indicator and snap smoothly to numerical values.

#### Scenario: Stacked BP and pulse picker interaction
- **WHEN** viewing the blood pressure and pulse input screen
- **THEN** three distinct horizontal scrolling rows (Systolic in Red, Diastolic in Blue, Pulse in Green) highlight selected values in colored bounding boxes.
