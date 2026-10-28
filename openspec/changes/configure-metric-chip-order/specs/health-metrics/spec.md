## MODIFIED Requirements

### Requirement: Log screen metric tab order
The Log screen metric selector tabs SHALL follow the user-configured metric order stored in the `metric-display-order` preference (see `metric-display-order` capability). When no preference is saved the default order SHALL be used: Weight, Blood Pressure, Glucose, Migraine, Waist Circumference, Activity. The `PrimaryScrollableTabRow` SHALL derive its tab sequence by iterating the stored ordered list at render time, not from `MetricType.ordinal`.

#### Scenario: Tabs rendered in user-configured order
- **WHEN** the user has saved a custom metric order and opens the Log screen
- **THEN** the `PrimaryScrollableTabRow` SHALL render tabs in the saved order

#### Scenario: Tabs rendered in default order on first launch
- **WHEN** no metric order preference has been saved and the user opens the Log screen
- **THEN** the tabs SHALL appear as: Weight, Blood Pressure, Glucose, Migraine, Waist Circumference, Activity (followed by Migraine — but Migraine is already included in the default list)

#### Scenario: Default selected tab is the first in the configured order
- **WHEN** the Log screen is first opened with no prior metric selection
- **THEN** the first tab in the configured (or default) order SHALL be selected

### Requirement: History screen filter chip order
The History screen filter chips SHALL follow the same user-configured metric order as the Log screen. After the "All" chip, chips SHALL be rendered by iterating the stored ordered list, not by a hardcoded sequence.

#### Scenario: Filter chips rendered in user-configured order
- **WHEN** the user has saved a custom metric order and opens the History screen
- **THEN** the filter chip row SHALL render chips in the order: All, then the saved metric sequence

#### Scenario: Filter chips rendered in default order on first launch
- **WHEN** no metric order preference has been saved and the user opens the History screen
- **THEN** the chips SHALL appear as: All, Weight, Blood Pressure, Glucose, Migraine, Waist, Activity

