# Add Medication Reminders

## Why
The pillbox (`add-medication-management`) shows what is planned, but only when the user opens it. A reminder at the planned time is what makes it work in daily life. People usually take several medications together (for example at 07:30 or 19:30), so one moment must give one notification, not one per medication.

## What Changes
- **Grouped reminders:** one local notification per slot (all planned intakes at the same local time), with **Taken all** and **Snooze** actions. With details shown the body lists each medication with its dose, otherwise it only says "Medication reminder".
- **Actions without opening the app:** Taken all records every pending intake of the slot as taken and dismisses the notification. Snooze offers 10, 30 and 60 minutes and keeps the original planned time. Tapping the notification opens the pillbox on that slot, where each medication can still be marked Taken or Skipped on its own.
- **Reliability:** reminders are re-armed after reboot, app update, time or time-zone change and any schedule or archive change. Exact alarms are used when granted, otherwise inexact alarms with a hint.
- **Permission flow:** notification permission is asked when the first schedule is saved, with a short explanation first. If denied, the pillbox keeps working and a banner says reminders are off.
- **Lock screen:** details are hidden by default, with a setting to show them.
- **Localization:** notification text and actions use the app language, not the device language.

## Capabilities
- **Modified Capability:** `medication` (reminders, grouped slots, permission flow)
- **Modified Capability:** `privacy` (details hidden on a locked device)
- **Modified Capability:** `localization` (notification language)
- **Modified Capability:** `platform-android` (AlarmManager, permissions, receivers)

## Impact
- **Database:** no change. Reminders read the schedule versions and intakes from `add-medication-management` and write outcomes through the same use cases.
- **Affected code:** `domain` gets `ReminderSchedulerPort` and a pure `NextSlot` function; `app` gets an `AlarmManager` scheduler, a notification builder, an action receiver, boot, update, time-change receivers, a Profile section for reminder settings, strings in English and Dutch.
- **Manifest:** `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, and the exact-alarm permission as an optional grant. Still no `INTERNET`.
- **Dependencies:** none.
- **Docs:** ADR 0022 (reminders: AlarmManager, grouping, exact-alarm decision), README, CHANGELOG, specs.

## Depends on
`add-medication-management` (schedule versions, slots, intake use cases, Profile notice). Implement and release it first.

## Non-goals
- No reminder driven by a health value, no advice wording ("take now"), no escalation or repeat nagging.
- No user-defined named moments (grouping is by identical time).
- No adherence figures (`add-medication-adherence`).
