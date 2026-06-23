# Privacy Policy (Draft — based on actual implementation)

Status: DRAFT for legal/clinical review. Must be published and reviewed before
release (`release_gate: privacy_policy_published`). Facts below are written to
match the implemented behavior (07_SECURITY_PRIVACY.md §5).

## What RxScan does

RxScan reads a dispensing-bag/prescription photo **on your device** to recognize
medicine names and directions, and shows official public medicine information. It
is an information aid, not a diagnosis or treatment tool.

## Data processed on the device

- Prescription image, OCR text, recognized medicine fields, photographed
  directions, and any match results are processed **only on your device**.
- None of this is uploaded. There is no account and no sign-in.

## What leaves the device

- Nothing derived from your scan is sent anywhere.
- The app does not currently request the INTERNET permission. When public-data
  updates are enabled, the only network request will be downloading a **generic,
  signed public medicine database** over HTTPS — identical for every user and
  unrelated to your scan.
- Opening an official source link is an explicit action you take.

## Storage and retention

- Prescription history is **off by default** (opt-in).
- Original image retention is **off by default**; if enabled it is stored
  encrypted on-device only.
- When saved, history is kept in an **encrypted private database** whose key is
  protected by the Android Keystore.
- Temporary scan images are deleted on cancel, on finalize, and on app startup.
- Backups and device-to-device transfer **exclude** all app data.

## Deletion

- "Discard scan" removes the temporary image and scan data.
- "Delete prescription" removes that record and its retained image.
- "Delete all data" removes the private database, retained images, the wrapped
  key, and caches. Destroying the key renders any residual encrypted data
  unrecoverable.

## Third parties

- On-device OCR uses Google ML Kit's **bundled** Korean text-recognition model;
  recognition runs locally.
- No analytics, advertising, trackers, or remote crash-reporting SDKs are included.

## Sources

- Official public medicine data is sourced from the Korea MFDS public datasets
  (see `14_SOURCE_REGISTRY.md`). The app shows the source agency, dataset, item
  code, source date, and data version.

## Contact and incidents

- Support must never ask you to send an unredacted prescription.
- Incident handling: see `docs/INCIDENT_AND_ROLLOUT_RUNBOOK.md`.
