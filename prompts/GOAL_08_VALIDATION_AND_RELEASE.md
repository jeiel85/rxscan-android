# Goal 08 — Validation, Compliance, and Release Candidate

Prepare a production release candidate without weakening earlier safety rules.

## Deliverables

- corpus annotation tooling/process;
- benchmark runner and metric report;
- device/OEM test matrix;
- data update rollback/revocation drill;
- penetration/dependency/license/SBOM reviews;
- privacy policy draft based on actual implementation;
- Play health/data-safety checklist;
- pharmacist sign-off package;
- regulatory review package;
- staged rollout and incident runbook.

## Acceptance criteria

- all gates in `09_TEST_QUALITY.md` and `checklists/RELEASE_CHECKLIST.md` are satisfied or explicitly marked as launch blockers;
- no wrong high-confidence medicine match in the required independent holdout;
- no sensitive network/log/backup finding;
- rollback and signed revocation demonstrated;
- store listing and screenshots do not make unsupported medical claims;
- release is blocked automatically if a mandatory gate fails.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
