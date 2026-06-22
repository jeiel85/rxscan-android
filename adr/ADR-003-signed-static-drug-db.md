# ADR-003: Signed Static Drug Database

- Status: Accepted
- Date: 2026-06-22

## Decision

Fetch official APIs in CI, build a normalized read-only SQLite database, sign its canonical manifest, and distribute it as a generic static artifact.

## Consequences

- Public API keys stay out of the APK.
- Scans never become server queries.
- Data freshness, signing, rollback, and source schema monitoring become operational responsibilities.
