# Goal 02 — Signed Public Drug Database

Build the normalized public SQLite database and signed manifest.

## Deliverables

- implement `schemas/drug_db_schema.sql`;
- normalize product names, manufacturers, strengths, forms, ingredients, easy information, DUR rules, and source links;
- FTS search population;
- quarantine/conflict report;
- manifest matching `schemas/dataset_manifest.schema.json`;
- Ed25519 signing and independent verification CLI;
- reproducibility test;
- local static-server fixture for Android integration;
- rollback/revocation design documented.

## Acceptance criteria

- same normalized input creates identical uncompressed SQLite bytes or a documented canonical alternative;
- SQLite `quick_check` and foreign keys pass;
- search smoke tests return expected Korean products from synthetic fixtures;
- one-byte artifact/manifest changes fail verification;
- private key is never written to repository or build output;
- missing `e약은요` data remains null and is not generated.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
