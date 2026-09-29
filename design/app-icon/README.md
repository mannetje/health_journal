# App icon sources

- `health-journal-logo.svg`: master vector (transparent background, original colours).
- `health-journal-512.png`: 512 px full-bleed icon (navy tile) for the README and store listings.
- `generate-icons.js`: regenerates everything derived from the master:
  the vector drawables (`ic_launcher_foreground`, `ic_launcher_monochrome`, `logo_health_journal` in `drawable/` and `drawable-night/`, and `logo_health_journal_on_navy` for the top app bar),
  the adaptive-icon XMLs, the background colour, the legacy `mipmap-*` PNGs and the 512 px icon.

```bash
npm install @resvg/resvg-js   # once, in any scratch folder
node design/app-icon/generate-icons.js
```

Colours: light-theme logo `#0560AF` / `#F2CE1B` / `#C1121F`; dark tile `#0B1D3A` with `#6FB1F2` / `#FFD93D` / `#FF6B6B`.
See [ADR 0011](../../docs/adr/0011-app-icon-and-adaptive-layers.md).
