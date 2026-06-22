# ADR-004: Fail-Closed Medicine Matching

- Status: Accepted
- Date: 2026-06-22

## Decision

Strength, dosage-form, route, or identifier contradictions reject a candidate. Missing evidence causes review or abstention. A mandatory user review precedes finalization.

## Consequences

- Coverage is intentionally lower than permissive fuzzy matching.
- User trust and safety are prioritized over recognition completion rate.
- Release metrics focus on precision and wrong confirmed matches.
