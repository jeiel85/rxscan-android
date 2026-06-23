# Screenshot Capture Guide

Play requires **2–8 phone screenshots** (JPEG/24-bit PNG, 320–3840 px per side,
max 2:1 ratio). Screenshots must be **real captures of the app** and must not make
unsupported medical claims (Goal 08 acceptance). They are **not generated here** —
fabricated UI shots would misrepresent the product. Capture them from the running
app, then drop them in `play_store/listing/screenshots/`.

## Capture

1. Install the release build on a device/emulator:
   `./gradlew :app:installRelease` (or sideload the AAB via bundletool), Korean
   locale, large-font + TalkBack variants as needed for the device matrix.
2. Use synthetic data only (AGENTS.md) — never a real prescription.
3. Capture, e.g. with `adb exec-out screencap -p > shot.png`.

## Recommended set (matches the implemented screens)

1. **Home** — intro + 약봉지 촬영 entry.
2. **Scan** — camera with document-boundary overlay and live quality guidance.
3. **Review** — medication cards: photographed direction vs official candidates,
   confirm/reject/unresolved, plain-language confidence (no color-only).
4. **Official info** — source agency, item code, date, data version; missing
   easy-info state.
5. **Safety (DUR)** — fixed wording with provenance; insufficient/stale states.

## Caption rules

- Describe what the screen does; no diagnosis/dosing/safety claims.
- Keep captions consistent with `store-listing.md` per locale.

## Other assets

- Hi-res icon: `play_store/listing/icon-512.png` (512×512).
- Feature graphic: `play_store/listing/feature-graphic-1024x500.png` (1024×500).
- Regenerate graphics with `python tools/store_assets/generate_play_assets.py`.
