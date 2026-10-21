## ADDED Requirements

### Requirement: Lotus button in the top bar
The top app bar SHALL show a lotus button (emoji icon, with a content description in the app language) next to the pill button on every main tab, SHALL open the Mindfulness area on top of the current tab, and SHALL handle the system back action by returning to that tab.

#### Scenario: Back from Mindfulness
- **WHEN** the user presses the system back button on the first Mindfulness screen
- **THEN** the area closes and the previous tab is shown with its state

#### Scenario: Both buttons visible on a narrow phone
- **WHEN** the language is Dutch on a narrow phone
- **THEN** both the lotus and the pill button are visible and tappable

### Requirement: Vibration permission and screen on
The manifest SHALL declare `VIBRATE` for optional haptic cues, SHALL NOT declare any network permission, and the app SHALL keep the screen on only while an exercise or timer is running.

#### Scenario: No prompt for vibration
- **WHEN** the user turns haptic cues on
- **THEN** no permission prompt is shown

#### Scenario: Screen on only during an exercise
- **WHEN** the user leaves the exercise screen
- **THEN** the screen is no longer kept on by the app
