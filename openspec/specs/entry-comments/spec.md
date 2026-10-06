# entry-comments Specification

## Purpose
Every health entry (weight, blood pressure, glucose, waist circumference, activity) can carry an optional single-line comment of at most 200 characters. It is entered while logging or editing, shown in History, stored with the entry and included in CSV export and import.

## Requirements

### Requirement: Optional comment on every entry
Weight, blood pressure, glucose, waist circumference and activity entries SHALL each accept an optional comment, given when the entry is recorded or later when it is edited. An entry without a comment SHALL behave exactly as before.

#### Scenario: Record with a comment
- **WHEN** a weight of 80.0 kg is saved with the comment "after a run"
- **THEN** the stored entry has that comment

#### Scenario: Record without a comment
- **WHEN** a glucose entry is saved with an empty comment field
- **THEN** the entry has no comment and nothing else differs

#### Scenario: Every entry type
- **WHEN** a comment is added to a weight, a blood pressure, a glucose, a waist and an activity entry
- **THEN** each of them stores and returns it

### Requirement: Length limit and normalisation
A comment SHALL be at most 200 characters after trimming. Leading and trailing whitespace SHALL be removed, a blank comment SHALL be stored as no comment, and line breaks SHALL be replaced by a single space. The domain SHALL reject a comment over the limit. The input field SHALL stop accepting text at 200 characters and show the count (for example "37/200").

#### Scenario: Blank is no comment
- **WHEN** the comment is three spaces
- **THEN** the entry has no comment

#### Scenario: Trimmed
- **WHEN** the comment is "  felt dizzy  "
- **THEN** the stored comment is "felt dizzy"

#### Scenario: Line break
- **WHEN** the comment contains a line break between two words
- **THEN** it is stored with a single space instead

#### Scenario: Limit reached
- **WHEN** the user types a 201st character
- **THEN** it is not accepted and the counter shows 200/200

#### Scenario: Over the limit in the domain
- **WHEN** a use case receives a 201-character comment
- **THEN** it is rejected and nothing is stored

### Requirement: Comment is stored as typed
The comment SHALL be stored as plain text exactly as entered after normalisation, independent of the app language and region. It SHALL NOT be translated, interpreted or used in classification.

#### Scenario: Language independence
- **WHEN** a comment is saved in Dutch and the app language is then changed to English
- **THEN** the comment is shown unchanged

### Requirement: Comment in the logging form
Each metric tab of the logging screen SHALL offer a single-line "Comment (optional)" field with the live counter, which is cleared after a successful save. The comment SHALL NOT be required to enable the save button.

#### Scenario: Save without touching the field
- **WHEN** a valid value is entered and the comment is left empty
- **THEN** saving is enabled and succeeds

#### Scenario: Field cleared after save
- **WHEN** an entry with a comment is saved
- **THEN** the comment field is empty for the next entry

### Requirement: Comment in History
History SHALL show an entry's comment below its value, in a secondary text style, limited to two lines with an ellipsis when longer. Entries without a comment SHALL show no extra line or gap. The full comment SHALL be visible in the edit dialog. The layout SHALL NOT truncate other row content at the large font size in English or Dutch.

#### Scenario: Comment shown
- **WHEN** an entry has the comment "new scale"
- **THEN** the list row shows "new scale" below the value

#### Scenario: Long comment
- **WHEN** a comment is 200 characters long
- **THEN** the row shows two lines ending in an ellipsis and the edit dialog shows all of it

#### Scenario: No comment
- **WHEN** an entry has no comment
- **THEN** its row looks as it did before this change
