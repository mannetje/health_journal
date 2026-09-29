## ADDED Requirements

### Requirement: Branded launcher icon
The system SHALL display the Health Journal logo as the app's launcher icon instead of the platform default.

#### Scenario: Icon shown on launcher
- **WHEN** the app is installed on a device
- **THEN** the launcher, app drawer and Settings SHALL show the Health Journal logo
- **AND** `AndroidManifest.xml` SHALL reference `@mipmap/ic_launcher` for `android:icon` and `@mipmap/ic_launcher_round` for `android:roundIcon`

### Requirement: Single vector source of truth
The system SHALL keep one final logo SVG in `design/app-icon/` from which every icon asset is derived.

#### Scenario: Derived assets
- **WHEN** the logo changes
- **THEN** the adaptive foreground, the monochrome layer, the legacy PNGs and the 512 px icon SHALL all be regenerated from that SVG, and no asset SHALL be edited by hand independently

### Requirement: Adaptive icon structure
The system SHALL provide `mipmap-anydpi-v26/ic_launcher.xml` and `mipmap-anydpi-v26/ic_launcher_round.xml`, each an `<adaptive-icon>` with `<background>`, `<foreground>` and `<monochrome>` children.

#### Scenario: Launcher applies its mask
- **WHEN** a launcher on API 26+ renders the icon with a circle, squircle or rounded-square mask
- **THEN** the full logo SHALL remain visible and uncropped
- **AND** the foreground artwork SHALL fit inside the central 66 dp diameter safe zone of the 108 dp layer

### Requirement: Background layer
The system SHALL define the background layer as a single solid colour resource, `ic_launcher_background`, with no gradients or images.

#### Scenario: Background contrast
- **WHEN** the icon is rendered
- **THEN** every foreground colour SHALL have a contrast ratio of at least 4.5:1 against the background so that every element of the mark stays legible
- **AND** the value SHALL be dark navy `#0B1D3A`

### Requirement: Foreground layer
The system SHALL provide the logo, recoloured for the dark tile, as a transparent-background vector drawable (`drawable/ic_launcher_foreground.xml`): the scale and heart in light blue `#6FB1F2`, the pulse line in yellow `#FFD93D`, and the runner, blood drop and "HJ" monogram in coral `#FF6B6B`. Each colour SHALL have a contrast ratio of at least 4.5:1 against the background.

#### Scenario: Transparent foreground
- **WHEN** the foreground drawable is rendered over any background
- **THEN** it SHALL have no opaque backdrop of its own; only the logo shapes SHALL be drawn

### Requirement: Monochrome layer for themed icons
The system SHALL provide `drawable/ic_launcher_monochrome.xml`, a single-colour flattened silhouette of the foreground mark on a transparent background.

#### Scenario: Themed icons on Android 13+
- **WHEN** the user enables themed icons in the launcher
- **THEN** the launcher SHALL tint the monochrome layer with the wallpaper-derived colour
- **AND** the mark SHALL stay recognisable, with the pulse line, runner, drop and "HJ" distinguishable from the scale, using negative space (knock-outs) rather than colour to separate them

### Requirement: Legacy raster fallbacks
The system SHALL provide `ic_launcher.png` and `ic_launcher_round.png` in `mipmap-mdpi` (48 px), `-hdpi` (72 px), `-xhdpi` (96 px), `-xxhdpi` (144 px) and `-xxxhdpi` (192 px).

#### Scenario: Launcher without adaptive support
- **WHEN** a launcher or tool ignores the adaptive definition
- **THEN** it SHALL find a correctly sized PNG for the current density

### Requirement: Store and README icon
The system SHALL provide a 512x512 full-bleed PNG of the icon for the README and any future store listing.

#### Scenario: Asset available
- **WHEN** a store listing or the README needs the logo
- **THEN** `design/app-icon/health-journal-512.png` SHALL exist, match the launcher artwork and use the solid navy background (full-bleed, no transparency, as Google Play requires)

## MODIFIED Requirements

### Requirement: Application manifest branding
`AndroidManifest.xml` SHALL reference the project's own mipmap resources for `android:icon` and `android:roundIcon`; it SHALL NOT reference `@android:drawable/sym_def_app_icon`.

#### Scenario: Manifest audit
- **WHEN** the manifest is inspected
- **THEN** no platform placeholder icon reference SHALL remain

### Requirement: Per-theme in-app logo
The system SHALL provide the logo as a vector drawable `logo_health_journal` in two variants: `drawable/` with the original colours for the light theme and `drawable-night/` with the lightened colours for the dark theme.

#### Scenario: Logo follows the system theme
- **WHEN** the device switches between light and dark theme
- **THEN** the resource resolves to the matching variant, in line with the app's `isSystemInDarkTheme()`-based Compose theming

### Requirement: Branded top app bar
The system SHALL show the Health Journal logo at the trailing end of the top app bar on the Log, History and Profile screens, on the brand navy `#0B1D3A` in both light and dark themes, with white title text and the lightened-colour logo variant (`logo_health_journal_on_navy`).

#### Scenario: Logo legible in both themes
- **WHEN** the device is in light mode or in dark mode
- **THEN** the top bar SHALL be navy with the logo visible at the right end
- **AND** every logo colour SHALL have a contrast ratio of at least 4.5:1 against the bar
