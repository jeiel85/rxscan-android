# RxScan Android

Privacy-first Android engineering project for local Korean dispensing-bag OCR and official public medicine-data lookup.

[Landing page](https://jeiel85.github.io/rxscan-android/) · [개인정보 처리방침 / Privacy Policy](https://jeiel85.github.io/rxscan-android/privacy.html) · [Download test build](https://github.com/jeiel85/rxscan-android/releases/latest) · [Engineering bootstrap](docs/ENGINEERING_BOOTSTRAP.md)

![RxScan landing hero](docs/assets/landing-hero.png)

## Status

All nine implementation goals (00–08) are complete. The app does on-device Korean dispensing-bag OCR (bundled ML Kit), deterministic fail-closed medicine matching, a mandatory review flow, official-source display, local DUR safety wording, and encrypted private storage, backed by a signed, reproducible public drug database built by the Python data-builder. No accounts, analytics, ads, or patient-data backend.

**This is an engineering preview / internal test build — not cleared for production or clinical use.** Pharmacist sign-off, regulatory review, a real validation holdout, and a security pentest are open launch blockers tracked in `config/release_gates.json`; the release gate blocks production automatically.

Package ID `io.github.jeiel85.rxscan`, current build v0.1.0 (versionCode 2).

### Download / test

Latest test build (signed APK, Android 8.0+): [Releases](https://github.com/jeiel85/rxscan-android/releases/latest). Use synthetic data only — do not photograph a real prescription.

## Safety boundaries

- Prescription images, OCR text, medicine selections, and saved history must not be uploaded.
- Medicine identification and parsing must be deterministic and fail closed.
- A medicine review step is mandatory before finalization.
- Official general information and photographed prescription directions must stay separate.
- User-facing medical content must be official text, a defined template, or pharmacist-reviewed fixed copy.
- Generative models must not identify medicines, parse dose, evaluate DUR, or write medical conclusions.
- Missing medicine fields must not be guessed.

## Repository layout

```text
apps/android/                 Android app and modular feature/data/engine/core modules
tools/drug-data-builder/       Typed Python package for official-data pipeline work
infra/data-distribution/       Distribution schemas and future runbooks
docs/                          GitHub Pages, public docs, and engineering notes
testdata/                      Synthetic-only development fixtures
config/                        Versioned policy and source registry YAML
schemas/                       Canonical public data and scan result schemas
prompts/                       Goal-by-goal implementation prompts
```

## Implemented (Goals 00–08)

- **Data builder (Python)**: MFDS source registry + paged fetcher (timeout/retry, schema/page checks, redacted URLs); reproducible signed public SQLite DB with an Ed25519 manifest and independent verification; rollback/revocation design. Official API access requires data.go.kr `활용신청` — see [docs/MFDS_DATA_ACCESS.md](docs/MFDS_DATA_ACCESS.md).
- **Scan**: CameraX preview/capture, document-boundary overlay, image-quality gate, bundled offline Korean OCR, perspective transform, temp-file lifecycle.
- **Parser / matcher**: deterministic row clustering, field extraction, and direction grammar; fail-closed matcher with a hard-contradiction engine and policy scoring (VERIFIED / HIGH / AMBIGUOUS / UNRESOLVED), property-tested.
- **Review / source UI**: mandatory review (no bypass), photographed direction vs official information separation, source/date/version visibility, non-color confidence semantics.
- **DUR**: confirmed-medicine-only local evaluation with fixed safety wording (never stop/change/double), stale-data handling.
- **Security**: Keystore-wrapped private-DB key (fail-closed), redacted diagnostics, delete-all / key-invalidation, network/backup hardening, FLAG_SECURE.
- **Validation / release**: synthetic holdout benchmark, automatic release-gate blocking, and the compliance document set under `docs/`.

Safety-critical logic is pure Kotlin and JVM-tested; CameraX/ML Kit/Keystore/Compose are device-only wrappers. Device-only acceptance (airplane-mode OCR, TalkBack, on-device backup/MITM, real holdout) is implemented but verified outside the JVM CI.

## Local verification

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:assembleDebug
python -m unittest discover -s tools/drug-data-builder/tests
npx --yes pyright tools/drug-data-builder
python tools/ci/check_repository_policy.py
$env:PYTHONPATH = "tools/drug-data-builder/src"
python -m rxscan_data list-sources
python -m rxscan_data build-fixture --source mfds_easy_drug --operation getDrbEasyDrugList --fixture tools/drug-data-builder/fixtures/synthetic/mfds_easy_drug_minimal.json --out tools/drug-data-builder/out/fixture-smoke
```

## Design source

Start with:

- [AGENTS.md](AGENTS.md)
- [00_MASTER_PLAN.md](00_MASTER_PLAN.md)
- [01_PRODUCT_REQUIREMENTS.md](01_PRODUCT_REQUIREMENTS.md)
- [02_SYSTEM_ARCHITECTURE.md](02_SYSTEM_ARCHITECTURE.md)
- [07_SECURITY_PRIVACY.md](07_SECURITY_PRIVACY.md)
- [09_TEST_QUALITY.md](09_TEST_QUALITY.md)
- [prompts/GOAL_00_REPOSITORY_BOOTSTRAP.md](prompts/GOAL_00_REPOSITORY_BOOTSTRAP.md)

The original bundle README is preserved at [docs/design/README.design-bundle.md](docs/design/README.design-bundle.md).

## License

License selection is pending. Do not assume redistribution rights for official source data until each source's terms are re-verified and documented.
