# entry-management Specification

## Purpose
Defines how recorded entries (weight, blood pressure, glucose, activity) are listed, edited and deleted from the History screen.

## Requirements

### Requirement: History list
The History screen SHALL show the entries of the active profile as one merged list of all four entry types, sorted newest first (activity by start time). When a single-metric filter is selected (weight, blood pressure or glucose) the list SHALL be limited to that type and to the selected trend range (see health-trends). The activity filter and the ALL filter show every entry of the type(s) without range restriction.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a glucose at 11:00 exist
- **THEN** the glucose entry is listed first

### Requirement: Update semantics
Updating an entry SHALL take the entry id, the owning profile and the new values (plus timestamp). The values SHALL be re-validated with the same rules as recording, and derived values (BMI for weight, category for blood pressure and glucose) SHALL be recomputed. The entry keeps its id and profile. Updating an unknown id SHALL return "not found" and create nothing. An invalid update SHALL be rejected and leave the stored entry unchanged.

#### Scenario: Update recomputes BMI
- **WHEN** a weight entry is edited from 75 kg to 80 kg for a 180 cm profile
- **THEN** the same entry now has 80 kg and BMI 24.7

#### Scenario: Unknown id
- **WHEN** an update targets an id that does not exist
- **THEN** the result is "not found" and no entry is created

#### Scenario: Invalid update
- **WHEN** an update carries 800 kg
- **THEN** it is rejected and the stored entry is unchanged

### Requirement: Edit dialog rules
Each entry can be edited in a dialog that pre-fills the current values in the user's display units. Validation in the dialog:
- weight: 1 to 700 kg, else an "invalid value" message
- blood pressure: systolic 40 to 300, diastolic 20 to 200, systolic > diastolic
- glucose: value greater than 0 in the dialog; the domain range (0.5 to 55.0 mmol/L) is enforced afterwards and its error message is shown
- activity: duration greater than 0 minutes, distance 0 or more; start time is not editable
Weight, blood pressure and glucose expose an editable date and time; activity does not.
If the update fails or the entry is gone, the dialog SHALL stay open and show an error ("Entry not found" when missing). On success the dialog closes and history and trend data refresh.

#### Scenario: Failed update keeps dialog
- **WHEN** the domain rejects the edited value
- **THEN** the dialog stays open showing the error

### Requirement: Untouched fields keep stored values
Each edit field is pre-filled with the stored value formatted for display: weight 2 decimals, glucose 1 decimal (mmol/L) or 0 decimals (mg/dL), distance 2 decimals, duration in minutes (seconds / 60) at 2 decimals; trailing zeros are stripped and the region's decimal separator is used. If a field's text equals its initial text when saved, the original stored value SHALL be saved, so display rounding never changes stored data.

#### Scenario: Round trip without change
- **WHEN** a glucose of 5.55 mmol/L is opened (shown as 5.6) and saved without editing
- **THEN** 5.55 mmol/L remains stored

### Requirement: Delete
Deleting an entry SHALL require confirmation in a dialog and is permanent (no undo). Deleting an unknown id returns "not found" (shown as "Entry not found") and changes nothing; only the targeted entry is removed.

#### Scenario: Confirm delete
- **WHEN** the user confirms deletion of one weight entry
- **THEN** only that entry is removed and the list refreshes

#### Scenario: Cancel delete
- **WHEN** the user cancels the confirmation
- **THEN** nothing is removed
