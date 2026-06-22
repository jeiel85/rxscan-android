# Master Plan

## 1. Product definition

RxScan is an information-retrieval and prescription-organization application. It is not a diagnosis tool, a prescription engine, a dosage calculator, or a substitute for a pharmacist.

The application:

- captures a Korean pharmacy dispensing bag;
- performs image correction and Korean OCR entirely on-device;
- extracts medicine lines and prescription directions;
- resolves each medicine against a locally stored, signed official-data database;
- requires a review step before medicine details are treated as confirmed;
- presents official efficacy, use, cautions, interactions, adverse reactions, storage, and DUR notices;
- clearly separates the photographed prescription directions from general approved information.

## 2. Product boundaries

### Supported in the first public release

- Android phones and tablets;
- Korean printed pharmacy dispensing bags;
- images captured in-app or imported by explicit user action;
- printed medicine names, strength, dosage form, manufacturer, item/insurance codes when present;
- oral medication schedules expressed with common Korean pharmacy phrases;
- official domestic medicines represented in the selected MFDS/HIRA datasets;
- offline use after the public drug database is installed.

### Explicitly unsupported in the first public release

- handwritten prescriptions;
- identifying pills from appearance alone;
- bags that contain only unlabeled one-dose sachets;
- foreign medicines;
- herbal prescriptions and health supplements;
- pediatric dose calculation;
- renal/hepatic dose adjustment;
- deciding whether a user should stop, skip, double, or change a dose;
- detecting all possible interactions outside the loaded official DUR scope;
- interpreting free-form physician notes;
- diagnosis, triage, emergency assessment, or treatment recommendations.

## 3. Delivery architecture

```text
Official public APIs
        │
        ▼
CI Data Builder ── validate ── normalize ── provenance ledger
        │
        ▼
Read-only SQLite + manifest + SHA-256 + Ed25519 signature
        │
        ▼
Static object storage / CDN
        │ generic files only
        ▼
Android updater ── verify signature ── atomic database swap

Dispensing-bag image
        │ never uploaded
        ▼
Camera quality gate → perspective correction → local OCR
        ▼
layout parser → medication parser → candidate retrieval
        ▼
deterministic scoring + hard contradiction rules
        ▼
user review and confirmation
        ▼
official information + source metadata + DUR notices
```

## 4. Critical design decisions

- Native Android is the first platform because CameraX, local OCR, file lifecycle, secure storage, and accessibility can be controlled directly.
- Public API keys are held only in CI. They are never embedded in the APK.
- The app has no patient-data backend.
- The official database is public data and read-only; user scan/history data is private and encrypted separately.
- A signed manifest controls version, checksums, source snapshots, minimum compatible app version, and emergency revocation.
- A medicine is never auto-resolved when strength or dosage form conflicts.
- A user-facing review screen is mandatory even for high-confidence recognition.
- Medical wording is either official source text or pharmacist-reviewed fixed content.
- No LLM or generative model participates in a safety-critical path.

## 5. Quality strategy

Accuracy is measured only on the explicitly supported domain. The application may reject difficult images or unresolved medicine lines.

Primary safety metric:

> Wrong confirmed medicine matches per accepted medicine line.

Coverage is secondary. The system is allowed to abstain.

## 6. Release phases

| Phase | Deliverable | Exit condition |
|---|---|---|
| P0 | Data feasibility | Stable item-code joins and source provenance verified |
| P1 | Camera/OCR lab build | Printed text and bounding boxes available offline |
| P2 | Parser/matcher alpha | Deterministic candidates and contradiction rules pass fixture tests |
| P3 | Private beta | Review UX, source cards, encrypted history, signed DB updates |
| P4 | Safety beta | Pharmacist-reviewed copy, DUR handling, field test corpus |
| P5 | Production candidate | Release gates, legal review, Play declarations, rollback drill |
| P6 | Production | Monitored data freshness and controlled supported-format expansion |

## 7. Decisions that require external professional review

Before production, obtain written review for:

- whether the final intended use and claims trigger Korean medical-device or digital-medical-product regulation;
- privacy policy and sensitive-data handling;
- consumer-facing medical copy and DUR presentation;
- public-data attribution and redistribution conditions;
- incident and user-support procedures.

This bundle is an engineering specification, not legal or medical certification.
