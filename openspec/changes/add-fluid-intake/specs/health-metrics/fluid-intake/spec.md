## ADDED Requirements

### Requirement: Record fluid intake
The system SHALL allow recording a fluid intake entry for a Profile at a given timestamp as a whole number of millilitres from 10 to 5000, with the optional comment. Every quick-add tap and every custom entry SHALL be one separate entry. This metric is optional: a Profile with no entries SHALL function identically to today. The system SHALL NOT record the type of drink.

#### Scenario: Valid custom entry
- **WHEN** an entry of 330 ml is recorded
- **THEN** it is stored and appears in History

#### Scenario: Out of range
- **WHEN** a value below 10 or above 5000, or a fraction such as 250.5, is entered
- **THEN** it is refused with a message in the app language and nothing is stored

#### Scenario: Two taps
- **WHEN** the same quick-add button is tapped twice
- **THEN** two entries are stored, each with its own timestamp

### Requirement: Quick add with own sizes
The Log card SHALL offer three quick-add buttons. A tap SHALL store one entry of that button's size at the current time without a further step, and show a short message with an Undo action that removes exactly that entry. The three sizes (10 to 5000 ml each) and their short names SHALL be editable by the person and kept in preferences. The defaults (150, 250 and 500 ml, named glass, cup and bottle) SHALL be stated as the app's own starting values and SHALL NOT be presented as a recommended amount. Changing a size SHALL NOT change stored entries.

#### Scenario: One tap
- **WHEN** the person taps "+1 cup" and the cup size is 250
- **THEN** an entry of 250 ml with the current time is stored and the Today line rises by 250

#### Scenario: Undo
- **WHEN** the person taps Undo in the message after a quick-add
- **THEN** that entry is deleted and the Today line returns to its earlier value

#### Scenario: Own size
- **WHEN** the person sets the glass to 120 ml
- **THEN** the next "+1 glass" stores 120 ml and earlier entries are unchanged

#### Scenario: Invalid size
- **WHEN** a size of 5 or 6000 is entered in the size settings
- **THEN** it is refused and the previous size is kept

### Requirement: Custom entry
The card SHALL offer a custom entry with the millilitres, the date and time (default now) and the optional comment from `entry-comments`. The comment SHALL be available on custom entries; quick-add entries SHALL be stored without a comment and MAY be given one later by editing in History.

#### Scenario: Custom with comment
- **WHEN** 400 ml with the comment "after the walk" is saved
- **THEN** History shows both

### Requirement: Daily total
The Log card SHALL show the total millilitres of the Profile's entries on the current calendar day of the device as a plain line ("Today: N ml"), reading "Today: 0 ml" when there are none; when the person has set a personal limit L the line SHALL read "Today: N of L ml". A day SHALL run from local midnight to local midnight. The total SHALL be shown only as numbers: no progress ring, bar fill, colour change, caption or comparison wording.

#### Scenario: Sum of the day
- **WHEN** entries of 250, 150 and 500 ml exist today and 300 ml yesterday
- **THEN** the line reads 900 ml

#### Scenario: Midnight
- **WHEN** the local date changes
- **THEN** the line shows only entries of the new day

#### Scenario: Edited timestamp
- **WHEN** an entry is edited to yesterday
- **THEN** the Today total drops by its amount

### Requirement: Personal limit set by the person
The person MAY set one daily limit in whole ml from 100 to 10000 per Profile, for example from a fluid restriction by their care team, labelled "Your limit (set by you or your care team)". The field SHALL be empty until the person sets it. The system SHALL NOT suggest, prefill, default, hint at or recommend a number. The limit SHALL be kept per Profile in the encrypted database, SHALL be changeable and clearable at any time, and changing or clearing it SHALL NOT alter any stored entry. When set it SHALL appear only as plain numbers in the Today line ("Today: 900 of 1500 ml") and as one thin dashed line in the graph's single hue. When the total is above the limit the card SHALL show the numbers (for example "1700 of 1500 ml") in the same style: no colour change, alert, notification, progress ring, and no wording such as over, exceeded, remaining or left.

#### Scenario: Limit empty by default
- **WHEN** the limit settings are opened on a new Profile
- **THEN** the field is empty and no example or default number is shown

#### Scenario: Limit set
- **WHEN** the person sets 1500 and 900 ml were drunk today
- **THEN** the Today line reads "Today: 900 of 1500 ml"

#### Scenario: Above the limit
- **WHEN** the total today is 1700 ml and the limit is 1500
- **THEN** the line reads "Today: 1700 of 1500 ml" in the same style, with no colour change, alert or wording

#### Scenario: Cleared
- **WHEN** the person clears the limit
- **THEN** the line reads "Today: N ml", the graph line disappears and no entry changes

#### Scenario: Invalid limit
- **WHEN** 50 or 20000 is entered
- **THEN** it is refused and the previous limit (or none) is kept

### Requirement: No target set by the app, no reminder or assessment
The system SHALL NOT set, suggest or compare to a daily target, minimum or maximum of fluid on its own; the only limit shown is the one the person entered. The system SHALL NOT show a progress indicator toward an amount; SHALL NOT send a reminder or notification to drink or stop drinking; and SHALL NOT label, classify, colour or comment on an amount or a day, including words such as enough, too little, too much, over, exceeded, remaining, left, dehydrated, good or bad in either language. Figures SHALL be plain numbers.

#### Scenario: Any amount
- **WHEN** one day totals 300 ml and another 4500 ml
- **THEN** both appear in the same style with no label, colour or message

#### Scenario: No reminder
- **WHEN** no entry is made all day
- **THEN** no notification is sent and the card shows nothing about it

#### Scenario: Wording check
- **WHEN** the fluid strings are scanned in English and Dutch
- **THEN** none contains an assessment word, an advice phrase or a number presented as a recommended or limit amount of the app; the person's own limit number shown back is allowed

### Requirement: Retrieve fluid intake history
The system SHALL allow retrieving all entries of a Profile in reverse chronological order and the total per local calendar day for a range.

#### Scenario: History returned
- **WHEN** a Profile has three entries
- **THEN** they are returned newest first

#### Scenario: Day totals
- **WHEN** a range holds entries on two days
- **THEN** two totals are returned, one per day, and days without entries are not returned

### Requirement: Fluid intake text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Drinken" for the metric, "Vandaag" for the daily total, "Jouw limiet (door jou of je zorgteam ingesteld)" for the limit, "van" in "Vandaag: 900 van 1500 ml", "Glas", "Kopje" and "Fles" for the default button names and "Ongedaan maken" for Undo; English SHALL use "Fluid intake", "Today", "Your limit (set by you or your care team)", "of", "Glass", "Cup", "Bottle" and "Undo".

#### Scenario: Dutch card
- **WHEN** the app language is Dutch and the limit is 1500
- **THEN** the card is titled "Drinken" and the total reads "Vandaag: 900 van 1500 ml"
