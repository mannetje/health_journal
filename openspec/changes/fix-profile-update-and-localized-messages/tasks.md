## 1. Profile update
- [x] 1.1 `saveProfile` edits the active profile via `Profile.reconstruct` and the repository upsert
- [x] 1.2 ViewModel test: second save keeps id and count, updates name, height and sex
- [x] 1.3 Emulator: change height, the summary follows; entries still listed

## 2. Localized messages
- [x] 2.1 `UiText`, `UiTextException`, `toUiText`, `asString`
- [x] 2.2 Logging, Profile and History ViewModels use `UiText` for success, info, error and edit errors
- [x] 2.3 Screens and edit dialog resolve with `asString()`
- [x] 2.4 English and Dutch strings for all messages
- [x] 2.5 ViewModel tests assert resource ids
- [x] 2.6 Emulator: banners in Dutch and English, validation error with unit (kg checked on device; lb and mi covered by ViewModelsTest); export shows a CSV preview, no banner

## 3. Docs
- [x] 3.1 ADR 0015 and ADR 0016, README (feature bullet, diagram, ADR list), ADR 0010 and ADR 0014 cross-references
