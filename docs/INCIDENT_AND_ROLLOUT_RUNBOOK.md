# Incident Response and Staged Rollout Runbook (Goal 08)

App-side incident handling and rollout plan (`release_gate: staged_rollout`).
Complements `checklists/DATA_UPDATE_RUNBOOK.md` (data ops) and
`docs/ROLLBACK_AND_REVOCATION.md` (dataset rollback).

## Staged rollout

1. Internal testing track → closed beta (small group) → production staged rollout
   (e.g. 5% → 20% → 50% → 100%).
2. Define a halt criterion before each stage (crash-free rate, ANR, confirmed
   wrong-match report).
3. Named release owner and on-call for each stage.
4. Ability to halt rollout and to revoke the active public DB **independently of an
   app release** (signed revocation; `docs/ROLLBACK_AND_REVOCATION.md`).
5. Crash-free target measured **without** collecting any health payloads.

## Incident classes (07_SECURITY_PRIVACY.md §7)

### S0 — wrong medicine displayed as confirmed
- Revoke the affected matching policy/DB version via signed revocation.
- Halt rollout; show an in-app safety notice that exposes no user data.
- Publish corrected artifact/app; root-cause; add a synthetic regression fixture.
- Assess user/regulatory notification duties.

### S1 — official DB tampering or signing-key compromise
- Revoke key/version; stop update activation; freeze last known-good manifest.
- Rotate the key via a trusted app release (embeds old+new public keys).
- Publish incident notice; verify previous active DB integrity.

### S2 — local privacy leak
- Disable the affected export/log/backup path; ship a hotfix.
- Document impact and notification duties.

## Severity, comms, and postmortem

- Triage to S0/S1/S2; assign owner; communicate status without exposing health data.
- Blameless postmortem with root cause, regression fixture, and a policy/version
  change when applicable (09_TEST_QUALITY.md §7).

## Rollback drill (must pass before release)

- Demonstrate: tampered manifest/artifact rejected; revoked version not activated;
  previous verified DB remains active; corrected monotonic version published.
- Builder-side capability is covered by `tools/drug-data-builder` tests; the
  on-device activation drill is part of release verification.
