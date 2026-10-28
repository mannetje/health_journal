## ADDED Requirements

### Requirement: User-configurable metric display order
The system SHALL let the user set the order in which metrics appear on the Log and History screens. The order SHALL be stored in SharedPreferences and survive app restarts. The orderable metrics are every metric the app can log, including Migraine.

#### Scenario: User saves a custom metric order
- **WHEN** the user moves a metric in the Profile tab's "Metric order" section
- **THEN** the new order SHALL be stored at once and the Log tabs and History chips SHALL show it without restarting the app

#### Scenario: Default order when nothing is saved
- **WHEN** no metric order has been saved
- **THEN** the order SHALL be: Weight, Blood Pressure, Glucose, Migraine, Waist, Activity

#### Scenario: Persisted order survives restart
- **WHEN** the user has saved a custom order and restarts the app
- **THEN** the Log and History screens SHALL show the saved order

#### Scenario: Stored value with unknown or repeated entries
- **WHEN** the stored value contains an unknown metric name or the same metric twice
- **THEN** the unknown entry SHALL be ignored, repeats SHALL be ignored after their first occurrence, and the remaining entries SHALL keep their order

#### Scenario: Metrics missing from the stored order
- **WHEN** a metric exists that the stored order does not mention (for example one added in a later version)
- **THEN** it SHALL appear after the stored entries, in its position from the default order

### Requirement: Reorder list on the Profile tab
The Profile tab SHALL contain a "Metric order" section listing every orderable metric exactly once. The user SHALL be able to change the order by dragging a row and also by Move up and Move down buttons on each row. Every change SHALL be saved as soon as it is complete.

#### Scenario: Drag to reorder
- **WHEN** the user long-presses a row's drag handle and drags it to another position
- **THEN** the row SHALL follow the gesture and the new order SHALL be saved on release

#### Scenario: Reorder with buttons
- **WHEN** the user presses Move up or Move down on a row
- **THEN** the row SHALL swap places with its neighbour and the new order SHALL be saved

#### Scenario: Buttons at the ends of the list
- **WHEN** a row is first in the list, or last
- **THEN** its Move up button, or its Move down button, SHALL be disabled

#### Scenario: Screen reader announces the move
- **WHEN** a screen reader user moves a row with a button
- **THEN** the new position SHALL be announced (for example "Glucose, position 2 of 6"), and each button SHALL name its metric (for example "Move Glucose up")

#### Scenario: Touch targets
- **WHEN** the section is shown
- **THEN** every button and the drag handle SHALL have a touch target of at least 48 dp

#### Scenario: The section does not recommend
- **WHEN** the section is shown
- **THEN** its texts SHALL describe positions only and SHALL NOT say that any order is better or recommended

### Requirement: One order for Log and History
A single stored order SHALL govern the Log screen tabs and the History screen filter chips. There SHALL be no per-screen order.

#### Scenario: Same sequence on both screens
- **WHEN** the user has set a custom order
- **THEN** the sequence of metrics SHALL be the same on the Log tab bar and on the History chip row (after "All")

### Requirement: Changing the order does not change what is selected
Changing the order SHALL NOT change which Log tab or History filter is selected.

#### Scenario: Selected Log tab stays selected
- **WHEN** the user has a Log tab selected and then moves metrics on the Profile tab
- **THEN** the same metric SHALL still be selected when the user returns to the Log screen

### Requirement: The order is not part of the encrypted backup
The metric order SHALL NOT be written to or read from the encrypted backup.

#### Scenario: Restore on a new phone
- **WHEN** a backup is restored
- **THEN** the metric order SHALL be the default order until the user changes it
