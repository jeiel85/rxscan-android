# MFDS Data Access Plan

Checked: 2026-06-22 (Asia/Seoul)

RxScan uses MFDS datasets through the Korean public-data portal. Live fetching is not anonymous scraping; it requires a data.go.kr account, a dataset-level utilization request, and a `ServiceKey`.

## Access Procedure

1. Sign in to data.go.kr.
2. Open the target MFDS OpenAPI page.
3. Submit `활용신청` for development access.
4. Use the issued decoded `ServiceKey` through the local environment variable `DATA_GO_KR_SERVICE_KEY`.
5. Do not commit the key, print it, or write it into snapshots, reports, CI logs, or Android assets.
6. Re-check the API terms and approval state before production redistribution. Production use may be automatic or review-based depending on the API.

The builder URL-encodes query parameters itself, so the recommended local value is the decoded public-data portal key.

PowerShell example:

```powershell
$env:PYTHONPATH = "tools/drug-data-builder/src"
$env:DATA_GO_KR_SERVICE_KEY = "<decoded-service-key>"
python -m rxscan_data fetch `
  --source mfds_easy_drug `
  --operation getDrbEasyDrugList `
  --max-pages 1 `
  --out tools/drug-data-builder/out/live-smoke
```

## Initial Official Sources

The implementation registry is in `config/source_registry.yaml` and `tools/drug-data-builder/src/rxscan_data/mfds.py`.

| Source ID | Portal | Host | Initial operations |
| --- | --- | --- | --- |
| `mfds_drug_approval` | https://www.data.go.kr/data/15095677/openapi.do | `apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07` | `getDrugPrdtPrmsnInq07`, `getDrugPrdtPrmsnDtlInq06`, `getDrugPrdtMcpnDtlInq07` |
| `mfds_easy_drug` | https://www.data.go.kr/data/15075057/openapi.do | `apis.data.go.kr/1471000/DrbEasyDrugInfoService` | `getDrbEasyDrugList` |
| `mfds_dur_product` | https://www.data.go.kr/data/15059486/openapi.do | `apis.data.go.kr/1471000/DURPrdlstInfoService03` | DUR product operations ending in `03` |
| `mfds_dur_ingredient` | https://www.data.go.kr/data/15056780/openapi.do | `apis.data.go.kr/1471000/DURIrdntInfoService03` | DUR ingredient operations ending in `02` |

## Builder Policy

- Request `type=json`; treat unexpected XML as a failed response for this goal.
- Use `pageNo` and `numOfRows` for pagination.
- Fail on duplicate or discontinuous pages.
- Fail on required field removal or empty required values.
- Preserve official fields as source records. Do not paraphrase, summarize, or fill missing medical copy.
- Snapshot raw response bodies and hashes, but store request URLs only with `ServiceKey=<redacted>`.
- CI uses synthetic fixtures only. Live fetches are local/operator actions until a protected secret workflow is deliberately added.

## Unresolved Checks Before Public Data Publication

- Confirm per-API operating-account approval mode and traffic limits after utilization approval.
- Confirm redistribution terms for transformed SQLite artifacts and any product images.
- Confirm whether an encoded portal key is needed in any special deployment environment. Current builder expects the decoded key.
- Confirm whether XML fallback should be supported after JSON contract tests stabilize.
