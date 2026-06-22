# Coding Agent Rules

## Mission

Implement the RxScan design bundle as a production-oriented Android application and official-data build pipeline. Complete goals in numerical order.

## Absolute constraints

- Never upload prescription images, OCR text, medicine selections, or saved history.
- Never add analytics, ads, trackers, Firebase Analytics, remote configuration, or user accounts.
- Never use a generative model to identify medicine, parse dose, evaluate DUR, or write medical conclusions.
- Never guess a missing medicine field.
- Never bypass the mandatory medicine review step.
- Never treat photographed prescription directions as the general approved use, or vice versa.
- Never commit a real prescription image or public API key.
- Never log sensitive values.
- Never silently relax confidence or contradiction policy to make tests pass.
- Never use destructive private-database migration in release.
- Never activate an unsigned or invalid public DB.

## Engineering behavior

1. Read the relevant design documents before editing.
2. Produce a small implementation plan in the issue/commit description.
3. Add tests before or with implementation.
4. Implement all failure states.
5. Run formatting, static analysis, unit tests, and relevant instrumentation tests.
6. Update documentation and versioned policies.
7. Stop the goal when acceptance criteria fail; report the exact failure.
8. Keep dependencies minimal and justify additions.
9. Use English filenames and identifiers; Korean is allowed in user-facing strings and documentation.
10. Preserve deterministic behavior in parser and matcher.

## Medical copy rule

User-facing medical content must be one of:

- verbatim/sanitized official source content;
- a fixed template explicitly defined in the design;
- pharmacist-reviewed, versioned fixed copy.

Do not improvise medical wording in code.

## Data handling rule

Use synthetic data for normal development. Access to consented de-identified real fixtures is a separate protected process.

## Definition of completion

A goal is complete only when:

- acceptance criteria pass;
- tests and checks pass;
- no TODO remains on a safety-critical path;
- privacy and failure behavior are demonstrated;
- relevant documentation is updated.
