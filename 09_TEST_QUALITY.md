# Test and Quality Plan

## 1. Test pyramid

### Unit

- normalization dictionaries;
- unit conversion;
- direction grammar;
- row clustering;
- hard contradiction rules;
- candidate scoring;
- freshness policy;
- manifest canonicalization and signature verification.

### Property and fuzz

- malformed Korean Unicode;
- mixed digits and units;
- extreme image dimensions;
- parser determinism;
- no high-confidence result after any hard contradiction;
- decompression/path traversal boundaries;
- SQL query parameterization.

### Integration

- CameraX capture fixture;
- bundled OCR availability offline;
- OCR-to-parser;
- parser-to-matcher;
- DB update and rollback;
- encrypted DB key lifecycle;
- process death and session recovery.

### UI

- mandatory review cannot be bypassed;
- ambiguous candidate behavior;
- source/date visibility;
- large font and TalkBack;
- stale/revoked DB banners;
- delete-all flow.

### Field validation

- pharmacy-format holdout set;
- device/OEM matrix;
- lighting, glare, crumple, thermal-print fading;
- user confirmation usability;
- pharmacist review.

## 2. Corpus governance

### Sources

- synthetic dispensing-bag generator;
- consented real images with documented purpose;
- fully de-identified derived fixtures;
- images photographed across supported devices and conditions.

### Rules

- no real prescription in source control;
- consent record separate from image;
- access limited to authorized QA;
- retention period documented;
- patient identifiers removed before annotation where possible;
- holdout split by pharmacy/template, not random image only;
- delete corpus material when consent/retention expires.

## 3. Annotation schema

For each image:

- supported/unsupported layout;
- quality flags;
- document polygon;
- OCR ground-truth lines;
- medicine rows;
- raw printed medicine string;
- official product item code;
- strength/form/manufacturer;
- prescription direction fields;
- expected candidate class;
- required abstention reason.

Double annotation and adjudication are required for safety-critical fields.

## 4. Metrics

### Capture

- quality-gate false accept;
- quality-gate false reject;
- rescan completion rate.

### OCR

- character error rate;
- line exact match;
- digits/units exact match;
- medicine-name exact match.

### Parser

- field exact match;
- complete medication-row exact match;
- direction-structure exact match;
- conflict-detection recall.

### Matcher

- verified/high-confidence precision;
- coverage;
- top-3 recall for ambiguous cases;
- wrong confirmed match rate;
- unresolved rate;
- performance by pharmacy template and product frequency.

### UX

- user correction rate;
- time to confirmed result;
- unresolved selections;
- source disclosure comprehension;
- accidental confirmation rate.

## 5. Proposed production release gates

These are target gates and must be reviewed with clinical/QA advisors.

### Safety-critical matching

For the supported holdout domain:

- zero wrong `VERIFIED_IDENTIFIER` or `HIGH_CONFIDENCE_REVIEW` selections in at least 3,000 independently held-out accepted medicine lines;
- include at least 500 unique official products and at least 30 materially different bag templates;
- no hard-contradiction invariant failure;
- top-3 candidate recall ≥ 99.5% for resolvable ambiguous lines;
- all unresolved cases remain explicitly unresolved.

A zero-error test does not prove zero real-world errors. Report confidence intervals and corpus limitations.

### Direction parsing

- exact match ≥ 99.5% on accepted scans for supported grammar;
- any disagreement involving amount, frequency, or duration must be highlighted for review;
- no silent defaulting of missing values.

### Privacy/security

- no prescription bytes or OCR values in captured network traffic;
- no sensitive files in backup/transfer tests;
- no sensitive values in release logs;
- tampered manifest/database rejected;
- private DB unreadable without key;
- all temporary scans cleaned after documented lifecycle events.

### Reliability

- update interruption leaves previous DB active;
- rollback drill succeeds;
- crash-free internal beta target established and measured without collecting health payloads;
- no ANR in scan pipeline benchmark matrix.

## 6. Device matrix

At minimum:

- low/mid/high Android devices;
- Samsung, Google, and at least one additional OEM;
- Android versions covering supported min SDK through current;
- small phone, large phone, tablet;
- cameras with different sensor aspect ratios;
- Korean locale and large-font/TalkBack configurations.

## 7. Regression policy

Every production incident, rejected field case, or approved-template change must create:

- a de-identified or synthetic regression fixture;
- an expected parser/matcher result;
- a documented root cause;
- a policy/version change when applicable.

Do not tune thresholds against the production holdout set.
