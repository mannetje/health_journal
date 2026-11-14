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
The Log card SHALL show the total millilitres of the Profile's entries on the current calendar day of the device as a plain line ("Today: N ml"), reading "Today: 0 ml" when there are none. A day SHALL run from local midnight to local midnight. When the person has set a minimum, a maximum or both, the line SHALL also show those numbers and the position label of the requirement "Personal minimum and maximum set by the person". The total SHALL be shown only as numbers and that label: no progress ring, bar fill, percentage or caption.

#### Scenario: Sum of the day
- **WHEN** entries of 250, 150 and 500 ml exist today and 300 ml yesterday
- **THEN** the line reads 900 ml

#### Scenario: Midnight
- **WHEN** the local date changes
- **THEN** the line shows only entries of the new day

#### Scenario: Edited timestamp
- **WHEN** an entry is edited to yesterday
- **THEN** the Today total drops by its amount

### Requirement: Personal minimum and maximum set by the person
The person MAY set a daily minimum, a daily maximum, both or neither, per Profile, each as a whole number of ml from 100 to 10000. When both are set the minimum SHALL NOT be above the maximum. The numbers are the person's own, for example from the huisarts, a care team or a fluid restriction leaflet; some people may not drink more than a set amount because of a condition, and some people need a minimum intake, so the two are independent. The fields SHALL be empty until the person sets them. The system SHALL NOT suggest, prefill, default, hint at, give an example of or recommend a number, and SHALL NOT offer a button that fills in a published amount. The numbers SHALL be kept per Profile in the encrypted database, SHALL be changeable and clearable independently at any time, and changing or clearing them SHALL NOT alter any stored entry.

When at least one number is set, the Today line SHALL show a plain position label in the form "name · range", from the person's own numbers: "Below your minimum · N ml" when the total is under the minimum, "Above your maximum · N ml" when the total is over the maximum, otherwise "Within your limits · A to B ml" (or "Within your limit · N ml" with one number set). Only the numbers that are set take part. A total equal to a number is within it. The label SHALL use the neutral sequential colour ramp of `range-labels`, never green, yellow, orange or red, with the text always present. It SHALL NOT contain advice, a warning, an instruction or a condition name, and SHALL NOT be accompanied by an alert, notification, reminder, progress ring or percentage.

#### Scenario: Empty by default
- **WHEN** the limits settings are opened on a new Profile
- **THEN** both fields are empty and no example or default number is shown

#### Scenario: Only a maximum
- **WHEN** the person sets a maximum of 1500, no minimum, and 900 ml were drunk today
- **THEN** the line reads "Today: 900 ml" with the label "Within your limit · 1500 ml"

#### Scenario: Only a minimum
- **WHEN** the person sets a minimum of 1400, no maximum, and 900 ml were drunk today
- **THEN** the label reads "Below your minimum · 1400 ml"

#### Scenario: Both numbers, within
- **WHEN** the minimum is 1400, the maximum is 2000 and the total is 1700
- **THEN** the label reads "Within your limits · 1400 to 2000 ml"

#### Scenario: Above the maximum
- **WHEN** the maximum is 1500 and the total today is 1700 ml
- **THEN** the label reads "Above your maximum · 1500 ml" in the neutral ramp, with no alert, notification, advice or red

#### Scenario: Exactly on a number
- **WHEN** the total equals the minimum or equals the maximum
- **THEN** the label is "Within your limits"

#### Scenario: Cleared
- **WHEN** the person clears both numbers
- **THEN** the line reads "Today: N ml" with no label, the graph lines disappear and no entry changes

#### Scenario: Minimum above maximum
- **WHEN** a minimum of 2500 is entered while the maximum is 2000
- **THEN** it is refused with a message about the accepted combination and the previous values are kept

#### Scenario: Invalid number
- **WHEN** 50 or 20000 is entered
- **THEN** it is refused and the previous value (or none) is kept

### Requirement: Reference note with Dutch sources
Next to the limits fields the system SHALL offer an "About these amounts" note that states, as information with its source and not as a target of the app: the general amounts Voedingscentrum gives for adults (at least 1.4 litres a day from drinks for women and girls from 13, at least 1.4 to 1.8 litres for men and boys from 13, all drinks count, and that more is needed with exercise, heat, fever, vomiting or diarrhoea), that a maximum for a condition (for example reduced heart, kidney or liver function) is set per person by the doctor (hospital leaflets commonly mention 1.5 to 2 litres), that what counts towards it, such as soup, custard or yoghurt, differs per care team and is to be asked there, and that questions go to the huisarts, with links to Voedingscentrum (Vocht), Radboudumc (Vochtbeperking) and Thuisarts. It SHALL say the app is not a medical device and sets no target. It SHALL claim no endorsement, use no logos and SHALL NOT write into, preselect or default either field.

#### Scenario: Open the note
- **WHEN** the person taps the info control next to the limits
- **THEN** the note shows the Voedingscentrum amounts with their source, the line that a maximum for a condition is personal, and the links, and both fields stay as they were

#### Scenario: No shortcut to a number
- **WHEN** the limits settings are inspected in English and Dutch
- **THEN** no control fills a field with a published amount

### Requirement: No target set by the app, no reminder or advice
The system SHALL NOT set, suggest or compare to a daily target, minimum or maximum of fluid on its own; the only numbers used are those the person entered. The system SHALL NOT show a progress indicator or percentage toward an amount; SHALL NOT send a reminder or notification to drink or to stop drinking; and SHALL NOT advise, label or comment on an amount or a day beyond the position label above, including words such as enough, too little, too much, dehydrated, good, bad, should or must in either language. Figures SHALL be plain numbers.

#### Scenario: Any amount without numbers set
- **WHEN** no minimum or maximum is set and one day totals 300 ml and another 4500 ml
- **THEN** both appear in the same style with no label, colour or message

#### Scenario: No reminder
- **WHEN** no entry is made all day and a minimum is set
- **THEN** no notification is sent and the card shows only the total and the position label

#### Scenario: Wording check
- **WHEN** the fluid strings are scanned in English and Dutch
- **THEN** none contains an advice phrase, a verdict word or a recommended amount of the app; the position label words and the person's own numbers shown back are allowed

### Requirement: Retrieve fluid intake history
The system SHALL allow retrieving all entries of a Profile in reverse chronological order and the total per local calendar day for a range.

#### Scenario: History returned
- **WHEN** a Profile has three entries
- **THEN** they are returned newest first

#### Scenario: Day totals
- **WHEN** a range holds entries on two days
- **THEN** two totals are returned, one per day, and days without entries are not returned

### Requirement: Fluid intake text in English and Dutch
Every string of this metric SHALL exist in English and Dutch. Dutch SHALL use "Drinken" for the metric, "Vandaag" for the daily total, "Jouw minimum", "Jouw maximum" and "Jouw grenzen" for the limits, "Onder je minimum", "Binnen je grenzen" (one number: "Binnen je grens") and "Boven je maximum" for the position labels, "Over deze hoeveelheden" for the note, "Glas", "Kopje" and "Fles" for the default button names and "Ongedaan maken" for Undo; English SHALL use "Fluid intake", "Today", "Your minimum", "Your maximum", "Your limits", "Below your minimum", "Within your limits" (one number: "Within your limit"), "Above your maximum", "About these amounts", "Glass", "Cup", "Bottle" and "Undo".

#### Scenario: Dutch card
- **WHEN** the app language is Dutch, the minimum is 1400 and the total is 900
- **THEN** the card is titled "Drinken" and shows "Vandaag: 900 ml" with the label "Onder je minimum · 1400 ml"
