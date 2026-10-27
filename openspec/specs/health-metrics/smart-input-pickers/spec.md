# health-metrics/smart-input-pickers Specification

## Purpose
Defines how values are entered on the Log screen: pulse recorded with blood pressure, a starting position taken from the latest entry only, and scrolling pickers for weight, waist, blood pressure and pulse, in English and Dutch.

## Requirements

### Requirement: Blood pressure includes heart rate (pulse)
The system SHALL support recording heart rate (pulse in bpm) alongside systolic and diastolic blood pressure readings.

#### Scenario: Blood pressure entry with pulse recorded
- **WHEN** a user records a blood pressure measurement with systolic, diastolic, and pulse values
- **THEN** the system persists all three values and includes pulse in history and CSV export/import.

### Requirement: Pickers start from the person's own entries
The system SHALL start a log picker at the person's latest entry for that metric, and SHALL NOT derive a starting value from the profile or treat an untouched starting position as a recorded value.

#### Scenario: Fallback to latest entry
- **WHEN** a user opens the log screen for a metric with previous entries
- **THEN** the initial picker position reflects the most recent measurement.

#### Scenario: No earlier entry
- **WHEN** no previous entry exists for a metric, whatever the profile holds
- **THEN** the picker shows a plain starting position (Weight = 75.0 kg, BP = 120/80, Pulse = 70 bpm, Waist = 90 cm) and the glucose field is empty; none of these is a target.

#### Scenario: A starting position is not recorded
- **WHEN** the person has not moved the picker or typed a value
- **THEN** no category is shown for it, Save is off with a short hint, and an untouched pulse is saved as no pulse.

#### Scenario: Repeating the last value
- **WHEN** a latest entry exists
- **THEN** its value counts as chosen and can be recorded again with one tap.

### Requirement: Interactive visual number pickers
The system SHALL provide horizontal ruler pickers for continuous metrics (weight, waist) and stacked scrolling number pickers for multi-value metrics (BP & pulse).

#### Scenario: Ruler picker interaction
- **WHEN** scrolling the weight or waist ruler picker
- **THEN** canvas ticks dynamically align with a fixed center indicator and snap smoothly to numerical values.

#### Scenario: Stacked BP and pulse picker interaction
- **WHEN** viewing the blood pressure and pulse input screen
- **THEN** three distinct horizontal scrolling rows (Systolic in Red, Diastolic in Blue, Pulse in Green) highlight selected values in colored bounding boxes.

### Requirement: Internationalization and design guideline adherence
The system SHALL provide English and Dutch localization for all UI components and adhere strictly to project guidelines (Hexagonal architecture, Material 3, offline-first, and accessibility standards).

#### Scenario: Bilingual localization displayed
- **WHEN** switching between English and Dutch in app settings
- **THEN** all picker labels, unit indicators, and category descriptions update immediately to the selected language.

#### Scenario: Accessibility and touch targets
- **WHEN** interacting with the pickers using TalkBack or touch
- **THEN** touch targets meet the 48dp minimum and semantic descriptions accurately state the selected values.
