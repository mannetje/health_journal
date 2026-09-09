# 1. Record Architecture Decisions

- **Date:** 2026-09-09
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

As the `health_journal` Android application is modernized from an outdated Java stub into a production-grade, offline-first application, critical architectural and technical decisions must be documented. Without a systematic record, architectural intent is lost, leading to accidental architecture erosion, inconsistent patterns, and undocumented dependencies over time.

We need a lightweight, version-controlled process that is accessible to all human developers and AI coding agents working on this project.

## Decision

We will use **Architecture Decision Records (ADRs)** as described by Michael Nygard.

1. **Storage Location:** All ADRs will be stored in the repository under `docs/adr/`.
2. **File Naming:** Files will use a 4-digit zero-padded sequential number followed by a kebab-case title: `NNNN-title-in-kebab-case.md` (e.g., `0001-record-architecture-decisions.md`).
3. **Structure:** Each ADR will follow a standardized template:
   - **Title & Number**
   - **Metadata:** Date, Status (Proposed / Accepted / Deprecated / Superseded), Deciders
   - **Context:** The problem or forces driving the decision
   - **Decision:** The chosen path, alternatives evaluated, and rationale
   - **Consequences:** Trade-offs, positive impacts, and risks/costs introduced
4. **Scope of Records:** Every significant technical choice—including module structure, isolation rules, third-party library adoption, database engines, and clinical guidelines—must be recorded in an ADR prior to merging.

## Consequences

### Positive
- Transparent, auditable architectural history directly in version control.
- Clear constraints for both human contributors and AI assistants.
- Architectural alignment across all development phases.

### Negative / Trade-offs
- Requires discipline to author and review ADRs for major choices before implementation.
- Superseded decisions require explicit status updates in historical documents.
