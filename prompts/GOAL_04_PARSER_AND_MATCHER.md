# Goal 04 — Prescription Parser and Fail-Closed Matcher

Implement deterministic layout parsing, direction grammar, local candidate retrieval, and match policy.

## Deliverables

- region and row clustering;
- medicine-line field extraction;
- structured direction parser;
- public DB repository;
- identifier/name/alias/FTS candidate retrieval;
- `config/confidence_policy.yaml` policy loader;
- hard contradiction engine;
- evidence-rich match result;
- manual local search;
- synthetic parser/matcher corpus and property tests.

## Acceptance criteria

- all hard contradiction invariants pass;
- missing strength/form is never inferred;
- same input, DB, and policy always yields identical output;
- ambiguous and unresolved states are first-class results;
- malformed input does not crash or create a high-confidence result;
- performance benchmark reports p95 candidate lookup against representative synthetic DB.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
