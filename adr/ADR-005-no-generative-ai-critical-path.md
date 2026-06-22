# ADR-005: No Generative AI in the Critical Path

- Status: Accepted
- Date: 2026-06-22

## Decision

Do not use LLMs or generative models for medicine identification, dose parsing, missing-field reconstruction, safety evaluation, or medical explanations.

## Consequences

- Behavior is deterministic and testable.
- Missing consumer content remains missing unless a pharmacist-reviewed fixed content layer is added.
- Marketing must not frame the application as an AI pharmacist.
