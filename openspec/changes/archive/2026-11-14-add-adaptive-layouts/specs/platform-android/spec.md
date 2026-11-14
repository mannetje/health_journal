## ADDED Requirements

### Requirement: Navigation and content adapt to the window
The application SHALL show its tab destinations in a bottom bar on a portrait phone and in a navigation rail when the window is at least 600 dp wide or less than 480 dp high. The destinations, their order, labels and selected state SHALL be identical in both. On windows wider than 840 dp the tab content SHALL be centred in a column no wider than 840 dp. The layout SHALL follow window size changes while the app is open without losing the entered values or the selected tab. The application SHALL NOT lock the orientation.

#### Scenario: Portrait phone
- **WHEN** the window is 411 dp wide and 891 dp high
- **THEN** the bottom bar is shown and no rail

#### Scenario: Landscape phone
- **WHEN** the phone is rotated so the window is 891 dp wide and 411 dp high
- **THEN** the rail is shown instead of the bottom bar and the form has the height that is left

#### Scenario: Tablet
- **WHEN** the window is 1280 dp wide
- **THEN** the rail is shown and the content is centred in a column of at most 840 dp

#### Scenario: Rotation keeps state
- **WHEN** the user has typed a value on the Log tab and rotates the device
- **THEN** the same tab and value are shown in the new layout
