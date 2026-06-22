# Release Checklist

## Product and medical

- [ ] Intended-use statement matches actual features and store listing.
- [ ] No dose recommendation, diagnosis, or stop/change instruction.
- [ ] Prescription directions and official general information are separated.
- [ ] Pharmacist review completed for fixed medical copy.
- [ ] Regulatory applicability review completed for the final build.

## Data

- [ ] All official sources and terms re-verified.
- [ ] Source snapshots current.
- [ ] Build report has no unexplained schema/count anomaly.
- [ ] Public DB passes integrity and provenance checks.
- [ ] Manifest signature and app verification pass.
- [ ] Rollback and revocation drill pass.

## Accuracy

- [ ] Release holdout gates pass.
- [ ] No wrong high-confidence match in required holdout.
- [ ] Hard contradiction property tests pass.
- [ ] Direction exact-match gate passes.
- [ ] Unsupported formats fail clearly.

## Privacy and security

- [ ] Network capture contains no prescription/OCR data.
- [ ] Backup/device-transfer leakage tests pass.
- [ ] Release logs contain no sensitive values.
- [ ] Temporary image lifecycle tests pass.
- [ ] Private DB encryption and key invalidation tests pass.
- [ ] Screenshot/recents/notification protections pass.
- [ ] SBOM, dependency, license, and vulnerability review pass.
- [ ] Penetration test findings resolved or accepted in writing.

## Play and documentation

- [ ] Health apps declaration completed accurately.
- [ ] Data safety form matches build behavior.
- [ ] Privacy policy published and reviewed.
- [ ] Current target API requirement met.
- [ ] Support process does not solicit unredacted prescriptions.
- [ ] User-facing data version and source details visible.
- [ ] Staged rollout and incident owner configured.
