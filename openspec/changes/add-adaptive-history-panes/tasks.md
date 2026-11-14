## 0. Decisions
- [x] 0.1 Scope: side by side entries and chart on wide windows, no detail pane (drafted)

## 1. Implementation (`:app`)
- [x] 1.1 `historyTwoPane` and `contentMaxWidthDp` as pure functions with unit tests
- [x] 1.2 `HistoryList` split into a list pane and a chart pane, shown one at a time or side by side
- [x] 1.3 Toggle hidden in two-pane mode, stored per-metric choice kept
- [x] 1.4 `AppContent` passes the pane mode and the wider cap for History only

## 2. Verification
- [x] 2.1 Emulator: tablet landscape (two panes) and portrait-sized window (one pane); light and dark
- [x] 2.2 Screenshots in `docs/screenshots.md`, `docs/dev/adaptive-layouts.md`, CHANGELOG
- [x] 2.3 `openspec validate --strict`, `scripts/check-docs.sh`, unit tests, lint
