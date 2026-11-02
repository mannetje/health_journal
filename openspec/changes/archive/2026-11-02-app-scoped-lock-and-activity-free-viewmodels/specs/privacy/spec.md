## ADDED Requirements

### Requirement: Recreating the Activity does not lock the app
The app lock SHALL track the app being in the background, not the lifetime of an Activity. A rotation, a language or region change or any other recreation of the Activity SHALL NOT lock an unlocked app or start the lock timeout. A new process SHALL start locked.

#### Scenario: The language is changed while the app is unlocked
- **WHEN** the lock is on, the app is unlocked and the user changes the language
- **THEN** the screen is rebuilt in the new language and SHALL NOT ask for authentication again

#### Scenario: The app is started again after the process ended
- **WHEN** the lock is on and the app is started in a new process
- **THEN** the system SHALL show only the locked screen and the authentication prompt

#### Scenario: A resume without leaving the app
- **WHEN** the app was backgrounded and returned within the timeout, and later resumes again without having been backgrounded (a system dialog, a rotation)
- **THEN** the system SHALL NOT compare that resume with the earlier background time
