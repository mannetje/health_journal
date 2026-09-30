## ADDED Requirements

### Requirement: Delete use cases
The system SHALL provide a delete use case for each of Weight, Blood Pressure, Glucose and Activity that removes one entry by its unique `MeasurementId`.

#### Scenario: Entry deleted
- **WHEN** a delete use case is invoked with the id of an existing entry
- **THEN** the repository SHALL remove exactly that entry and no other entry SHALL change

#### Scenario: Unknown id
- **WHEN** a delete use case is invoked with an id that does not exist
- **THEN** the system SHALL report a not-found result and SHALL NOT modify any data

### Requirement: Update use cases
The system SHALL provide an update use case for each of Weight, Blood Pressure, Glucose and Activity that replaces the values and timestamp of one existing entry, identified by its `MeasurementId`, keeping the same id and profile.

#### Scenario: Entry updated
- **WHEN** an update use case is invoked with the id of an existing entry and valid new values
- **THEN** the repository SHALL persist the new values under the same id
- **AND** derived data SHALL be recomputed (for weight, the BMI from the profile's height)

#### Scenario: Invalid values rejected
- **WHEN** an update use case is invoked with values that fail the existing domain validation (for example weight or blood-pressure range, or activity end before start)
- **THEN** the system SHALL reject the update and the stored entry SHALL remain unchanged

#### Scenario: Unknown id
- **WHEN** an update use case is invoked with an id that does not exist
- **THEN** the system SHALL report a not-found result and SHALL NOT create a new entry

### Requirement: Repository port
`HealthLogRepositoryPort` SHALL declare `updateX(entry)` and `deleteX(id: MeasurementId)` for each metric X, each returning whether a row was affected.

#### Scenario: Room adapter
- **WHEN** the Room adapter implements these methods
- **THEN** it SHALL use `@Update` and a `DELETE … WHERE id = :id` query on the existing tables
- **AND** the database schema and version SHALL NOT change

### Requirement: Delete confirmation
The system SHALL ask for confirmation, in the app language (Dutch or English), before permanently deleting an entry.

#### Scenario: User confirms
- **WHEN** the user chooses delete on an entry and confirms in the dialog
- **THEN** the entry SHALL be deleted and removed from the list and the trend chart

#### Scenario: User cancels
- **WHEN** the user dismisses the dialog or chooses cancel
- **THEN** no data SHALL change

### Requirement: Entry action icons
Each History entry card SHALL show an Edit icon button and a Delete icon button. The system SHALL NOT use swipe gestures for these actions. The buttons SHALL follow Android/Material guidelines: touch target of at least 48 dp, standard Edit and Delete icons, and localized (Dutch/English) content descriptions for accessibility services.

#### Scenario: Accessible actions
- **WHEN** a screen reader focuses an action button
- **THEN** it SHALL announce a localized description such as "Edit entry" / "Bewerk meting"

### Requirement: Edit entry
The system SHALL let the user open an entry from its Edit icon button on the History screen in an edit dialog, pre-filled with the entry's values and timestamp, reusing the Log screen's input fields and validation.

#### Scenario: Edit and save
- **WHEN** the user changes a value or the date/time and saves
- **THEN** the entry SHALL be updated and the list, statistics and trend chart SHALL show the new values immediately

#### Scenario: Validation feedback
- **WHEN** the user enters an invalid value
- **THEN** the dialog SHALL show the same localized validation message as the Log screen and SHALL NOT save

#### Scenario: Cancel
- **WHEN** the user cancels the edit dialog
- **THEN** the entry SHALL remain unchanged

### Requirement: Immediate refresh
After a successful update or delete, `HistoryViewModel` SHALL reload the history so that `HistoryUiState`, and therefore the entry list, the trend charts and the statistic chips, reflect the change without leaving the screen.

#### Scenario: Chart updates
- **WHEN** an entry is edited or deleted while a trend chart is shown
- **THEN** the chart and its statistics SHALL be recomputed from the new data, including the date-range window anchored to the newest remaining entry

## MODIFIED Requirements

### Requirement: History list entries
History entries were read-only cards. Each entry card SHALL now carry Edit and Delete icon buttons, for all four metrics and under the All filter.

#### Scenario: Edit under All
- **WHEN** the user taps the Edit icon of a weight, blood-pressure, glucose or activity entry under the All filter
- **THEN** the edit dialog for that entry's metric SHALL open
