# Goal 07 — Private Storage and Security Hardening

Add optional encrypted history and complete security controls.

## Deliverables

- opt-in prescription history;
- encrypted private DB with Keystore-protected key;
- app lock option;
- original image retention off by default and encrypted when enabled;
- backup/device-transfer exclusions;
- screenshot/recents/notification privacy;
- redacted local diagnostics and explicit export preview;
- delete one/delete all/key invalidation flows;
- network security configuration;
- security tests and threat-model traceability.

## Acceptance criteria

- history works without account/network;
- private DB cannot be opened without the protected key;
- key invalidation fails closed and offers clear destructive reset;
- backup and device-transfer tests expose no private data;
- release logs and diagnostics contain no prescription/OCR values;
- deleting all data removes private DB, retained images, wrapped key, and caches.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
