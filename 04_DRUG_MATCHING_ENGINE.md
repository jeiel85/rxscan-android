# Drug Matching Engine

## 1. Principle

Candidate retrieval may be permissive; final acceptance must be conservative.

The engine must expose why a candidate was selected and why alternatives were rejected.

## 2. Inputs

```text
raw medicine-line text
normalized medicine-line text
OCR token boxes
recognized identifier(s)
recognized product name
strength value + unit
dosage form
manufacturer
ingredient text
OCR quality flags
layout classification
public DB version
```

## 3. Candidate retrieval order

1. Exact item code / standard code / supported insurance code.
2. Exact normalized product name.
3. Exact alias plus strength and dosage form.
4. FTS/trigram-like normalized name search.
5. Token-level retrieval for broken OCR strings.
6. Manual local search.

Every retrieved candidate must be filtered by active/valid source status when the source exposes that field.

## 4. Hard contradiction rules

A candidate is rejected, regardless of fuzzy score, when:

- recognized item code points to another product;
- strength value conflicts after unit conversion;
- dosage form conflicts in a clinically meaningful way;
- release formulation conflicts (`서방`, `ER`, `CR`, etc.);
- route conflicts;
- manufacturer conflicts when the product name is non-unique and manufacturer is clearly recognized;
- a required compound-product suffix is absent/present inconsistently;
- source record is revoked or not eligible under the selected source policy.

A missing field is not a contradiction, but it lowers confidence.

## 5. Evidence scoring

The default configuration is stored in `config/confidence_policy.yaml`. Scores are policy, not model output.

Suggested components:

| Component | Maximum contribution |
|---|---:|
| exact supported identifier | terminal verified state |
| normalized name similarity | 0.50 |
| strength match | 0.20 |
| dosage form match | 0.12 |
| manufacturer match | 0.08 |
| ingredient/context match | 0.05 |
| OCR consensus/layout quality | 0.05 |

The implementation must not rely on weighted score alone. Hard gates and top-candidate margin apply.

## 6. Decision classes

### `VERIFIED_IDENTIFIER`

- supported identifier exactly matches one valid record;
- no recognized field contradicts it;
- still displayed on the mandatory review screen.

### `HIGH_CONFIDENCE_REVIEW`

- exact or near-exact name;
- strength and dosage form confirmed;
- no contradiction;
- score and top-two margin exceed policy;
- user confirmation required.

### `AMBIGUOUS`

- multiple plausible candidates;
- missing strength or formulation;
- low top-two margin;
- candidate list presented with evidence.

### `UNRESOLVED`

- no safe candidate;
- conflicting evidence;
- unsupported medicine;
- no candidate is preselected.

Do not use labels such as “confirmed by AI.”

## 7. Candidate result contract

```kotlin
data class DrugMatchResult(
    val status: MatchStatus,
    val selectedCandidate: DrugCandidate?,
    val candidates: List<DrugCandidateScore>,
    val recognizedFields: RecognizedDrugFields,
    val hardRejections: List<RejectionReason>,
    val policyVersion: String,
    val publicDbVersion: String
)
```

## 8. User correction

User actions are local:

- edit recognized name/strength/form;
- choose one official candidate;
- mark unresolved;
- rescan;
- report unsupported template without attaching health data by default.

User corrections must not silently train or alter the matcher. Any future learning workflow requires explicit consent, de-identification, governance, and separate review.

## 9. Search index

Recommended indexes:

- exact canonical normalized name;
- alias normalized name;
- item code;
- standard/insurance code mapping;
- manufacturer normalized name;
- FTS5 index over product name, aliases, ingredients, manufacturer;
- numeric strength columns separate from display text;
- dosage form enum.

Name normalization dictionaries are versioned and covered by regression tests.

## 10. Safety invariant tests

Property tests must enforce:

- a strength conflict can never produce `VERIFIED_IDENTIFIER` or `HIGH_CONFIDENCE_REVIEW`;
- a dosage-form conflict can never produce a high-confidence state;
- increasing fuzzy name similarity cannot override a hard contradiction;
- missing evidence may lower confidence but cannot be auto-filled;
- the same inputs and policy version always produce the same result;
- a stale or incompatible DB cannot be queried as current.
