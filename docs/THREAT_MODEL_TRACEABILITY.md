# Threat Model Traceability (Goal 07)

Maps the controls in `07_SECURITY_PRIVACY.md` to where they are implemented and
how they are verified. Device-only controls cannot run in the JVM-only CI; they
are implemented in production code and verified by instrumentation/manual security
testing (`07_SECURITY_PRIVACY.md` §8).

## Storage & key protection

| Control | Implementation | Verification |
| --- | --- | --- |
| Random 256-bit private-DB key | `core:security` `DatabaseKey.generate` | `KeyWrappingTest` (size, randomness) |
| Key wrapped with AES-GCM KEK | `core:security` `AesGcmKeyWrapper` | `KeyWrappingTest` round-trip |
| Wrong/invalidated key fails closed | `AesGcmKeyWrapper.unwrap` → `KeyUnwrapException` | `KeyWrappingTest` wrong-key / tampered-ciphertext |
| KEK held in Android Keystore | `data:privatedb` `AndroidKeystoreKeyWrapper` | instrumentation (device) |
| App-lock requires device auth | `AndroidKeystoreKeyWrapper(requireUserAuthentication=true)` | instrumentation (device) |
| Only wrapped key persisted | `data:privatedb` `PrivateDataStore.persistWrappedKey` | `PrivateDataStoreTest` |
| Private DB unreadable without key | DB encrypted with unwrapped key only | `KeyWrappingTest` (fail-closed unwrap) + instrumentation |

## Data lifecycle & deletion

| Control | Implementation | Verification |
| --- | --- | --- |
| Opt-in history / image retention (off by default) | `core:security` `PrivacySettings` | `PrivacySettingsTest` |
| Temp scan images cleaned on cancel/finalize/startup | `feature:scan` `ScanTempFileStore` | `ScanTempFileStoreTest` |
| Delete one prescription | `PrivateDataStore.deletePrescriptionImage` | `PrivateDataStoreTest` |
| Delete all (DB, key, images, cache) | `PrivateDataStore.deleteAll` | `PrivateDataStoreTest` |
| Key invalidation → destructive reset | `PrivateDataStore.destructiveReset` | `PrivateDataStoreTest` |

## Backup / device transfer

| Control | Implementation | Verification |
| --- | --- | --- |
| `allowBackup=false` | `app/AndroidManifest.xml` | manifest review |
| Cloud-backup + device-transfer exclusions | `res/xml/data_extraction_rules.xml` | manifest review + instrumentation |
| Full-backup content excluded | `res/xml/backup_rules.xml` | instrumentation |

## UI leakage

| Control | Implementation | Verification |
| --- | --- | --- |
| Recents/screenshot protection | `MainActivity` `FLAG_SECURE`; `PrivacySettings.protectScreenshots` | instrumentation (device) |
| Hide content in background | `PrivacySettings.hideContentInBackground` | instrumentation (device) |

## Network

| Control | Implementation | Verification |
| --- | --- | --- |
| Cleartext disabled | `res/xml/network_security_config.xml` | manifest review |
| No INTERNET permission yet (Goal 0x) | `app/AndroidManifest.xml` | `tools/ci/check_repository_policy.py` |
| No scan-derived requests | scan/OCR modules have no network code | architecture review |

## Logging & diagnostics

| Control | Implementation | Verification |
| --- | --- | --- |
| Sensitive values cannot be interpolated into logs | `core:logging` `SensitiveText` (`toString` = `[REDACTED]`) | `DiagnosticsTest` |
| Rotating size-capped diagnostics | `core:logging` `DiagnosticsBuffer` | `DiagnosticsTest` |
| Redacted export preview (explicit action) | `core:logging` `Redactor` / `DiagnosticsBuffer.exportPreview` | `DiagnosticsTest` |

## Untrusted input (cross-goal)

| Control | Implementation | Verification |
| --- | --- | --- |
| Artifact path-traversal rejected | `tools/drug-data-builder` `safe_artifact_path`; `data:publicdb` parameterized queries | builder tests; review |
| Signed artifact verification | `tools/drug-data-builder` Ed25519 verify | builder tests |
| Missing fields never inferred | `engine:parser` / `engine:matcher` | parser/matcher tests |

## Not yet covered (future goals / device verification)

- Real encrypted SQLite at rest (SQLCipher-equivalent) wired to the wrapped key —
  key management is in place; DB-engine integration is a later step.
- Full instrumentation security suite (backup/restore leakage, MITM, rooted-device,
  recents/screenshot) — requires emulator/device, not run in this CI.
