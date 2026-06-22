# Goal 03 — Camera, Quality Gate, and Local OCR

Implement the on-device scan shell.

## Deliverables

- CameraX preview and full-resolution capture;
- document boundary overlay;
- blur, glare, clipping, and text-size quality signals;
- bundled Korean ML Kit OCR;
- perspective correction and conservative preprocessing;
- OCR token model with raw text, normalized text, boxes, variant, and flags;
- temporary file lifecycle and startup cleanup;
- explicit image import;
- no network operation from scan flow.

## Acceptance criteria

- airplane-mode first-run OCR works on a supported device after installation;
- process death/cancel/finalize cleanup tests pass;
- network capture shows no scan-derived request;
- image quality failures produce actionable Korean guidance;
- OCR boxes map correctly to displayed image after perspective transform;
- raw and normalized text are stored separately in session memory/local temp state.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
