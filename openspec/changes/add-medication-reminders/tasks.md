## 0. Decisions
- [x] 0.1 Exact alarms: use when granted, otherwise inexact with a hint (approved)
- [x] 0.2 One notification per slot with Taken all and Snooze; per-medication handling in the pillbox (approved 2026-10-06)
- [x] 0.3 Release order: `add-medication-management` first (released in 1.5.3, 2026-10-07)
- [x] 0.4 One Snooze action, length 10, 30 or 60 minutes set in Profile, default 10 (approved 2026-10-07)

## 1. Domain
- [x] 1.1 `NextSlot(medications, after)` pure function over schedule versions and the active profile, with tests for the next slot across midnight, versions, archive, as-needed (no slot), daylight saving and several medications at one time
- [x] 1.2 `ReminderSchedulerPort` (arm, cancel) and a use case that finds the slot at a time and records "Taken all" for its `openItems` through the existing `RecordSlotIntakesUseCase`, never overwriting an existing outcome, with tests

## 2. App
- [x] 2.1 `AlarmManager` implementation (next alarm only, exact when granted else inexact), one-shot snooze alarm using the snooze length from settings
- [x] 2.2 Notification channel, slot notification (inbox style when details are shown, generic when hidden), `BroadcastReceiver` (`goAsync`) for Taken all and Snooze, tap opens the pillbox on Today
- [ ] 2.3 Notification text and action labels built from an app-locale context, tested in Dutch on an English device
- [x] 2.4 Boot, app-update, time-zone and clock-change receivers; re-arm on schedule edit, archive, delete and profile switch
- [x] 2.5 Permission flow with explanation, denied banner, exact-alarm hint, battery hint
- [x] 2.6 Profile section for reminder settings: snooze length (default 10 minutes) and the lock-screen details setting (default hidden)
- [x] 2.7 English and Dutch strings, `UiText`; layout check at 1.3x and 2.0x
- [x] 2.8 Recording an outcome in the pillbox updates or cancels that slot's notification

## 3. Verification
- [ ] 3.1 Emulator and real-device check, including reboot, Doze, a slot with three medications, Taken all, Snooze and a medication archived before the alarm
- [x] 3.2 Wording check of every notification string against the `compliance` no-advice rule

## 4. Docs
- [x] 4.1 ADR 0022 (AlarmManager, grouping, exact-alarm decision), README, CHANGELOG (Unreleased), affected specs, the ADR index and `scripts/check-docs.sh`
- [ ] 4.2 Archive the change and merge the deltas into `openspec/specs`
- [ ] 4.3 Commits carry no attribution trailer
