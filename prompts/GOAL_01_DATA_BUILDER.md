# Goal 01 — Official Data Builder

Implement a local/CI CLI that fetches and snapshots the initial MFDS sources defined in `config/source_registry.yaml`.

## Deliverables

- source adapters with pagination, retries, timeouts, and schema validation;
- API keys only through environment variables;
- immutable raw snapshot metadata and SHA-256;
- fixtures made from synthetic/minimal sanitized API-shaped responses;
- normalized staging models;
- source-level build report;
- command examples for one source and all sources;
- no final SQLite publication yet.

## Required failure behavior

- fail on missing required fields or page discontinuity;
- do not silently coerce schema changes;
- mark optional-source unavailability distinctly;
- redact secrets from logs.

## Acceptance criteria

- fixture tests cover success, timeout, malformed JSON/XML, changed field, duplicate page, and Korean encoding;
- repeated normalization of the same fixture is byte-for-byte deterministic;
- build report contains source ID, counts, fetch time, and hashes;
- service key is absent from process output and artifacts.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
