## 1. Data & Domain Layer
- [ ] 1.1 Update `BloodPressureEntry` and `BloodPressureReading` to include `pulse` (heart rate in bpm), and update Blood Pressure Room entity, DAO, mappers, and Room migration with unit/migration tests.
- [ ] 1.2 Implement smart pre-fill fallback logic in `LoggingViewModel` (Latest entry -> Profile-derived ideal -> Standard default) with unit tests.

## 2. Presentation Layer: Canvas Ruler Pickers (Weight & Waist)
- [ ] 2.1 Implement `HorizontalRulerPicker` Compose component with Canvas tick rendering (long whole numbers, short decimals), fixed center indicator, and smooth `rememberSnapFlingBehavior`.
- [ ] 2.2 Wire `HorizontalRulerPicker` into `LogMetricScreen` for Weight and Waist Circumference tabs.

## 3. Presentation Layer: Stacked BP & Pulse Pickers
- [ ] 3.1 Implement `StackedBpPulsePicker` Compose component featuring three stacked horizontal `LazyRow` components (Systolic in Red, Diastolic in Blue, Pulse in Green) with selection bounding boxes and faded unselected states.
- [ ] 3.2 Wire `StackedBpPulsePicker` into `LogMetricScreen` for Blood Pressure tab.

## 4. Internationalization & Guidelines Compliance
- [ ] 4.1 Add all English and Dutch string resources (`values/strings.xml` and `values-nl/strings.xml`) for labels, units, and accessibility/TalkBack descriptions.
- [ ] 4.2 Ensure Material 3 compliance, 48dp touch targets, and Hexagonal architecture boundary separation.

## 5. State Integration & Verification
- [ ] 5.1 Bind initial picker scroll positions to pre-filled values and connect scroll/snap state changes to ViewModel inputs.
- [ ] 5.2 Verify implementation with UI and ViewModel unit tests.
