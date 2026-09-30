## ADDED Requirements

### Requirement: Unit system preference
The system SHALL let the user choose a unit system (System, Metric, Imperial), independent of language and regional formats. System SHALL resolve to imperial only for devices whose region uses it, otherwise metric.

#### Scenario: Default follows the device
- **WHEN** the preference is System and the device region is the Netherlands
- **THEN** all values SHALL be shown in metric

#### Scenario: Override
- **WHEN** the user selects Imperial
- **THEN** weight, height and distance SHALL be shown and entered in lb, ft/in and mi

### Requirement: Metric storage
The system SHALL store all measurements in metric units regardless of the selected unit system.

#### Scenario: Entry in imperial
- **WHEN** the user logs 170 lb with Imperial selected
- **THEN** the stored weight SHALL be the equivalent kg value and domain validation SHALL apply to the kg value

#### Scenario: Round trip
- **WHEN** the user opens an entry for editing and saves without changing values
- **THEN** the stored metric value SHALL be unchanged

### Requirement: CSV stays metric
The system SHALL import and export CSV in metric units independent of the unit system.

## MODIFIED Requirements

### Requirement: History and charts
History cards, trend charts and stat chips SHALL present values in the selected unit system, using the region's number formatting.
