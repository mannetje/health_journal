# app-identity Specification

## Purpose
Defines the platform-neutral brand identity of the app icon. Platform packaging details (adaptive layers, safe zones, store asset sizes) are recorded in the ADRs.

## Requirements

### Requirement: Icon palette
The app icon SHALL use brand navy 0B1D3A as background and the foreground accent colors 6FB1F2, FFD93D and FF6B6B, each with a contrast ratio of at least 4.5:1 against the background. A single-color (monochrome) variant SHALL exist for platforms with themed icons. A 512 px store icon SHALL be provided.

#### Scenario: Contrast
- **WHEN** an accent color is compared to 0B1D3A
- **THEN** the contrast ratio is at least 4.5:1

### Requirement: Consistent branding
The brand navy SHALL match the top bar color (see theming).

#### Scenario: Same navy
- **WHEN** the icon and top bar are compared
- **THEN** both use 0B1D3A
