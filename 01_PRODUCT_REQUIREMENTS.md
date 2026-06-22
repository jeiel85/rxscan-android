# Product Requirements

## 1. Users

### Primary

- adults managing their own short-term or chronic prescriptions;
- caregivers organizing medicines for a family member;
- older users who need readable explanations;
- users who want to verify the official source behind a medicine description.

### Secondary

- pharmacists participating in content review;
- QA operators validating supported bag templates;
- maintainers operating the official-data pipeline.

## 2. Jobs to be done

1. “I want to understand what each printed medicine is without sending my prescription photo to a server.”
2. “I want the app to show exactly what it read before I trust the result.”
3. “I want to distinguish my pharmacy’s directions from general drug information.”
4. “I want to know where each explanation came from and how current it is.”
5. “When the app is uncertain, I want it to say so instead of guessing.”

## 3. Core user stories

### Capture and recognition

- As a user, I can capture a bag with framing, blur, glare, and text-size guidance.
- As a user, I am prevented from accepting a scan that fails minimum quality checks.
- As a user, I can crop or mask personal identifiers before optionally retaining the image.
- As a user, I can view the OCR overlay and correct text before medicine matching.

### Medicine confirmation

- As a user, I see each detected medicine line separately.
- As a user, I see the evidence used for matching: name, strength, dosage form, manufacturer, and code.
- As a user, I must confirm or reject ambiguous candidates.
- As a user, I can mark a line unresolved without selecting a guess.
- As a user, I can search the local database manually.

### Information and provenance

- As a user, I see photographed prescription directions in a dedicated section.
- As a user, I see general official information in a different section.
- As a user, I see agency, dataset, item code, source update date, and database version.
- As a user, I can open the official source page when a stable link is available.
- As a user, I am warned when the local official-data database is stale.

### Privacy

- As a user, I can use recognition without an account.
- As a user, I can use the app with networking disabled after data installation.
- As a user, I choose whether to save a prescription.
- As a user, I can delete one prescription or all private data immediately.
- As a user, I can require biometric/device authentication to open saved records.

## 4. Functional requirements

### FR-CAPTURE

- `FR-CAPTURE-001`: Use the rear camera by default.
- `FR-CAPTURE-002`: Show live document boundary and quality indicators.
- `FR-CAPTURE-003`: Capture full-resolution still image only after quality threshold passes or explicit override.
- `FR-CAPTURE-004`: Record no audio or location.
- `FR-CAPTURE-005`: Keep temporary images in app-private cache.
- `FR-CAPTURE-006`: Delete temporary images when the session is discarded or finalized unless explicit encrypted retention is selected.

### FR-OCR

- `FR-OCR-001`: OCR must run on-device.
- `FR-OCR-002`: The Korean recognition model must be bundled for first-run offline use.
- `FR-OCR-003`: Return text, confidence if available, block/line/element geometry, rotation, and preprocessing variant.
- `FR-OCR-004`: Preserve the unmodified OCR result and the normalized parse value separately.
- `FR-OCR-005`: No raw OCR text may enter remote logs, analytics, crash reports, or URLs.

### FR-PARSE

- `FR-PARSE-001`: Parse drug name, strength, unit, dosage form, manufacturer, item/insurance code, quantity, frequency, timing, and duration when present.
- `FR-PARSE-002`: Associate fields by spatial row/column relationships, not string order alone.
- `FR-PARSE-003`: Every parsed field carries evidence coordinates and a confidence band.
- `FR-PARSE-004`: Contradictory fields remain unresolved.

### FR-MATCH

- `FR-MATCH-001`: Candidate retrieval uses the local official-data DB only.
- `FR-MATCH-002`: Exact recognized identifiers take priority over fuzzy names.
- `FR-MATCH-003`: Strength and dosage-form conflicts are hard rejection conditions.
- `FR-MATCH-004`: The matcher returns evidence, score components, contradiction reasons, and top candidates.
- `FR-MATCH-005`: The review screen is mandatory before a prescription is finalized.
- `FR-MATCH-006`: The app never fabricates a missing strength or product suffix.

### FR-INFO

- `FR-INFO-001`: Display photographed directions and official approved/general information separately.
- `FR-INFO-002`: Preserve official text semantics; do not use generative summaries.
- `FR-INFO-003`: Any simplified explanation must be versioned, fixed, and pharmacist-reviewed.
- `FR-INFO-004`: Display source provenance on every medicine page.
- `FR-INFO-005`: Show a stale-data indicator according to the freshness policy.

### FR-DUR

- `FR-DUR-001`: Run only after all involved medicines are confirmed.
- `FR-DUR-002`: Identify the official DUR rule type and source date.
- `FR-DUR-003`: Never instruct the user to stop or change medication.
- `FR-DUR-004`: State that a prescriber/pharmacist may have a valid clinical reason and advise confirmation.
- `FR-DUR-005`: If the database is beyond the critical freshness threshold, disable “current safety check” claims.

## 5. Non-functional requirements

### Privacy and security

- No account, advertising SDK, behavioral analytics, remote configuration, or patient-data API.
- Public database downloads contain no scan-derived query.
- Keystore-protected encryption for private retained data.
- Screens containing prescription data are protected from recents thumbnails and screenshots according to the selected privacy mode.
- Backups exclude private health data by default.

### Reliability

- Atomic database replacement.
- Keep the previous two verified database versions for rollback.
- Process death must not leak temporary plaintext images.
- A partially downloaded or invalid database must never become active.
- The app must remain usable with the last verified database while offline.

### Performance targets

Targets must be measured on a defined reference device class.

- Live quality analysis: median under 80 ms per sampled frame.
- Full-resolution preprocessing and OCR: p95 under 4 seconds.
- Candidate search: p95 under 150 ms per medicine line.
- Detail screen from local DB: p95 under 200 ms.
- Cold launch to home: p95 under 2 seconds.
- Memory peak during scan: under 350 MB on the reference midrange device.

These are engineering targets, not guarantees until benchmarked.

### Accessibility

- Korean screen-reader labels for controls and confidence status.
- Minimum touch target 48 dp.
- Do not encode status using color alone.
- Dynamic text sizing without truncating medicine names.
- Readable source and warning text.
- Camera guidance must include audio/haptic alternatives where practical.

## 6. Out-of-scope claims

Do not use marketing statements such as:

- “100% accurate”
- “prevents all drug interactions”
- “AI pharmacist”
- “safe to take”
- “recommended dose”
- “replace your pharmacist”
- “diagnoses side effects”

Permitted product language should emphasize organization, recognition assistance, official-source lookup, and user verification.
