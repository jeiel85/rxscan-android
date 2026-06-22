# Goal 06 — DUR Engine and Safety Wording

Implement local DUR lookup only for user-confirmed medicines.

## Deliverables

- ingredient/product relationship resolution;
- supported DUR rule-type evaluation;
- deterministic result model;
- fixed warning templates from `08_UX_SPEC.md`;
- source rule ID/type/date display;
- stale-data behavior that disables current-safety-check claims;
- pharmacist-review export containing all fixed wording.

## Acceptance criteria

- unconfirmed or unresolved medicine never participates in DUR evaluation;
- output never tells the user to stop/change/double medication;
- every warning has source type/date/version;
- stale threshold behavior passes clock-controlled tests;
- missing relationship data produces “not evaluated/insufficient data,” not “no interaction.”

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
