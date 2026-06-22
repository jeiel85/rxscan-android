# Decisions, Risks, and Open Questions

## 1. Decision log summary

See `adr/`.

| Decision | Status |
|---|---|
| Android-native first | Accepted |
| No patient-data backend | Accepted |
| CI-built signed static drug DB | Accepted |
| Fail-closed medicine matching | Accepted |
| No generative AI in safety-critical path | Accepted |
| History opt-in and encrypted | Accepted |
| Original image not retained by default | Accepted |

## 2. Major risks

### R1 — pharmacy bag format diversity

Impact: OCR/layout parser may fail on unseen templates.

Mitigation:

- explicit supported scope;
- template-diverse corpus;
- layout-agnostic row evidence;
- user correction/manual search;
- abstention;
- controlled template expansion.

### R2 — product-name OCR ambiguity

Impact: wrong product candidate.

Mitigation:

- item/code priority;
- strength/form hard gates;
- top-two margin;
- mandatory review;
- zero-tolerance release holdout for high-confidence wrong matches.

### R3 — consumer-friendly information coverage gap

Impact: prescription medicines may lack `e약은요` explanations.

Mitigation:

- display available official approval text;
- state that consumer-friendly official content is unavailable;
- pharmacist-reviewed fixed layer only after governance;
- never generate missing content.

### R4 — public source schema/availability change

Impact: stale or malformed DB.

Mitigation:

- immutable snapshots;
- schema contract tests;
- no partial publication;
- freshness warnings;
- rollback versions;
- source runbook.

### R5 — regulatory classification drift

Impact: product may require authorization or different controls.

Mitigation:

- narrow intended use;
- avoid recommendations/calculations;
- formal pre-launch review;
- re-review before adding symptom, adherence, or personalization features.

### R6 — local device compromise

Impact: exposure of saved prescriptions.

Mitigation:

- data minimization;
- encryption;
- app lock;
- no backup;
- original image off by default;
- clear limitation that an unlocked/rooted compromised device cannot be fully defended.

### R7 — CI signing-key compromise

Impact: malicious DB accepted.

Mitigation:

- managed key/KMS;
- restricted environment;
- two-person release control;
- key rotation;
- signed revocation;
- audit logs.

## 3. Open questions for product owner

These do not block the first engineering bootstrap, but must be resolved before public beta:

- final app name and package ID;
- whether history is included in v1 or deferred;
- whether original image retention is offered at all;
- Cloudflare R2 versus another static host;
- commercial model that does not require health-data monetization;
- pharmacist reviewer and review cadence;
- exact supported pharmacy-template launch set;
- whether manual official-drug search is available without a scan;
- support language beyond Korean;
- whether an iOS version is planned after Android validation.
