## MODIFIED Requirements

### Requirement: NHG classification
The classifier SHALL use these bands.
FASTING: HYPOGLYCAEMIA below 3.5; NORMAL from 3.5 to 6.0 inclusive; IMPAIRED_FASTING above 6.0 up to 6.9 inclusive; DIABETES_RANGE above 6.9.
POSTPRANDIAL: HYPOGLYCAEMIA below 3.5; NORMAL from 3.5 up to but excluding 7.8; IMPAIRED_GLUCOSE_TOLERANCE from 7.8 to 11.0 inclusive; DIABETES_RANGE above 11.0.
The category is computed on the stored mmol/L value, stored with the entry, and recomputed on update. The category names are internal identifiers that are stored and exported but never shown to the user. The shown names are Low, Normal, Slightly raised and High blood glucose, with the ranges defined in `range-labels`.

#### Scenario: Fasting boundaries
- **WHEN** fasting levels are 3.4, 3.5, 6.0, 6.1, 6.9 and 7.0
- **THEN** categories are HYPOGLYCAEMIA, NORMAL, NORMAL, IMPAIRED_FASTING, IMPAIRED_FASTING and DIABETES_RANGE

#### Scenario: Postprandial boundaries
- **WHEN** postprandial levels are 7.7, 7.8, 11.0 and 11.1
- **THEN** categories are NORMAL, IMPAIRED_GLUCOSE_TOLERANCE, IMPAIRED_GLUCOSE_TOLERANCE and DIABETES_RANGE

#### Scenario: Internal names not shown
- **WHEN** an entry with category DIABETES_RANGE is displayed
- **THEN** the screen shows "High blood glucose" with its range and never the category name
