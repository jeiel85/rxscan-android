# Security, Privacy, and Threat Model

## 1. Protected assets

Highest sensitivity:

- prescription image;
- OCR text;
- medicine list;
- dosage schedule;
- dates that reveal treatment;
- optional user notes;
- any patient identity accidentally visible in the image.

Integrity-sensitive public assets:

- medicine database;
- DUR rules;
- source dates;
- source links;
- matching policy;
- consumer wording.

## 2. Threat actors

- person with temporary access to an unlocked device;
- person with access to device backups;
- malicious or overprivileged app;
- compromised network/CDN;
- compromised CI account;
- malicious dependency;
- accidental developer logging;
- attacker submitting crafted images;
- ordinary OCR/matching error.

## 3. Mandatory controls

### Device storage

- app-private directories only;
- encrypted private database;
- Keystore-backed key protection;
- StrongBox preference only when supported and operationally justified;
- private data excluded from backup and device transfer unless a separately reviewed encrypted migration feature is built;
- no external shared storage for health data.

### Image lifecycle

- temporary file names contain no identity;
- use cache/no-backup storage;
- delete on cancellation, completion, timeout, and startup cleanup;
- no gallery insertion;
- no EXIF location retention;
- encrypted original retention only after explicit opt-in.

### UI leakage

- protect scan/review/detail screens from recents thumbnails;
- optional/default screenshot blocking according to usability testing;
- clear clipboard prohibition for sensitive values;
- redact notifications;
- no medicine name on lock-screen notifications;
- hide content when app moves to background if privacy mode is enabled.

### Network

- HTTPS only;
- generic artifact downloads;
- manifest signature verification;
- no scan-dependent request;
- no raw health data in URL/query/header;
- no third-party analytics, ads, or remote crash attachment;
- dependency inspection for hidden telemetry.

### Logging

- structured local diagnostics only;
- sensitive types cannot be interpolated into release logs;
- release log level minimal;
- rotating size cap;
- explicit user action to export diagnostics;
- export preview and redaction;
- no image/OCR dump in release.

## 4. Cryptographic design

### Private DB key

1. Generate a random 256-bit database key.
2. Encrypt/wrap it with an AES-GCM key held by Android Keystore.
3. Store only wrapped key material and nonce.
4. Optionally require device authentication before unwrapping when app lock is enabled.
5. Handle key invalidation by offering destructive local reset; never bypass encryption.

### Data artifact verification

- Ed25519 signature over canonical manifest bytes;
- SHA-256 hashes for compressed and uncompressed DB;
- fail closed on canonicalization/signature ambiguity;
- version monotonicity and rollback policy;
- explicit revocation list.

## 5. Privacy model

The app should be usable without account creation. The business model must not depend on health-data monetization.

Privacy-policy facts must match implementation:

- what remains on device;
- what network requests occur;
- whether data is backed up;
- retention defaults;
- deletion behavior;
- third-party SDK list;
- public-data source use;
- support contact and incident process.

## 6. Abuse and malformed input

Treat images and public data as untrusted input.

- cap image dimensions and decoded memory;
- time-limit OCR/preprocessing;
- sanitize HTML from official data;
- parameterize all database queries;
- validate downloaded SQLite before opening in the active path;
- fuzz parsers with malformed Unicode and numbers;
- prevent zip/decompression bombs through expected sizes and limits;
- reject path traversal in artifact names.

## 7. Incident classes

### S0 — wrong medicine displayed as confirmed

- disable affected matching policy/database version through signed revocation;
- show in-app safety notice without exposing affected user data;
- publish corrected artifact/app;
- perform root-cause and corpus regression;
- evaluate regulatory/user notification obligations.

### S1 — official database tampering or signing-key compromise

- revoke key/version;
- stop update activation;
- rotate key through trusted app release;
- publish incident notice;
- verify previous active DB integrity.

### S2 — local privacy leak

- disable affected export/log/backup path;
- ship hotfix;
- document impact and notification duties.

## 8. Security verification

Before production:

- mobile application penetration test;
- dependency and SBOM review;
- backup/restore leakage test across Android versions and OEMs;
- rooted-device behavior assessment;
- MITM test;
- artifact tampering test;
- process-death and cache cleanup test;
- screenshot/recents/notification leakage test;
- static and dynamic secret scan;
- signing-key rotation drill.
