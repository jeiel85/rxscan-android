# Official Source Registry

Checked: 2026-06-22 (Asia/Seoul)

This file records the initial source plan. API schemas, usage terms, traffic limits, and links must be re-verified during implementation and before every public release.

## MFDS medicine product approval information

- Agency: Ministry of Food and Drug Safety (식품의약품안전처)
- Dataset: 의약품 제품 허가정보
- Portal: https://www.data.go.kr/data/15095677/openapi.do
- Base service shown by portal: `apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07`
- Intended use: product identity, ingredient, manufacturer, packaging, storage, appearance, approval details
- Update description: real-time
- Portal usage scope at check date: no restriction shown
- Primary join key: item sequence / 품목기준코드, subject to schema verification

## MFDS easy drug information

- Agency: MFDS
- Dataset: 의약품개요정보(e약은요)
- Portal: https://www.data.go.kr/data/15075057/openapi.do
- Service endpoint shown by portal: `apis.data.go.kr/1471000/DrbEasyDrugInfoService/getDrbEasyDrugList`
- Intended use: efficacy, use, warning, cautions, interaction, adverse reaction, storage
- Important limitation: portal describes coverage as general medicines with supply records
- Primary join key: `itemSeq`
- Important fields shown by portal:
  - `efcyQesitm`
  - `useMethodQesitm`
  - `atpnWarnQesitm`
  - `atpnQesitm`
  - `intrcQesitm`
  - `seQesitm`
  - `depositMethodQesitm`
  - `openDe`
  - `updateDe`

## MFDS DUR product information

- Agency: MFDS
- Dataset: 의약품안전사용서비스(DUR)품목정보
- Portal: https://www.data.go.kr/data/15059486/openapi.do
- Base service shown by portal: `apis.data.go.kr/1471000/DURPrdlstInfoService03`
- Intended use: contraindicated combinations, age/pregnancy cautions, dose/duration cautions, elderly cautions, therapeutic duplication, extended-release split cautions
- Update description: real-time
- Portal usage scope at check date: no restriction shown

## MFDS DUR ingredient information

- Agency: MFDS
- Dataset: 의약품안전사용서비스(DUR)성분정보
- Portal: https://www.data.go.kr/data/15056780/openapi.do
- Base service shown by portal: `apis.data.go.kr/1471000/DURIrdntInfoService03`
- Intended use: ingredient-level DUR relationships and notices

## Android on-device OCR

- Provider: Google
- Documentation: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Intended use: bundled Korean on-device text recognition
- Implementation note: use `KoreanTextRecognizerOptions` and verify bundled-model dependency behavior in the build.

## Android security and backup

- Android Keystore: https://developer.android.com/privacy-and-security/keystore
- Auto Backup: https://developer.android.com/identity/data/autobackup
- Intended use: key protection and explicit backup exclusion/rules

## Google Play health policy

- Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Health content and services: https://support.google.com/googleplay/android-developer/answer/12261419
- Intended use: release compliance checklist

## MFDS regulatory consultation

- Product/innovation pre-consultation information:
  - https://data.mfds.go.kr/preConsult
- Intended use: obtain an official view based on the final intended use and functionality.

## Source-governance rules

- Store source field-level provenance.
- Do not merge conflicting values silently.
- Preserve official source dates.
- Re-check terms before redistributing transformed data and images.
- Do not present a source as current after the configured freshness threshold.
- Do not fill missing official content with generated medical text.
