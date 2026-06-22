# Goal 00 — Repository Bootstrap

Create the production-oriented monorepo skeleton described in `12_PROJECT_STRUCTURE.md`.

## Deliverables

- Android Gradle project with modular placeholders:
  - app, core, data, engine, and feature groups;
- Kotlin/Compose app launches to a non-medical placeholder home;
- version catalog and dependency locking;
- baseline formatting, lint, unit test, and CI;
- Python typed package for `tools/drug-data-builder`;
- secret scanning and dependency/SBOM placeholders;
- root `README.md`, `SECURITY.md`, and copied `AGENTS.md`;
- synthetic-only testdata policy;
- no Internet permission until the updater goal actually requires it.

## Decisions

- Android min SDK 26.
- Compile/target SDK: latest stable/current release requirement at implementation time.
- Package ID remains a documented placeholder until the owner selects it.
- No medical content or OCR implementation in this goal.

## Acceptance criteria

- clean checkout builds;
- Android unit tests run;
- Python tests and type checks run;
- CI runs both;
- no secret or real prescription fixture;
- module dependency rules are documented;
- dependency report shows no analytics/ad SDK.

General constraints:
- Follow `AGENTS.md`.
- Do not add analytics, accounts, ads, cloud OCR, or patient-data endpoints.
- Use synthetic test data.
- Do not begin the next goal.
- Report changed files, test commands, results, and unresolved risks.
