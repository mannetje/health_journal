## 0. Decisions
- [x] 0.1 Scope: rail and content cap, no new library, no two-pane History (drafted)
- [ ] 0.2 Spec review by the user before implementation starts

## 1. Implementation (`:app`)
- [ ] 1.1 `NavigationLayout` thresholds as a pure function with unit tests (portrait phone, landscape phone, 600 dp edge, 840 dp edge)
- [ ] 1.2 Rail in `AppChrome.kt` from the same destinations as the bottom bar
- [ ] 1.3 `AppContent` chooses bar or rail and wraps the content in the width cap
- [ ] 1.4 Log tab row scrolls with the form on short windows
- [ ] 1.5 Rotation keeps the selected tab and typed values (check `rememberSaveable` use)

## 2. Verification
- [ ] 2.1 Emulator: portrait phone, landscape phone, tablet-sized window; light and dark; English and Dutch
- [ ] 2.2 Screenshots for the README and `docs/dev/` note on the thresholds
- [ ] 2.3 CHANGELOG entry, `openspec validate --strict`, `scripts/check-docs.sh`
