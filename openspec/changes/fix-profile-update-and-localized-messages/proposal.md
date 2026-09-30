# Fix Profile Update and Localize Banners

## Why
Emulator verification of the units feature found two defects that predate it:
1. *Update Profile* created a second profile instead of editing the active one, so edits (for example height 178 to 180 cm) were lost and the summary never changed.
2. Success and error banners were English string literals in the ViewModels, so the Dutch UI showed "Activity session recorded!".

## What Changes
- `ProfileViewModel.saveProfile` updates the active profile in place (same id) when one exists, and creates one only when there is none.
- New `UiText` (resource id plus arguments, or plain text). ViewModel states hold `UiText?` for success, info and error messages; screens call `asString()`.
- Validation errors are thrown as `UiTextException` so they can be translated. Domain exception messages stay as plain text.
- 24 message strings added to `values/strings.xml` and `values-nl/strings.xml`.
- No schema change, no migration. Extra profile rows created by the old behaviour are left alone (deleting user data needs consent).

## Capabilities
- **Modified Capabilities:** `profile` (update), `localization` (messages)

## Impact
- Affected code: `ProfileViewModel`, `LoggingViewModel`, `HistoryViewModel`, `LogMetricScreen`, `HistoryScreen`, `ProfileScreen`, `EntryDialogs`, `ui/common/UiText.kt`, both `strings.xml` files, `ViewModelsTest`.
- Documentation: ADR 0015 (UiText), ADR 0016 (profile update), README, cross-references in ADR 0010 and ADR 0014.
