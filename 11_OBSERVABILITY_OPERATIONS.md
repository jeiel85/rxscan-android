# Operations and Observability

## 1. Privacy-preserving observability

No server analytics are required for the first release.

Allowed local counters:

- scan started/completed;
- quality failure category;
- OCR duration;
- parser status;
- match class counts;
- DB update status;
- crash marker and non-sensitive stack fingerprint.

Never include:

- OCR text;
- medicine name/code;
- prescription date;
- pharmacy/hospital;
- patient identity;
- image hash that could be used as a persistent identifier.

Local counters are deleted with app data and exported only by explicit user action.

## 2. Data pipeline monitoring

CI produces a machine-readable build report:

- source availability;
- response/schema changes;
- per-source record counts;
- join rates;
- quarantined records;
- field null-rate changes;
- DB size;
- integrity results;
- signature key ID;
- publication result.

Alert on:

- source failure;
- record-count drop beyond threshold;
- item-code uniqueness violation;
- sanitizer failure;
- signing failure;
- artifact upload mismatch;
- active DB age threshold.

## 3. Runbooks

### Source API failure

- keep last verified DB;
- retry with backoff;
- do not publish partial data;
- assess whether source endpoint/schema changed;
- update source adapter with fixture;
- publish only after regression pass.

### Bad artifact published

- add version to signed revocation list;
- restore previous known-good version;
- block activation on app;
- produce corrected version with new monotonically increasing ID;
- document affected source/build.

### Matching incident

- determine app, policy, parser, and DB versions;
- reproduce only with consented/redacted evidence;
- add regression fixture;
- consider signed DB/policy revocation;
- communicate that users should confirm with pharmacy, without advising treatment changes.

## 4. Version identifiers shown in app

- app version;
- OCR engine/model identifier;
- parser version;
- matching policy version;
- public DB version;
- source snapshot date range;
- simplified-content review version.

## 5. Retention

- CI build logs: retain according to security policy, without secrets;
- immutable source/build metadata: retain for audit;
- raw source snapshots: retain according to license and storage policy;
- user device diagnostics: short rotating retention, suggested 7 days;
- no central prescription retention.
