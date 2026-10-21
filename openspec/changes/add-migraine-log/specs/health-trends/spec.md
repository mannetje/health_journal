## ADDED Requirements

### Requirement: Migraine graph view
The History graph section SHALL list Migraine next to the other metrics. Its view, range chips, charts, calendar and wording are defined in the `migraine` capability. The shared range rules (calendar-aligned range, empty range handling) and the neutral colour rule SHALL apply to it, and it SHALL use a single hue with no band, limit or comparison-with-previous-period figures.

#### Scenario: Metric picker
- **WHEN** the graph section is opened
- **THEN** Migraine can be chosen like Weight, Blood pressure, Glucose and Waist

#### Scenario: No bands or limits
- **WHEN** the Migraine view is shown
- **THEN** it contains no reference range, no limit line and no previous-period comparison
