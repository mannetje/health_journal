# 21. Medication Compliance and Privacy

- **Date:** 2026-10-06
- **Status:** Accepted
- **Deciders:** Architecture Team, AI Coding Assistant

## Context

Medication is close to the line between a personal log and a medical device. Whether software is a medical device depends on the intended purpose the manufacturer states (EU MDR 2017/745, supervised in the Netherlands by the IGJ). Medication data is also health data under the AVG and UAVG.

## Decision

- **Intended purpose:** the pillbox is a personal logging tool. It is not a medical device. This is stated in the app (a notice on first open, reachable again from Profile), in the README and here.
- **No advice, ever:** the app does not check doses or interactions, does not calculate or suggest a dose, does not link glucose to medication, and says nothing about what to do after a missed dose. A missed intake only shows the status "Missed".
- **Sources:** apotheek.nl and Thuisarts are shown as a neutral list of links, with no summary and no recommendation.
- **No alerts from health values.** Nothing in the app reacts to a measured value.
- **Privacy:** no `INTERNET` permission and no analytics, so the developer collects no health data. Platform backup is outside the app's control; CSV export is the user-controlled copy. Encryption at rest is a separate change.
- **Pharmacy and exchange:** no sale, prescriptions, pharmacy contact or exchange standards. CSV is the only export.
- **Review gate:** any function that diagnoses, advises, alerts on health values, transmits data or serves care providers needs a regulatory assessment ADR before it is built.
- **Spec and docs rules:** specs use "Medication A" placeholders and name no real medicine, and no other project.

## Open items (deferred, not part of this change)

- A licence file for the repository.
- A legal review (MDR intended purpose, AVG for a distributed app) before publishing beyond personal use.
- The adherence overlay on the trend charts.
- The no-advice audit of every user-visible string (tasks 5.4 to 5.8).

## Consequences

- Positive: a small, explicit claim keeps the app out of medical-device territory, and the rule is checkable per string and per feature.
- Negative: useful features such as interaction checks or dose advice are closed unless a new assessment is done.
