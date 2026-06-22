# RxScan Data Builder

This package is the bootstrap home for the public official-data build pipeline.
Goal 00 intentionally does not fetch official APIs or publish SQLite artifacts.

Allowed in this package now:

- schema and policy contract checks;
- synthetic API-shaped fixtures;
- deterministic local commands;
- tests that prove secrets and patient data are not required.

Not allowed in this package:

- committed public API keys;
- real prescription images or OCR text;
- generated medical summaries;
- scan-derived server queries.

## Commands

```powershell
python -m unittest discover -s tools/drug-data-builder/tests
npx --yes pyright tools/drug-data-builder
$env:PYTHONPATH = "tools/drug-data-builder/src"
python -m rxscan_data --describe
python -m rxscan_data list-sources
python -m rxscan_data build-fixture `
  --source mfds_easy_drug `
  --operation getDrbEasyDrugList `
  --fixture tools/drug-data-builder/fixtures/synthetic/mfds_easy_drug_minimal.json `
  --out tools/drug-data-builder/out/fixture-smoke
```

Live MFDS fetches require a data.go.kr utilization request and a decoded public-data portal `ServiceKey`:

```powershell
$env:PYTHONPATH = "tools/drug-data-builder/src"
$env:DATA_GO_KR_SERVICE_KEY = "<decoded-service-key>"
python -m rxscan_data fetch `
  --source mfds_easy_drug `
  --operation getDrbEasyDrugList `
  --max-pages 1 `
  --out tools/drug-data-builder/out/live-smoke
```

The fetcher redacts `ServiceKey` from stored request URLs and reports. Do not add live output directories to commits.
