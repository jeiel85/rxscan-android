# Regulatory Review Package (Goal 08)

Inputs for a regulatory applicability review of the final build
(`release_gate: regulatory_review`; `10_RELEASE_COMPLIANCE.md`). Not legal advice;
for counsel/regulatory advisor assessment.

## Intended use statement

RxScan helps a user read their own dispensing-bag/prescription photo on-device and
look up **official public** medicine information (product identity, approved
information, and official DUR safety-use notices). It is an **information aid**.

## What the app does NOT do

- No diagnosis.
- No dose recommendation or calculation.
- No instruction to start, stop, change, or double any medication.
- No generative-AI determination of medicine, dose, or safety (AGENTS.md).
- No automatic confirmation: a human review step is mandatory and cannot be
  bypassed; ambiguous/unresolved results are first-class and never preselected.

## Safety design summary

- Fail-closed matching: hard contradictions reject candidates before scoring; missing
  fields are never inferred (`engine:matcher`, property-tested).
- DUR limited to confirmed medicines and to pairwise rule types evaluable without
  patient data; otherwise reported as not-evaluated/insufficient (`engine:dur`).
- Official information is shown separately from the photographed direction.
- Source provenance (agency, dataset, item code, date, DB version) always visible.
- Stale/revoked data disables current-safety claims.

## Data/privacy posture

- On-device processing; no account; no scan-derived network requests; opt-in
  encrypted history (see `docs/PRIVACY_POLICY_DRAFT.md`,
  `docs/THREAT_MODEL_TRACEABILITY.md`).

## Review questions for the advisor

1. Given the intended use and the explicit exclusions, what is the medical-device
   classification (if any) in the target market(s)?
2. Are the fixed wordings (see `docs/PHARMACIST_SIGNOFF_PACKAGE.md`) acceptable?
3. Are labeling/store-listing claims constrained appropriately (no diagnostic or
   safety guarantees)?
4. Incident/vigilance obligations for an information aid of this kind?

## Decision record

- Applicability outcome: ____________________
- Conditions/constraints: ____________________
- Reviewer / date / build version: ____________________
