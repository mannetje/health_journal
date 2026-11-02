## ADDED Requirements

### Requirement: The Activity holds framework interactions only
`MainActivity` SHALL contain only what needs the Android framework: locale and window setup, lifecycle callbacks, Activity Result launchers, system intents and `recreate()`. Navigation state, settings state and the rules that decide which ViewModels refresh SHALL live in classes that hold no Activity and can be tested on the JVM.

#### Scenario: A navigation rule is tested
- **WHEN** a reminder notification is tapped while Mindfulness is open
- **THEN** a unit test of the navigation state SHALL show the pillbox open on Today and Mindfulness closed, without starting an Activity

#### Scenario: Behaviour is unchanged
- **WHEN** the person rotates the device, changes the language or restores a backup
- **THEN** the current tab, an open pillbox and any banner SHALL behave as before this change
