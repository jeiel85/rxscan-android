# Official Data Platform

## 1. Why a build pipeline is required

The Android app must not embed public API service keys. It also should not send a recognized medicine name to a server. A CI data builder fetches official public datasets independently of any user scan, normalizes them, records provenance, and publishes a generic signed SQLite database.

## 2. Initial official sources

See `14_SOURCE_REGISTRY.md` and `config/source_registry.yaml`.

Primary initial sources:

- MFDS medicine product approval information;
- MFDS easy drug information (`e약은요`);
- MFDS DUR product information;
- MFDS DUR ingredient information;
- optional HIRA product/code mapping after field and redistribution verification;
- optional MFDS pill-identification data as a secondary manual aid, never sole confirmation.

`e약은요` is not assumed to cover all prescription medicines. Missing easy-information content is represented as missing, not generated.

## 3. Pipeline stages

```text
fetch
→ immutable raw snapshot
→ schema validation
→ canonical normalization
→ source-specific staging tables
→ join and conflict report
→ quality gates
→ final SQLite build
→ provenance ledger
→ manifest generation
→ checksum
→ signature
→ publication
```

## 4. Raw snapshot policy

For every source run store:

- source ID;
- official dataset name;
- endpoint;
- request date/time in UTC;
- source-provided update fields;
- request parameters excluding secrets;
- page counts and record counts;
- raw body checksum;
- builder version;
- success/failure status.

Raw data is retained in restricted CI artifact storage according to data-source conditions. Never commit service keys or full raw snapshots to a public repository without permission review.

## 5. Data normalization

### Product

Canonical key: MFDS item sequence/item code where available.

Normalize separately:

- display product name;
- search product name;
- manufacturer display/search form;
- strength numeric value and UCUM-like internal unit;
- dosage form enum;
- professional/general classification;
- active status;
- source dates.

### Text fields

Official HTML/text is sanitized through an allowlist. Store:

- original source payload;
- sanitized display text;
- source field name;
- source update date;
- sanitizer version;
- content checksum.

Do not paraphrase within ETL.

## 6. Conflict handling

The build fails or quarantines records when:

- one canonical item code maps to incompatible product names;
- strength parsing conflicts across sources;
- duplicate identifiers map to multiple active products without an explicit valid relationship;
- required source fields disappear due to API schema change;
- source record count falls beyond an approved threshold;
- DUR relationship endpoints cannot be resolved;
- an encoding failure corrupts Korean text.

Quarantined records are excluded from high-confidence matching and listed in the build report.

## 7. Database artifacts

Recommended artifacts:

```text
manifest.json
manifest.sig
rxscan-drugs-YYYYMMDD-N.sqlite.zst
rxscan-drugs-YYYYMMDD-N.sqlite.zst.sha256
build-report-YYYYMMDD-N.json
licenses-and-attribution-YYYYMMDD-N.json
```

The app may download a full file initially. Delta updates are a later optimization and must preserve signature/integrity guarantees.

## 8. Manifest

The manifest includes:

- artifact version;
- schema version;
- created time;
- compatible app version range, including an explicit nullable maximum app version;
- URL/path;
- compressed and uncompressed hashes;
- sizes;
- source snapshot list, including source endpoint and builder version;
- freshness status;
- revoked versions;
- emergency message ID, nullable when there is no active emergency message;
- signing key ID.

See `schemas/dataset_manifest.schema.json`.

Artifact paths must be relative, must not start with `/`, and must not contain
`..` path traversal segments. The app must still validate paths after schema
validation before writing any staged file.

## 9. Signing

Use Ed25519 signatures.

- Private signing key is never stored in the repository.
- Prefer a managed signing service/KMS or protected CI environment.
- App embeds only public verification keys.
- Support key rotation through a release that contains both old and new public keys.
- A compromised artifact host alone must not be sufficient to replace the DB.
- Run and document a key-compromise rotation drill before launch.

## 10. Schedule

- Fetch official sources daily.
- Publish only when source changes pass all quality gates.
- Create an alert after one failed run.
- Escalate after three consecutive failed runs or when active DB age exceeds the warning threshold.
- Maintain reproducible build reports and two rollback artifacts.

## 11. App freshness behavior

Suggested policy:

- `CURRENT`: source snapshot age ≤ 14 days;
- `WARNING`: 15–30 days;
- `STALE`: > 30 days;
- `REVOKED`: explicitly revoked.

When stale:

- basic historical official information may remain visible with a clear date;
- the app must not label DUR output as a current safety check;
- prompt the user to update;
- never fabricate freshness.
