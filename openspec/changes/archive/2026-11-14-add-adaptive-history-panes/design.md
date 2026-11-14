## Context
`HistoryList` shows either the entry timeline or, behind a toggle kept per metric, the chart and statistics. Both read the same state and the same date range, so showing them at once needs no new data flow.

## Decisions

### 1. Pane mode is a pure function of the window width
`AdaptiveLayout.historyTwoPane(widthDp)` is true from 900 dp. A Pixel Tablet in portrait (800 dp) keeps one pane; in landscape (1280 dp) it gets two. After the 80 dp rail and 16 dp margins each pane is then at least about 390 dp, enough for the chart and the cards.

### 2. Two named panes instead of an `if` around the toggle
The timeline (migraine-day chip, month rows, entries) and the chart (trend and statistics) become two local composables. The single-pane modes render one of them as before, the two-pane mode renders both in a `Row` with equal weight. The chart pane scrolls on its own; the list pane keeps its `LazyColumn`.

### 3. Toggle hidden, stored choice kept
In two-pane mode the toggle button is hidden and the stored per-metric graph choice is ignored, not changed, so going back to a narrow window restores what the user had.

### 4. Cap per tab
The 840 dp cap of the first change becomes a function of the pane mode: 1240 dp for History in two-pane mode, 840 dp otherwise. The wide History still does not stretch over a 2560 dp window.

## Risks / Trade-offs
- Two scrolling regions need distinct scroll states; the chart pane's state is keyed on the metric and range so it starts at the top like the list does.
- A migraine-day tap used to switch to the list; with both visible it only filters, which is the same information with one tap less.
