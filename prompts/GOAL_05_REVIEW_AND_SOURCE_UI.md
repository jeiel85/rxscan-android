# Goal 05 — Mandatory Review and Official Source UI

Implement the complete review and medicine-information experience.

## Deliverables

- OCR overlay/edit screen;
- medication-line review cards;
- candidate evidence comparison;
- mandatory confirm/reject/unresolved flow;
- photographed prescription direction section;
- separate official information section;
- source agency, dataset, item code, update date, and DB version;
- missing easy-information state;
- stale/revoked data state;
- TalkBack, large text, and non-color confidence semantics.

## Acceptance criteria

- no path bypasses review before finalization;
- ambiguous candidate is never pre-labeled as confirmed;
- official information is never shown as the photographed direction;
- source metadata is visible and accessible;
- UI tests cover large font, TalkBack semantics, rotation/process recreation, unresolved lines, and stale DB.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
