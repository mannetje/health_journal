# entry-management Specification

## Purpose
Defines how recorded entries (weight, blood pressure, glucose, waist circumference, activity) are listed, edited and deleted from the History screen.

## Requirements

### Requirement: History list
The History screen SHALL show the entries of the active profile as one merged list of all five entry types, sorted newest first (activity by start time). When a single-metric filter is selected (weight, blood pressure, glucose or waist circumference) the list SHALL be limited to that type and to the selected trend range (see health-trends). The activity filter and the ALL filter show every entry of the type(s) without range restriction.

#### Scenario: Merged order
- **WHEN** a weight at 10:00 and a glucose at 11:00 exist
- **THEN** the glucose entry is listed first

### Requirement: Update semantics
Updating an entry SHALL take the entry id, the owning profile and the new values (plus timestamp and the optional comment). The values SHALL be re-validated with the same rules as recording, and derived values (BMI for weight, category for blood pressure and glucose) SHALL be recomputed. The comment SHALL follow the rules of `entry-comments`; an empty comment removes the stored one. The entry keeps its id and profile. Updating an unknown id SHALL return "not found" and create nothing. An invalid update SHALL be rejected and leave the stored entry unchanged.

#### Scenario: Update recomputes BMI
- **WHEN** a weight entry is edited from 75 kg to 80 kg for a 180 cm profile
- **THEN** the same entry now has 80 kg and BMI 24.7

#### Scenario: Unknown id
- **WHEN** an update targets an id that does not exist
- **THEN** the result is "not found" and no entry is created

#### Scenario: Invalid update
- **WHEN** an update carries 800 kg
- **THEN** it is rejected and the stored entry is unchanged

#### Scenario: Comment edited and removed
- **WHEN** an entry with the comment "old" is edited to "new", and later saved with an empty comment
- **THEN** the entry shows "new" after the first edit and no comment after the second

### Requirement: Edit dialog rules
Each entry can be edited in a dialog that pre-fills the current values in the user's display units, including an editable comment field with the counter from `entry-comments`. Validation in the dialog:
- weight: 1 to 700 kg, else an "invalid value" message
- blood pressure: systolic 40 to 300, diastolic 20 to 200, systolic > diastolic, optional pulse 30 to 250
- waist circumference: 40 to 200 cm
- glucose: 0.5 to 55.0 mmol/L (after conversion from the display unit), else an "invalid value" message
- activity: duration greater than 0 minutes, distance 0 or more; start time is not editable
- comment: at most 200 characters, never a reason to block saving otherwise
Weight, blood pressure, glucose and waist circumference expose an editable date and time; activity does not.
If the update fails or the entry is gone, the dialog SHALL stay open and show an error ("Entry not found" when missing). On success the dialog closes and history and trend data refresh.

#### Scenario: Failed update keeps dialog
- **WHEN** the domain rejects the edited value
- **THEN** the dialog stays open showing the error

#### Scenario: Comment pre-filled
- **WHEN** an entry with the comment "new scale" is opened for editing
- **THEN** the comment field shows "new scale"

### Requirement: Untouched fields keep stored values
Each edit field is pre-filled with the stored value formatted for display: weight 2 decimals, glucose 1 decimal (mmol/L) or 0 decimals (mg/dL), distance 2 decimals, duration in minutes (seconds / 60) at 2 decimals; trailing zeros are stripped and the region's decimal separator is used. If a field's text equals its initial text when saved, the original stored value SHALL be saved, so display rounding never changes stored data. The comment is never reformatted for display.

#### Scenario: Round trip without change
- **WHEN** a glucose of 5.55 mmol/L is opened (shown as 5.6) and saved without editing
- **THEN** 5.55 mmol/L remains stored

#### Scenario: Comment round trip
- **WHEN** an entry with a comment is opened and saved without editing
- **THEN** the comment is unchanged

### Requirement: Delete
Deleting an entry SHALL require confirmation in a dialog and is permanent (no undo). Deleting an unknown id returns "not found" (shown as "Entry not found") and changes nothing; only the targeted entry is removed.

#### Scenario: Confirm delete
- **WHEN** the user confirms deletion of one weight entry
- **THEN** only that entry is removed and the list refreshes

#### Scenario: Cancel delete
- **WHEN** the user cancels the confirmation
- **THEN** nothing is removed
