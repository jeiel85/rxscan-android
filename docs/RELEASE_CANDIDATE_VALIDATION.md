# Release Candidate Validation Report (Goal 08)

Summarizes the validation status against `09_TEST_QUALITY.md` §5 and
`checklists/RELEASE_CHECKLIST.md`. The machine-readable gate status lives in
`config/release_gates.json`; `tools/ci/release_gate.py` blocks a release while any
mandatory gate is not `pass`.

**Current verdict: NOT RELEASABLE.** Automated safety/privacy gates pass in CI, but
real-world gates (independent holdout, device matrix, pentest, audits, pharmacist
and regulatory sign-off, published policy, staged rollout) remain open launch
blockers. This is the correct state for an engineering preview.

## Automated gates satisfied in CI (JVM unit/property tests + builder + policy)

- Hard-contradiction invariants — `engine:matcher` property tests.
- No wrong high-confidence on a synthetic holoout — `MatcherHoldoutBenchmarkTest`
  (120 synthetic products + distractors; zero wrong VERIFIED/HIGH selections,
  top-3 recall ≥ 99.5% on ambiguous lines, unresolved stays unresolved).
- Direction grammar exact-match + conflict detection — `engine:parser` tests.
- Privacy/security units — key wrap/unwrap fail-closed, redaction/non-leakage,
  delete-all/reset, temp-image lifecycle.
- Manifest signature + tamper + rollback/revocation — `tools/drug-data-builder`
  tests; `docs/ROLLBACK_AND_REVOCATION.md`.
- Repository policy — no analytics/ads, no INTERNET permission.
- Fixed medical copy — no banned wording (DUR/review/detail copy tests).
- Reproducible public DB build — byte-identical artifact test.

## Metrics (synthetic, CI-scale)

The matcher benchmark reports high-confidence count and ambiguous top-3 recall to
stdout (`MatcherHoldoutBenchmarkTest`), and `DrugMatcherBenchmarkTest` reports p95
candidate lookup over 5,000 records. These are synthetic stand-ins; real metrics
require the corpus in `docs/CORPUS_ANNOTATION.md`.

## Open launch blockers (must be `pass` before release)

| Gate | Why open |
| --- | --- |
| `holdout_3000_real` | needs ≥3,000-line independent real holdout (≥500 products, ≥30 templates) |
| `network_no_sensitive` | needs device MITM capture test |
| `backup_transfer_leak` | config in place; needs device backup/restore test |
| `ui_instrumentation` | logic unit-tested; full TalkBack/large-font/rotation needs emulator |
| `ocr_offline_device` | airplane-mode first-run OCR needs device |
| `device_matrix` | `docs/DEVICE_TEST_MATRIX.md` not yet executed |
| `pentest`, `sbom_license` | external security activities |
| `pharmacist_signoff` | `docs/PHARMACIST_SIGNOFF_PACKAGE.md` not yet signed |
| `regulatory_review` | `docs/REGULATORY_REVIEW_PACKAGE.md` not yet completed |
| `privacy_policy_published`, `data_safety_form` | drafts pending publication/review |
| `staged_rollout`, `crash_free_beta` | needs rollout config + internal beta |

## Limitations

A zero-error synthetic result does not prove zero real-world errors. Do not tune
thresholds against the production holdout (09_TEST_QUALITY.md §7). Report confidence
intervals and corpus limitations when the real holdout is run.
