# Public Dataset Rollback and Revocation Design

This document describes how RxScan recovers from a bad or compromised public drug
database. It complements the operational steps in
`checklists/DATA_UPDATE_RUNBOOK.md` and the platform overview in
`05_DATA_PLATFORM.md` (§8–§11). The binding manifest shape lives in
`schemas/dataset_manifest.schema.json`.

## Goals

- The app must never activate an unsigned, invalid, or revoked public DB
  (`AGENTS.md` absolute constraints).
- Recovery must not depend on trusting the artifact host: a compromised host
  alone must not be able to push or keep a bad DB active.
- Clients must converge to a safe state — either the last verified-good DB or a
  clearly-dated read-only state — without fabricating freshness.
- Every rollback/revocation decision is authenticated by the dataset signing key,
  not by transport security alone.

## Manifest controls

All controls are fields of the signed `manifest.json`. Because the manifest is
covered by the Ed25519 signature, none of them can be forged by a host that does
not hold the private key.

| Field | Type | Role in rollback/revocation |
| --- | --- | --- |
| `artifactVersion` | `YYYYMMDD-N` | Monotonic identity of a build. Corrections are published as a strictly newer version, never by mutating an existing one. |
| `revokedVersions` | `string[]` (unique) | Versions the client must refuse to keep active, even if already installed. |
| `freshness` | `CURRENT` / `WARNING` / `STALE` | Source-age signal that gates how DUR/easy-info output may be presented. |
| `minAppVersion` / `maxAppVersion` | string / nullable string | Compatible app-version range; an app outside the range must not activate the artifact. |
| `emergencyMessageId` | nullable string | Identifier of a fixed, pre-translated in-app message (e.g. "temporarily withdrawn"). Never free-text medical wording. |
| `signingKeyId` | string | Which public verification key signed this manifest; enables key rotation. |
| `artifact.compressedSha256` / `uncompressedSha256` / sizes | string / int | Integrity binding; any one-byte change to artifact or manifest fails verification. |

## Builder / publisher procedure

Revocation and rollback are publishing actions, performed by the offline builder,
never by mutating already-published bytes.

### Revoke a bad version

1. Add the bad `artifactVersion` to `revokedVersions`.
2. Re-point the active manifest at the last verified-good artifact (or a corrected
   newer artifact — see below).
3. Re-sign the manifest with the current signing key and publish it.
4. Retain at least two rollback artifacts so the previous good version stays
   downloadable (`05_DATA_PLATFORM.md` §10).

### Publish a correction

1. Build a strictly newer `artifactVersion` (monotonic; never reuse or edit an
   old version).
2. Keep the bad version listed in `revokedVersions` so stale clients still
   distrust it.
3. Sign, upload to staging, re-download, and independently verify before
   promoting the manifest pointer (`verify-public-db`).

### Signing-key concern

If the signing key may be compromised, stop publication, freeze the last
known-good manifest, and invoke the key-compromise rotation drill. Rotation ships
an app release that trusts both the old and new public keys (`signingKeyId`
distinguishes them); a bad host cannot downgrade trust on its own. Unsigned
"emergency" artifacts are never accepted.

## Client (Android) behavior

The client verifies the signature and all hashes offline against its embedded
public key(s) before any activation. After verification it applies the controls:

1. **App-version range** — if the running app is outside
   `[minAppVersion, maxAppVersion]`, do not activate; keep the previous verified
   DB and prompt the user to update the app.
2. **Revocation** — if the currently-active `artifactVersion` appears in a newly
   verified manifest's `revokedVersions`, the client must stop treating it as a
   current safety source and roll back to the last verified-good version. If no
   safe version remains, it falls back to a clearly-dated read-only state and must
   not present DUR output as a current safety check.
3. **Monotonic guard** — never replace an installed version with an older
   `artifactVersion`; this blocks downgrade attacks via a manipulated host.
4. **Freshness** — gate presentation by `freshness` (`STALE` ⇒ historical
   information may remain visible with its date, DUR is not labeled current,
   prompt to update; never fabricate freshness).
5. **Emergency message** — if `emergencyMessageId` is set, show the corresponding
   fixed, pre-translated message; never synthesize medical wording.

Failure is always closed: any verification, range, or revocation failure leaves
the app on the previous safe state rather than activating questionable data.

## Invariants

- Rollback/revocation state is only ever changed through a freshly signed
  manifest; transport (HTTPS, CDN) is not a trust anchor.
- Published artifact bytes are immutable; corrections are always a newer
  `artifactVersion`.
- The private signing key is never written to the repository or to build output
  (enforced by `tests/test_public_db.py`).
- At least two rollback artifacts are retained so a roll back target always
  exists.

## References

- `05_DATA_PLATFORM.md` — §8 manifest, §9 signing, §10 schedule, §11 freshness.
- `checklists/DATA_UPDATE_RUNBOOK.md` — "On bad artifact" and "On signing-key
  concern" operational steps.
- `schemas/dataset_manifest.schema.json` — manifest contract.
- `tools/drug-data-builder/README.md` — `build-public-db` / `verify-public-db` /
  `serve-public-db` usage.
