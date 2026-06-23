# Device / OEM Test Matrix (Goal 08)

Minimum matrix from `09_TEST_QUALITY.md` §6. Execution is a launch blocker
(`release_gate: device_matrix`). Use synthetic dispensing-bag images only unless a
consented corpus is approved.

## Dimensions

- **Tier:** low-end, mid-range, high-end.
- **OEM:** Samsung, Google (Pixel), and at least one additional OEM (e.g. Xiaomi/LG legacy).
- **Android version:** minSdk 26 → current target (36), covering key intermediates.
- **Form factor:** small phone, large phone, tablet.
- **Camera:** different sensor aspect ratios (4:3, 16:9, high-MP).
- **Accessibility/locale:** Korean locale, large font, TalkBack on.

## Per-device checklist

- [ ] Airplane-mode first-run OCR works after install (bundled model).
- [ ] CameraX preview + full-res capture; quality gate guidance appears.
- [ ] Document boundary overlay aligns; OCR boxes map after perspective transform.
- [ ] Mandatory review cannot be bypassed; unresolved stays unresolved.
- [ ] Source/date/DB version visible; stale/revoked banners render.
- [ ] Large font does not truncate strength/dosage form.
- [ ] TalkBack reads confidence/warnings/source.
- [ ] Recents thumbnail and screenshots blocked on private screens (FLAG_SECURE).
- [ ] Process death mid-scan recovers; temp files cleaned on restart.
- [ ] Delete-all removes private DB, images, key, caches.
- [ ] No ANR in the scan pipeline benchmark.

## Reporting

Record device, OS, result, and any defect ID per row. File a synthetic regression
fixture for every defect (09_TEST_QUALITY.md §7).
