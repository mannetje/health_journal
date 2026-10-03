## MODIFIED Requirements

### Requirement: NHG BMI categories
The BMI category SHALL be UNDERWEIGHT below 18.5; NORMAL from 18.5 up to but excluding 25.0; OVERWEIGHT from 25.0 up to but excluding 30.0; OBESE from 30.0 upward. The shown label is the class name followed by its BMI range, as defined in `range-labels`, and never describes the class as a condition.

#### Scenario: Boundaries
- **WHEN** BMI is 18.4, 18.5, 24.9, 25.0, 29.9 or 30.0
- **THEN** the categories are UNDERWEIGHT, NORMAL, NORMAL, OVERWEIGHT, OVERWEIGHT and OBESE respectively

#### Scenario: Label with range
- **WHEN** a BMI of 31.2 is shown in English
- **THEN** the label is "Obese · BMI 30 and above"
