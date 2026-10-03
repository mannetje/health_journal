# Update App Launcher Icon

## Why
`AndroidManifest.xml` still points `android:icon` and `android:roundIcon` at the platform placeholder `@android:drawable/sym_def_app_icon`, and the project has no `mipmap-*` resources at all. The app therefore shows the generic Android icon on the launcher, in the app drawer, in Settings and in the sideload installer. A dedicated "Health Journal" logo has been designed and should become the launcher icon.

## The logo
The supplied artwork (`7.jpg`, opaque white background) is a portrait mark built from these elements:
- **Blue (~`#0560AF`)**: a balance scale (beam, two pans, base) whose central column forms a heart outline.
- **Yellow (~`#F2CE1B`)**: a pulse / ECG line running along the top beam.
- **Red (~`#C1121F`)**: a running figure (left pan, activity), a blood drop (right pan, glucose), and the **"HJ"** monogram inside the heart.

> **Discrepancy to confirm.** The request describes a slate-blue journal cover, white pages and a cyan "health machine". The supplied image has no journal, no white pages and no cyan: it is a blue scale with yellow and red accents on white. This proposal follows the **image**. If a different final SVG is delivered, the layer definitions below are updated to match it.

## What Changes
- Add a **final logo SVG** as the single source of truth (`design/app-icon/health-journal-logo.svg`) and a transparent-background version of the JPEG artwork for reference.
- Add an **adaptive icon** (API 26+) with background, foreground and monochrome layers, wired up through `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
- Add **legacy raster fallbacks** (`ic_launcher.png`, `ic_launcher_round.png`) in each density bucket.
- Point `AndroidManifest.xml` at `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.
- Add a 512x512 PNG store/README icon derived from the same source.
- No dependency changes and no domain or data module changes.

## Capabilities
- **Added Capability:** `branding/app-icon`

## Impact
- Affected code: `app/src/main/AndroidManifest.xml`; new resources under `app/src/main/res/` (`drawable/`, `mipmap-*/`, `values/ic_launcher_background.xml`); new `design/app-icon/` sources.
- `minSdk` is **26**, and adaptive icons exist from API 26. The "API 25 and below" PNG fallbacks therefore cannot be used by any supported device. They are still generated because some launchers and tooling read them, and they cost little. Real devices use the `mipmap-anydpi-v26` adaptive definition.
- App signing and the in-place update path from ADR 0008 are unaffected.
- Documentation: README logo/header, and a short ADR (`0011-app-icon-and-adaptive-layers.md`) recording the layer and colour decisions.

## Open questions
1. ~~Adaptive background colour~~ **Decided: dark navy `#0B1D3A`** with a lightened-colour foreground. Chosen for contrast: the yellow pulse line is about 1.5:1 on white but about 12:1 on navy, and every element clears 4.5:1 on navy.
2. **Final SVG.** Will you supply the vector, or should the JPEG be traced? A supplied SVG is preferable; a trace is only an approximation. Both paths are in `tasks.md`.
