# Data Update Runbook

## Normal daily run

1. Fetch each source into an immutable timestamped raw snapshot.
2. Verify response status, encoding, schema, page continuity, and counts.
3. Normalize into source staging tables.
4. Build conflict and quarantine report.
5. Run data quality tests.
6. Build final SQLite DB.
7. Run `quick_check`, foreign keys, counts, and search smoke tests.
8. Generate manifest and build report.
9. Sign canonical manifest.
10. Upload to staging path.
11. Re-download and independently verify.
12. Promote manifest pointer.
13. Record active version and notification status.

## On source failure

- Do not publish a partial artifact.
- Keep the last verified version.
- Retry with controlled backoff.
- Compare API documentation/schema.
- Update adapter and fixture tests.
- Review freshness threshold and user warning.

## On bad artifact

- Add version to signed revocation list.
- Restore previous manifest pointer.
- Publish corrected monotonically newer artifact.
- Run app rollback verification.
- Create incident report and regression test.

## On signing-key concern

- Stop publication.
- Freeze active known-good manifest.
- Invoke key-compromise plan.
- Release app containing rotated public key chain as required.
- Do not accept unsigned emergency artifacts.
