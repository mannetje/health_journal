## ADDED Requirements

### Requirement: UI state is collected with the lifecycle
Screens SHALL collect a ViewModel's state with `collectAsStateWithLifecycle()`, so collection stops while the app is not visible and resumes when it is.

#### Scenario: The app goes to the background
- **WHEN** the app is no longer visible
- **THEN** screens SHALL stop collecting their state flows until the app is visible again

### Requirement: Cancellation is not a failure
Code that catches `Exception` around a suspending call SHALL rethrow `CancellationException` first, so a cancelled operation stops instead of being reported as an error.

#### Scenario: A backup is cancelled while it is being created
- **WHEN** the coroutine creating a backup is cancelled
- **THEN** the system SHALL NOT set a "could not create the backup" error
