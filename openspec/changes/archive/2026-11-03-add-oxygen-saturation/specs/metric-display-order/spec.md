## MODIFIED Requirements

### Requirement: User-configurable metric display order
The system SHALL let the user set the order in which metrics appear on the Log and History screens. The order SHALL be stored in SharedPreferences and survive app restarts. The orderable metrics are every metric the app can log, including Migraine and Oxygen saturation.

#### Scenario: User saves a custom metric order
- **WHEN** the user moves a metric in the Profile tab's "Metric order" section
- **THEN** the new order SHALL be stored at once and the Log tabs and History chips SHALL show it without restarting the app

#### Scenario: Default order when nothing is saved
- **WHEN** no metric order has been saved
- **THEN** the order SHALL be: Weight, Blood Pressure, Glucose, Migraine, Waist, Activity, Oxygen

#### Scenario: Persisted order survives restart
- **WHEN** the user has saved a custom order and restarts the app
- **THEN** the Log and History screens SHALL show the saved order

#### Scenario: Stored value with unknown or repeated entries
- **WHEN** the stored value contains an unknown metric name or the same metric twice
- **THEN** the unknown entry SHALL be ignored, repeats SHALL be ignored after their first occurrence, and the remaining entries SHALL keep their order

#### Scenario: Metrics missing from the stored order
- **WHEN** a metric exists that the stored order does not mention (for example Oxygen saturation in an order saved before it existed)
- **THEN** it SHALL appear after the stored entries, in its position from the default order
