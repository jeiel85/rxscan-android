# Security Review Checklist

- [ ] No real API key in repository, APK, logs, or test fixtures.
- [ ] No analytics/ad/tracking SDK.
- [ ] No scan-dependent remote request.
- [ ] Cleartext traffic disabled.
- [ ] Manifest signature verified before artifact download/activation.
- [ ] SHA-256 and SQLite integrity verified.
- [ ] Artifact path and decompression size constrained.
- [ ] Public DB opened read-only.
- [ ] Private DB key generated randomly and protected by Keystore.
- [ ] Private DB excluded from backup.
- [ ] Temporary scans stored in private no-backup/cache directory.
- [ ] Startup garbage-collects abandoned temporary files.
- [ ] Release logs reject sensitive model types.
- [ ] Export requires explicit action and preview.
- [ ] Screens/recents/notifications reviewed for leakage.
- [ ] Parser and sanitizer fuzz tests pass.
- [ ] Dependency SBOM and license review complete.
- [ ] Key rotation and incident revocation tested.
