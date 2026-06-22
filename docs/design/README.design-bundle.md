# RxScan Design Bundle

> Working title only. Rename the product and package before public release.

RxScan is an Android-first, privacy-first application that reads Korean pharmacy dispensing bags on-device, asks the user to confirm recognized medicines, and displays official medicine information with explicit provenance.

## Non-negotiable principles

1. Prescription images and OCR text never leave the device.
2. The app never guesses a medicine when essential fields conflict or are missing.
3. Prescription-specific directions and general approved drug information are displayed separately.
4. Every official-information card exposes its agency, dataset, item code, source update date, and local database version.
5. Generative AI is excluded from medicine identification, dosage parsing, safety-rule evaluation, and user-facing medical conclusions.
6. A low-confidence result is an abstention, not an approximate answer.
7. The first release supports only declared Korean printed dispensing-bag formats.

## Recommended stack

- Android: Kotlin, Jetpack Compose, CameraX, Room, WorkManager
- OCR: bundled on-device Korean ML Kit Text Recognition v2
- Image processing: OpenCV Android or an equivalent audited local implementation
- Private storage: encrypted private database with a Keystore-wrapped database key
- Public medicine database: signed read-only SQLite asset with FTS indexes
- Data builder: Python CLI, run in CI; no patient data is processed
- Distribution: static manifest and signed database artifacts through Cloudflare R2 or equivalent object storage
- Architecture: modular Clean Architecture with unidirectional data flow

## Repository layout

```text
rxscan/
├─ apps/android/
├─ tools/drug-data-builder/
├─ infra/data-distribution/
├─ docs/
├─ testdata/
│  ├─ synthetic/
│  └─ consented-deidentified/
├─ AGENTS.md
└─ README.md
```

## Reading order

1. `00_MASTER_PLAN.md`
2. `01_PRODUCT_REQUIREMENTS.md`
3. `02_SYSTEM_ARCHITECTURE.md`
4. `03_OCR_PIPELINE.md`
5. `04_DRUG_MATCHING_ENGINE.md`
6. `05_DATA_PLATFORM.md`
7. `07_SECURITY_PRIVACY.md`
8. `09_TEST_QUALITY.md`
9. `prompts/GOAL_00_REPOSITORY_BOOTSTRAP.md`

## First command for a coding agent

Give the agent `AGENTS.md`, then execute the prompts in `prompts/` in numerical order. A goal is complete only when its acceptance criteria pass. Do not start the next goal to hide failures in the current one.
