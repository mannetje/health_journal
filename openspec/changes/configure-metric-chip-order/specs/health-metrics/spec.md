## MODIFIED Requirements

### Requirement: Log screen metric tab order
The Log screen metric tabs SHALL follow the stored metric order (see the `metric-display-order` capability), Migraine included. When no order is saved the default order SHALL be used: Weight, Blood Pressure, Glucose, Migraine, Waist, Activity. The `PrimaryScrollableTabRow` SHALL build its tabs and its selected index from the ordered list at render time and SHALL NOT use `MetricType.ordinal`.

#### Scenario: Tabs in the saved order
- **WHEN** the user has saved a custom order and opens the Log screen
- **THEN** the tabs SHALL appear in the saved order

#### Scenario: Tabs in the default order
- **WHEN** no order is saved and the user opens the Log screen
- **THEN** the tabs SHALL appear as Weight, Blood Pressure, Glucose, Migraine, Waist, Activity

#### Scenario: First metric of the order is selected on a fresh launch
- **WHEN** the app starts and the Log screen is shown for the first time
- **THEN** the first metric of the order SHALL be selected, including when that metric is Migraine

#### Scenario: Selection stays when the order changes
- **WHEN** the user changes the order while a Log tab is selected
- **THEN** the same metric SHALL stay selected and only its position in the tab row changes

### Requirement: History screen filter chip order
The History screen filter chips SHALL follow the same stored order. The "All" chip SHALL come first and the metric chips SHALL follow in the stored order.

#### Scenario: Chips in the saved order
- **WHEN** the user has saved a custom order and opens the History screen
- **THEN** the chips SHALL appear as All, then the metrics in the saved order

#### Scenario: Chips in the default order
- **WHEN** no order is saved and the user opens the History screen
- **THEN** the chips SHALL appear as All, Weight, Blood Pressure, Glucose, Migraine, Waist, Activity

#### Scenario: Import dialog chips are unaffected
- **WHEN** the user opens the History import dialog
- **THEN** its file-format chips SHALL keep their fixed order
