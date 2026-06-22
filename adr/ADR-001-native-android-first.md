# ADR-001: Native Android First

- Status: Accepted
- Date: 2026-06-22

## Context

The product depends on camera quality analysis, full-resolution document processing, bundled on-device Korean OCR, secure local file lifecycle, encrypted storage, accessibility, and reliable background database updates.

## Decision

Build the first production version in Kotlin with Jetpack Compose and CameraX.

## Consequences

- Strong control over privacy and performance.
- Android-specific engineering and QA are required.
- iOS is a separate later implementation sharing data contracts and policies, not UI code.
