# Local Data Model

## 1. Physical separation

### Public DB

- read-only;
- signed and checksum-verified;
- contains official public drug information and indexes;
- not encrypted for confidentiality because content is public;
- opened only after integrity verification.

### Private DB

- encrypted;
- contains user-retained prescriptions, confirmations, and settings;
- key is random and wrapped/protected with Android Keystore;
- excluded from cloud backup by default.

Temporary images reside in app-private cache/no-backup storage and are deleted after finalization unless explicit encrypted retention is selected.

## 2. Private domain entities

### `ScanSession`

- ID;
- state;
- created/finalized time;
- image-retention choice;
- capture quality report;
- OCR/parser versions;
- public DB version;
- error status.

### `PrescriptionRecord`

- local ID;
- display title chosen by user;
- prescription date if user confirms it;
- encrypted optional notes;
- source scan ID;
- retention and deletion timestamps.

Avoid storing patient name, full birth date, phone, address, institution, or pharmacy unless a later explicit feature has a documented necessity and consent flow.

### `MedicationEntry`

- prescription ID;
- official item code if confirmed;
- photographed raw medicine line;
- normalized fields;
- user-confirmed directions;
- match status;
- source DB version;
- user confirmation time.

### `DoseSchedule`

Store structured fields and original photographed text. The app must display which came from the bag.

## 3. Data minimization defaults

- History is opt-in at finalization.
- Original image retention is off.
- Patient identity fields are not saved.
- Pharmacy/hospital information is not saved.
- No cross-device sync.
- No contact, location, calendar, or account access.
- Manual export is explicit, encrypted where possible, and warns about destination exposure.

## 4. Deletion semantics

- “Discard scan” deletes temporary image, OCR evidence, candidate cache, and session data.
- “Delete prescription” deletes private DB rows and retained encrypted image.
- “Delete all data” deletes private DB, keys, retained images, and caches.
- Secure deletion on flash storage cannot be absolutely guaranteed; encryption-key destruction is the primary protection for encrypted retained data.
- Public DB can be re-downloaded and is not considered user-private data.

## 5. Migration rules

- Public DB schema migration happens in the data builder, not on the user’s device where possible.
- App supports an explicit range of public schema versions.
- Private DB migrations require fixture-based migration tests.
- Destructive migration is prohibited for private DB release builds.
- Every private record carries parser and DB versions for auditability.

## 6. Export format

An optional export should contain:

- user-confirmed medicine;
- photographed direction text;
- official item code;
- source dataset and date;
- app/db version;
- an explicit statement that the export is not a prescription replacement.

Never export the original image unless the user separately selects it.
