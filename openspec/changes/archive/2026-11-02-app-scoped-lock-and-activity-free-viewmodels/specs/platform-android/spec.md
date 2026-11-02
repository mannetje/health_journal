## ADDED Requirements

### Requirement: ViewModels hold no Activity
A ViewModel SHALL NOT hold a reference to an Activity or to a view, directly or through a lambda, because it outlives the Activity that created it. What a ViewModel needs from the platform SHALL come from the application's own objects, and what it needs the screen to do SHALL leave it as an event the screen collects.

#### Scenario: A restore finishes after the Activity was recreated
- **WHEN** the screen was rotated or recreated after a restore was started and the restore then finishes
- **THEN** the current Activity SHALL receive the result and refresh its screens, and the first Activity SHALL NOT be used

#### Scenario: A result arrives while the app is in the background
- **WHEN** a restore finishes while the app is not visible
- **THEN** the result SHALL be delivered when the app is visible again and SHALL NOT be lost
