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
python -m rxscan_data --describe
```

