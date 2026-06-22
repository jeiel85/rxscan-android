# System Architecture

## 1. Architecture overview

The system has two trust domains:

1. **User device domain** — contains prescription images, OCR text, user confirmations, and optional history.
2. **Public-data build domain** — fetches public official drug data and publishes generic signed artifacts.

No patient information crosses from the first domain into the second.

## 2. Android module graph

```text
:app
├─ :feature:home
├─ :feature:scan
├─ :feature:review
├─ :feature:drugdetail
├─ :feature:safety
├─ :feature:history
├─ :feature:settings
├─ :engine:imagequality
├─ :engine:document
├─ :engine:ocr
├─ :engine:parser
├─ :engine:matcher
├─ :engine:dur
├─ :data:publicdb
├─ :data:privatedb
├─ :data:updater
├─ :core:model
├─ :core:security
├─ :core:ui
├─ :core:logging
└─ :core:testing
```

### Dependency rules

- Feature modules depend on domain interfaces, not database implementations.
- OCR and parser modules do not depend on Compose.
- Matcher consumes normalized domain models and a read-only search interface.
- Public and private databases are separate physical files.
- Private data never enters public DB search telemetry.
- `:core:logging` rejects sensitive value types at compile-time/API boundary where feasible.

## 3. Layer responsibilities

### Presentation

- Compose screens and state holders;
- camera permission and capture interaction;
- review/confirmation flow;
- source disclosure and warnings;
- accessibility semantics.

### Domain

- scan-session state machine;
- medicine-line parsing contracts;
- candidate scoring policy;
- confirmation and abstention rules;
- freshness policy;
- DUR wording policy.

### Data

- public read-only drug database;
- encrypted private prescription store;
- signed artifact updater;
- source links and provenance lookup.

### Platform

- CameraX;
- on-device OCR;
- Android Keystore;
- biometric/device credential;
- WorkManager;
- file lifecycle and secure deletion best effort.

## 4. Scan session state machine

```text
IDLE
  → CAPTURING
  → QUALITY_CHECKED
  → PREPROCESSING
  → OCR_RUNNING
  → PARSING
  → MATCHING
  → REVIEW_REQUIRED
  → CONFIRMED
  → FINALIZED

Any active state
  → CANCELLED
  → temp files deleted

Any processing state
  → FAILED_RECOVERABLE
  → retry/correct/import

Security or integrity failure
  → FAILED_CLOSED
  → no medical result displayed
```

Invalid transitions must be rejected and tested.

## 5. Public database update flow

1. WorkManager downloads `manifest.json` from a fixed HTTPS host.
2. The app validates JSON schema and compatibility range.
3. The app verifies the manifest’s Ed25519 signature using an embedded public key.
4. If a newer version is available, download to an app-private staging directory.
5. Verify file size and SHA-256.
6. Open the SQLite file read-only and run integrity checks:
   - `PRAGMA quick_check`;
   - expected schema version;
   - required source snapshots;
   - non-zero core table counts;
   - referential integrity.
7. Atomically switch the active-version pointer.
8. Retain the previous two verified versions.
9. Delete failed staging artifacts.
10. Never activate an unsigned, incompatible, or corrupt artifact.

## 6. Network policy

Allowed production network operations:

- download generic public database manifest/artifacts;
- open an official source URL by explicit user action;
- Play Store platform services required by distribution.

Forbidden:

- upload prescription image;
- upload OCR text;
- server-side medicine search based on scan content;
- analytics events containing medicines, conditions, pharmacies, dates, or scan identifiers;
- remote feature flags that can alter medical wording or matching thresholds without an app release and review.

Use a Network Security Configuration that disables cleartext traffic and, if operationally feasible, limits app-owned requests to the artifact host.

## 7. Build variants

| Variant | Purpose | Network | Test data |
|---|---|---|---|
| `debug` | local development | local/mock endpoints allowed | synthetic only by default |
| `qa` | internal field testing | staging artifacts | consented de-identified corpus |
| `release` | public | production artifact host | none bundled except synthetic fixtures |
| `offlineRelease` | optional audit build | disabled after bundled DB | none |

No real prescription should be committed to Git.

## 8. Dependency policy

- Pin dependencies with version catalogs and dependency locking.
- Generate an SBOM for release builds.
- Review licenses for bundled OCR, OpenCV, SQLCipher-equivalent, and other native libraries.
- Block dependencies with ad/analytics transitive SDKs.
- Use reproducible CI builds where practical.
- Run static analysis, vulnerability scanning, and secret scanning on every release branch.
