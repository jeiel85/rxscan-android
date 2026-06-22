# Project Structure and Engineering Standards

## 1. Monorepo

```text
rxscan/
├─ apps/
│  └─ android/
│     ├─ app/
│     ├─ core/
│     ├─ data/
│     ├─ engine/
│     ├─ feature/
│     ├─ build-logic/
│     └─ gradle/
├─ tools/
│  └─ drug-data-builder/
│     ├─ src/rxscan_data/
│     ├─ tests/
│     ├─ fixtures/
│     └─ pyproject.toml
├─ infra/
│  └─ data-distribution/
│     ├─ workflows/
│     ├─ schemas/
│     └─ runbooks/
├─ docs/
├─ testdata/
│  ├─ synthetic/
│  └─ README.md
├─ AGENTS.md
├─ SECURITY.md
└─ README.md
```

## 2. Android conventions

- Kotlin strict nullability and explicit API where useful.
- Coroutines and Flow with structured concurrency.
- Immutable UI state.
- One-way data flow.
- Interfaces at module boundaries.
- No business logic in Composables.
- No Android framework types in parser/matcher domain APIs where avoidable.
- Time through injected clock.
- Randomness through injected provider.
- Database and policy versions included in results.
- Release builds fail on lint errors relevant to security/correctness.

## 3. Data builder conventions

- Python type checking;
- deterministic output for the same normalized input;
- schema fixtures for every source;
- retry only idempotent requests;
- raw snapshots written before transformation;
- secrets only through environment/CI secret store;
- structured build reports;
- no silent coercion on source schema changes.

## 4. Definition of done

A feature is done only when:

- code and tests pass;
- failure states are implemented;
- accessibility is included;
- privacy/logging review passes;
- source/provenance behavior is correct;
- documentation and migration impact are updated;
- no unsupported medical claim is introduced;
- acceptance criteria are demonstrated.

## 5. Branch and release strategy

- trunk-based or short-lived branches;
- protected main;
- required CI;
- signed release tags;
- separate data artifact versioning from app versioning;
- staged app rollout;
- ability to revoke DB independent of app release.

## 6. CI jobs

### Android

- formatting and static analysis;
- unit/property tests;
- instrumentation smoke tests;
- dependency/license scan;
- secret scan;
- release build;
- SBOM;
- artifact signature verification test.

### Data builder

- source-adapter fixture tests;
- schema validation;
- ETL unit tests;
- reproducibility test;
- SQLite integrity;
- policy data checks;
- manifest schema/signature test.

### Nightly/weekly QA

- synthetic corpus benchmark;
- selected de-identified corpus benchmark in protected environment;
- performance regression;
- backup/network privacy test where automated.
