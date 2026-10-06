## ADDED Requirements

### Requirement: Reminders on AlarmManager
The Android app SHALL schedule only the next reminder slot with `AlarmManager`, using an exact alarm when the exact-alarm permission is granted and an inexact alarm with a hint otherwise, and SHALL re-arm after boot, app update, time change, time-zone change and any schedule edit through broadcast receivers. Notification actions Taken all and Snooze SHALL be handled by a `BroadcastReceiver` without opening the Activity. The notification identity SHALL be derived from the slot time so a re-arm never posts a second notification for the same slot.

#### Scenario: Re-armed after reboot
- **WHEN** the device restarts
- **THEN** the boot receiver re-arms the next reminder for the active profile

#### Scenario: Exact alarm not granted
- **WHEN** the exact-alarm permission is not granted
- **THEN** reminders are inexact and the app shows a hint on how to allow exact alarms

#### Scenario: Re-arm does not duplicate
- **WHEN** the app re-arms while a notification for the same slot is already shown
- **THEN** the existing notification is updated and no second one appears

### Requirement: Reminder permissions
The manifest SHALL declare `POST_NOTIFICATIONS` (requested at first reminder setup on Android 13 and higher, with no request on older versions), `RECEIVE_BOOT_COMPLETED`, and the exact-alarm permission as an optional grant, and SHALL continue to declare no network permission.

#### Scenario: Notification permission on older Android
- **WHEN** the device runs Android 12 or lower
- **THEN** no notification permission prompt is shown

### Requirement: Notification text uses the app locale
Notification text and action labels SHALL be built from a context wrapped with the app locale (`withAppLocale`), not the device language, because receivers run without the Activity.

#### Scenario: Receiver builds Dutch text
- **WHEN** a receiver builds a notification while the app language is Dutch
- **THEN** all its text comes from the Dutch resources
