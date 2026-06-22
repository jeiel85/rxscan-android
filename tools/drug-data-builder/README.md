# RxScan Data Builder

This package is the offline pipeline that turns official public drug datasets
into a single **signed, reproducible** public SQLite database plus a signed
manifest. It runs in CI, independently of any user scan, and never embeds public
API keys in the Android app.

Allowed in this package:

- schema and policy contract checks;
- synthetic API-shaped fixtures;
- deterministic local commands;
- public SQLite build, manifest generation, Ed25519 signing, and independent
  verification;
- a local static HTTP server fixture for Android integration testing;
- tests that prove secrets and patient data are not required.

Not allowed in this package:

- committed public API keys or private signing keys;
- real prescription images or OCR text;
- generated medical summaries;
- scan-derived server queries.

## Commands

```powershell
python -m pip install -e tools/drug-data-builder
python -m unittest discover -s tools/drug-data-builder/tests
npx --yes pyright tools/drug-data-builder
$env:PYTHONPATH = "tools/drug-data-builder/src"
python -m rxscan_data --describe
python -m rxscan_data list-sources
python -m rxscan_data build-fixture `
  --source mfds_easy_drug `
  --operation getDrbEasyDrugList `
  --fixture tools/drug-data-builder/fixtures/synthetic/mfds_easy_drug_minimal.json `
  --out tools/drug-data-builder/out/fixture-smoke
```

Live MFDS fetches require a data.go.kr utilization request and a decoded public-data portal `ServiceKey`:

```powershell
$env:PYTHONPATH = "tools/drug-data-builder/src"
$env:DATA_GO_KR_SERVICE_KEY = "<decoded-service-key>"
python -m rxscan_data fetch `
  --source mfds_easy_drug `
  --operation getDrbEasyDrugList `
  --max-pages 1 `
  --out tools/drug-data-builder/out/live-smoke
```

The fetcher redacts `ServiceKey` from stored request URLs and reports. Do not add live output directories to commits.

## Build, sign, and verify the public database

`build-public-db` reads the `normalized/records.jsonl` files produced by `fetch`
or `build-fixture`, builds the public SQLite database from
`schemas/drug_db_schema.sql`, runs `quick_check` and foreign-key checks, writes a
conflict/quarantine report, and produces a canonical `manifest.json` signed with
Ed25519.

The private signing key is read from an environment variable as raw 32-byte
Ed25519 key material in base64. It is **never** written to the repository or to
any build output.

```powershell
$env:PYTHONPATH = "tools/drug-data-builder/src"
# Provide a 32-byte raw Ed25519 private key as base64 (kept out of the repo).
$env:RXSCAN_DATASET_SIGNING_PRIVATE_KEY_B64 = "<base64-raw-ed25519-private-key>"
python -m rxscan_data build-public-db `
  --input tools/drug-data-builder/out/fixture-smoke `
  --out tools/drug-data-builder/out/public-db `
  --artifact-version 20260623-1 `
  --created-at-utc 2026-06-23T00:00:00Z `
  --signing-key-id rxscan-dataset-2026-06
```

Independent verification checks the signature and every artifact hash/size. It
needs only the **public** key, mirroring what the Android client does after
download:

```powershell
python -m rxscan_data verify-public-db `
  --manifest tools/drug-data-builder/out/public-db/manifest.json `
  --signature tools/drug-data-builder/out/public-db/manifest.sig `
  --artifact-root tools/drug-data-builder/out/public-db `
  --public-key-base64 "<base64-raw-ed25519-public-key>"
```

### Reproducibility and the compression format

The same normalized input produces a byte-for-byte identical **uncompressed**
SQLite file (`PRAGMA journal_mode = OFF`, fixed page size, `VACUUM`, no wall-clock
or random inputs). `05_DATA_PLATFORM.md` §7 lists Zstandard (`.zst`) as the
recommended compressed artifact, but Zstandard is not in the Python standard
library. To keep the builder dependency-light (see `docs/DEPENDENCY_POLICY.md`),
this bootstrap ships the documented canonical alternative: **gzip written with a
zeroed mtime and empty filename field**, which is also byte-for-byte
deterministic. The manifest records both compressed and uncompressed SHA-256
hashes and sizes, so a one-byte change to either the artifact or the manifest
fails verification.

## Local static-server fixture for Android integration

`serve-public-db` serves a public-DB output directory over a local static HTTP
server so the Android client can exercise its real download → verify path against
synthetic data. Verification of the downloaded copy is fully offline.

```powershell
python -m rxscan_data serve-public-db --dir tools/drug-data-builder/out/public-db --port 8765
# Android (or curl) downloads manifest.json, manifest.sig, and artifacts/*.gz,
# then verifies the signature/hashes offline against the embedded public key.
```

`copy_public_dataset_to_static_dir` lays out the same directory structure
(`manifest.json`, `manifest.sig`, `artifacts/<name>.sqlite.gz`) and is covered by
`tests/test_public_db.py`, which serves the fixture over HTTP, downloads it, and
verifies it (including a tamper-detection assertion).

## Easy-information (`e약은요`) text handling

`e약은요` text is stored verbatim in this bootstrap (`sanitizer_version =
verbatim-passthrough-v1`): only surrounding whitespace is trimmed and no HTML
transformation is applied. Missing easy-information content stays null and is
never generated. An allowlist HTML sanitizer must be added — and the
`sanitizer_version` bumped — before ingesting live `e약은요` HTML, per
`05_DATA_PLATFORM.md` §5.

## Normalized fields deferred and known bootstrap limitations

These are deliberate, documented limitations of this bootstrap — not silent gaps:

- **`strength_value` / `strength_unit` / `dosage_form` / `route` are stored
  NULL.** The structured approval-detail source that carries these fields is not
  ingested yet, and strength is never parsed from the free-text product name
  because `AGENTS.md` forbids guessing a missing medicine field. These columns are
  populated when the structured approval-detail source is wired in (with covering
  fixtures), not before.
- **The conflict/quarantine report covers product-name conflicts.** When the same
  item code appears with an incompatible product name, the losing record is
  excluded and listed in `conflict-report.json`. Easy-info, DUR, and ingredient
  rows currently resolve duplicate keys by deterministic last-write-wins (stable
  file/line order) without a per-table conflict entry. Broadening typed conflict
  detection to those tables is follow-up work.
- **Full-text search uses the `unicode61` tokenizer.** A whitespace-delimited
  Korean run is one token, so search matches whole tokens, not arbitrary
  substrings. Substring/trigram search (and the app query layer that would append
  prefix wildcards) belongs to the later search-UI goal, not this build goal.

## Rollback and revocation

Bad or compromised artifacts are recovered through the signed manifest
(`revokedVersions`, `emergencyMessageId`, freshness, app-version range, and
monotonic `artifactVersion`). See `docs/ROLLBACK_AND_REVOCATION.md` for the
design and `checklists/DATA_UPDATE_RUNBOOK.md` for the operational procedure.
