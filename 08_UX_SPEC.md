# UX and Content Specification

## 1. UX principles

- Show evidence before conclusions.
- State uncertainty directly.
- Separate photographed instructions from official general information.
- Keep high-risk actions out of the product.
- Make source and date visible without hiding them behind legal pages.
- Use plain Korean while preserving official meaning.

## 2. Navigation

```text
Home
├─ Scan dispensing bag
├─ Import image
├─ Saved prescriptions (only when enabled)
├─ Official medicine search
└─ Settings / Privacy / Data version
```

## 3. Scan flow

### Screen A — Introduction

Copy intent:

> 약봉지 사진은 이 기기 안에서만 분석됩니다. 촬영한 사진은 저장을 선택하지 않으면 확인 후 삭제됩니다.

Buttons:

- `약봉지 촬영`
- `사진 가져오기`
- `지원 범위 보기`

### Screen B — Camera

Indicators:

- document boundary;
- “조금 더 가까이”;
- “빛 반사를 줄여 주세요”;
- “흔들림 없이 고정해 주세요”;
- “약 이름과 복용법이 모두 보이게 해 주세요.”

Do not use a fake progress percentage.

### Screen C — OCR review

Show:

- image with line overlays;
- extracted medicine rows;
- directions;
- low-confidence characters highlighted;
- edit and rescan actions.

Copy:

> 흐리거나 잘린 글자는 직접 확인해 주세요. 앱은 보이지 않는 내용을 추측하지 않습니다.

### Screen D — Medicine match review

For each line show:

- photographed text;
- recognized fields;
- official candidate;
- evidence chips;
- source agency and item code;
- state: identifier match / review / ambiguous / unresolved.

Mandatory final action:

- `확인한 약으로 계속`
- unresolved lines remain visibly unresolved.

### Screen E — Prescription summary

Two explicit sections:

1. `약봉지에 적힌 복용법`
2. `공식 의약품 정보`

Do not merge approved general use with prescribed directions.

## 4. Confidence wording

Use:

- `코드가 일치했습니다`
- `이름·함량·제형이 일치합니다. 직접 확인해 주세요`
- `비슷한 약이 여러 개입니다`
- `정확한 약을 확인하지 못했습니다`

Avoid:

- `AI가 확정함`
- `안전함`
- `문제 없음`
- `100% 일치`
- unexplained numeric percentages.

## 5. DUR wording template

Title:

> 공식 안전사용 정보가 있습니다

Body:

> 확인한 약들 사이에서 **{DUR_TYPE}** 정보가 조회되었습니다. 처방에는 의료진의 판단에 따른 사유가 있을 수 있으므로 약을 임의로 중단하거나 변경하지 말고, 처방한 의료기관 또는 조제약국에 확인하세요.

Footer:

- agency;
- DUR rule ID/type;
- notice/source date;
- DB version;
- `공식 출처 보기`.

Never produce “Do not take these together” as an unconditional app-generated instruction.

## 6. Missing consumer-friendly content

When `e약은요` content is absent:

> 이 제품은 현재 앱이 사용하는 소비자용 공식 설명 데이터에 내용이 없습니다. 아래에는 확인 가능한 허가정보 원문만 표시합니다.

Do not generate a summary to fill the gap.

## 7. Stale data

Warning:

> 공식 정보 데이터가 {N}일 동안 업데이트되지 않았습니다. 표시된 기준일을 확인하고 최신 정보는 약사 또는 공식 원문에서 확인하세요.

When critical threshold is exceeded:

> 현재 데이터로는 최신 DUR 확인을 제공하지 않습니다.

## 8. Privacy settings

- Save prescription history: off by default.
- Save original image: off by default.
- App lock: recommended when history is enabled.
- Protect screenshots: on by default for private screens, with documented accessibility trade-off.
- Delete temporary data now.
- Delete all saved prescriptions.
- View network activity explanation.
- View official-data version.

## 9. Accessibility copy

Confidence and warnings require text labels, icons, and semantic descriptions. Source metadata must be reachable by screen reader. Large text must not truncate strength or dosage form.
