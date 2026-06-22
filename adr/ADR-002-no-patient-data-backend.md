# ADR-002: No Patient-Data Backend

- Status: Accepted
- Date: 2026-06-22

## Decision

The application has no API that accepts prescription images, OCR text, medicine queries derived from scans, or history.

## Consequences

- Lower privacy and breach surface.
- No account sync or server-side correction learning.
- Official data is delivered as generic signed artifacts.
- Support must use redacted, explicit user exports.
