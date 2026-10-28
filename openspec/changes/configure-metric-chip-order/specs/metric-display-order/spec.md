## ADDED Requirements

### Requirement: User-configurable metric display order
The system SHALL allow the user to configure the order in which metric types appear across the Log and History screens. The configured order SHALL be persisted in SharedPreferences and survive app restarts.

#### Scenario: User saves a custom metric order
- **WHEN** the user reorders metrics in the Settings screen and leaves the screen
- **THEN** the new order SHALL be persisted and reflected immediately on the Log and History screens

#### Scenario: Default order on first launch
- **WHEN** no metric order preference has been saved
- **THEN** the system SHALL use the default order: Weight, Blood Pressure, Glucose, Migraine, Waist, Activity

#### Scenario: Persisted order survives restart
- **WHEN** the user has saved a custom order and restarts the app
- **THEN** the Log and History screens SHALL display metrics in the previously saved order

### Requirement: Settings screen reorderable metric list
The Settings screen SHALL contain a drag-to-reorder list of all configurable metric types. The user SHALL be able to drag items up and down to change their position. Changes SHALL be saved automatically when the user finishes dragging an item.

#### Scenario: Drag to reorder
- **WHEN** the user long-presses or drags a metric item in the Settings reorder list
- **THEN** the item SHALL follow the drag gesture and be inserted at the new position on release

#### Scenario: All configurable metrics are present
- **WHEN** the reorder list is displayed
- **THEN** it SHALL contain exactly these metric types: Weight, Blood Pressure, Glucose, Migraine, Waist Circumference, Activity (all six, no duplicates, none omitted)

### Requirement: Shared order across Log and History
A single stored preference SHALL govern the metric sequence on both the Log screen (tabs) and the History screen (filter chips). There SHALL be no separate per-screen order setting.

#### Scenario: Order parity between Log tabs and History chips
- **WHEN** the user has configured a custom metric order
- **THEN** the relative sequence of metrics SHALL be identical on the Log tab bar and the History chip row

