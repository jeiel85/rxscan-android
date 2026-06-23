# Google Play Data Safety — Draft Answers

Status: DRAFT; must match final build behavior before submission
(`release_gate: data_safety_form`). Grounded in the current implementation.

## Data collection and sharing

| Question | Answer | Basis |
| --- | --- | --- |
| Does the app collect or share any of the required user data types? | **No data collected or shared off-device** | No scan-derived network requests; no account; no analytics/ads SDKs |
| Is all collected data encrypted in transit? | N/A (no data leaves the device); artifact downloads, when enabled, are HTTPS-only | `network_security_config.xml` |
| Do you provide a way to request data deletion? | Yes — on-device "Delete all data" | `PrivateDataStore.deleteAll` |

## Health data

- Prescription images and recognized medicine data are **health-adjacent** and are
  processed **on-device only**; they are not "collected" in the Play sense (not
  sent off device).
- History/image retention are **opt-in**, stored **encrypted on-device**, excluded
  from backup.

## Security practices

- Data encrypted at rest (private DB key wrapped by Android Keystore).
- User can request deletion (delete one / delete all).
- No data shared with third parties.

## Health apps declaration

- The app is an information aid; it does not provide diagnosis, dosing
  recommendations, or stop/change instructions (see
  `docs/REGULATORY_REVIEW_PACKAGE.md`).
- Complete the Play "Health apps" declaration accordingly before release.

## Reviewer notes

- The only third-party component is Google ML Kit's bundled on-device OCR model;
  it performs no network calls for recognition.
- Verify against the final build with a network-capture test
  (`release_gate: network_no_sensitive`) before submitting.
