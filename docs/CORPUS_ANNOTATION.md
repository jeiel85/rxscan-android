# Corpus Annotation Tooling and Process (Goal 08)

Defines the annotation schema and governance for the validation corpus
(09_TEST_QUALITY.md §2/§3). Supports the holdout gate (`holdout_3000_real`).

## Governance (hard rules)

- **No real prescription in source control.** Synthetic fixtures only in the repo.
- Consent record is stored **separately** from the image.
- Access limited to authorized QA; retention period documented; delete on expiry.
- Patient identifiers removed before annotation where possible.
- **Holdout split by pharmacy/template**, not random-image-only.
- Do not tune thresholds against the production holdout (§7).

## Sources

- Synthetic dispensing-bag generator (default).
- Consented real images with documented purpose → fully de-identified derived
  fixtures.
- Images across supported devices/conditions (lighting, glare, crumple, fading).

## Annotation schema (per image)

| Field | Notes |
| --- | --- |
| layout_supported | supported / unsupported |
| quality_flags | blur/glare/clipping/text-too-small/underexposure |
| document_polygon | 4 corner points |
| ocr_ground_truth_lines | verbatim |
| medicine_rows | row boxes |
| raw_printed_medicine_string | as printed |
| official_item_code | MFDS item code |
| strength / form / manufacturer | structured |
| direction_fields | dose/frequency/timings/meal/offset/duration |
| expected_candidate_class | VERIFIED / HIGH_CONFIDENCE / AMBIGUOUS / UNRESOLVED |
| required_abstention_reason | when expected class is UNRESOLVED |

Safety-critical fields (item code, strength, form, expected class) require **double
annotation and adjudication**.

## Process

1. Generate/import images under governance rules.
2. Two annotators label independently; adjudicate disagreements on safety fields.
3. Export a machine-readable corpus (JSON) keyed by image id.
4. Run the benchmark runner (matcher/parser) against the corpus; produce the metric
   report (09_TEST_QUALITY.md §4).
5. Hold out by template; never feed the holdout into threshold tuning.

## Tooling status

- Schema and governance defined here; the synthetic corpus generator and the
  cross-platform JSON exporter are to be built when the real corpus program starts.
  The matcher/parser benchmark harness already exists as JVM tests
  (`MatcherHoldoutBenchmarkTest`, `DrugMatcherBenchmarkTest`).
