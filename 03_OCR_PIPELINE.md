# OCR and Document Pipeline

## 1. Safety objective

The OCR pipeline is not responsible for deciding the medicine. It produces auditable evidence: text, geometry, preprocessing origin, and uncertainty. The matcher may abstain.

## 2. Capture pipeline

### Live frame analysis

Use a throttled CameraX `ImageAnalysis` stream for guidance only:

- document quadrilateral detection;
- text-region occupancy estimate;
- blur estimate using variance of Laplacian or equivalent;
- glare/overexposure ratio;
- underexposure ratio;
- skew and perspective severity;
- edge clipping;
- motion stability across frames.

Do not run full OCR on every frame. Full OCR runs on the captured still image.

### Capture acceptance

A scan is accepted when:

- document corners or a stable crop are available;
- the medication-information area is not clipped;
- blur and glare are below calibrated thresholds;
- median text height is expected to meet OCR requirements;
- no critical quality flag remains.

An override may exist for accessibility, but an overridden scan is never eligible for high-confidence auto-resolution.

## 3. Image preprocessing

Keep the original bytes until review is finalized. Produce derived in-memory or temporary variants:

1. perspective-corrected color image;
2. grayscale image;
3. local contrast-enhanced image;
4. adaptive threshold image when print/background requires it.

Use orientation metadata correctly. Preserve a transform matrix so OCR boxes can be mapped back to the displayed original.

Avoid destructive sharpening that creates false characters. Every OCR token records the preprocessing variant that produced it.

## 4. OCR strategy

Use the bundled Korean on-device recognition model.

Run:

- primary pass on perspective-corrected color/grayscale;
- secondary pass only when the primary pass fails quality/lexicon checks;
- optional region-specific pass on detected medicine table and direction block.

Combine passes using token geometry and agreement. Do not simply concatenate text.

## 5. OCR output model

```kotlin
data class OcrToken(
    val rawText: String,
    val normalizedText: String,
    val polygon: List<PointF>,
    val lineId: String,
    val blockId: String,
    val sourceVariant: ImageVariant,
    val engineConfidence: Float?,
    val consensusCount: Int,
    val flags: Set<OcrFlag>
)
```

Normalization must not overwrite `rawText`.

## 6. Normalization

Allowed deterministic normalization:

- Unicode NFKC;
- normalize whitespace and punctuation variants;
- standardize Korean dosage units while preserving the original;
- join tokens split only by layout evidence;
- convert common visually confusable characters only when constrained by field grammar;
- canonicalize manufacturer suffixes and common dosage-form suffixes through versioned dictionaries.

Examples requiring context:

- `0` vs `O`;
- `1` vs `I` vs `l`;
- `5` vs `S`;
- `mg`, `㎎`, `MG`;
- `1일 3회`, `하루3번`;
- `식후30분`, `식후 30분`.

Never infer a missing digit, unit, or dosage-form suffix from the candidate drug database.

## 7. Layout analysis

1. Cluster OCR lines by vertical overlap and baseline.
2. Detect column boundaries from repeated x-coordinates.
3. Classify regions:
   - patient/pharmacy header;
   - prescription directions;
   - medicine table;
   - footer/disclaimer.
4. Identify medicine rows using:
   - pharmaceutical suffix lexicon;
   - strength/unit pattern;
   - nearby manufacturer/code fields;
   - repeated table structure.
5. Preserve evidence boxes for every extracted field.

## 8. Direction parser

Implement a deterministic grammar/state machine.

### Canonical fields

- `doseAmount`: decimal + unit;
- `frequencyPerDay`;
- `timings`: morning/lunch/evening/bedtime/as-needed;
- `mealRelation`: before/with/after meal;
- `mealOffsetMinutes`;
- `durationDays`;
- `route`;
- `specialInstructionRaw`.

### Examples

```text
1회 1정, 1일 3회, 식후 30분, 5일분
→ amount=1 tablet
→ frequency=3/day
→ mealRelation=AFTER
→ offset=30 min
→ duration=5 days
```

If the same row contains contradictory frequencies, return `CONFLICT` and require user correction.

## 9. PII treatment

The OCR engine may necessarily read the full captured image locally, but:

- header regions are not required for medicine matching;
- patient name, resident number, phone, address, hospital, and pharmacy fields are tagged as sensitive;
- these values are excluded from normal diagnostic output;
- optional retained images show a masking preview;
- no PII enters golden parser fixtures unless synthetic.

## 10. Failure behavior

Return one of:

- `CAPTURE_TOO_BLURRY`;
- `GLARE_OVER_TEXT`;
- `TEXT_TOO_SMALL`;
- `DOCUMENT_CLIPPED`;
- `UNSUPPORTED_LAYOUT`;
- `OCR_INSUFFICIENT`;
- `PARSER_CONFLICT`;
- `NO_MEDICATION_LINES`.

Each error includes an actionable rescan instruction, not a medicine guess.
