**Internal test build (preview) — not a production release.**

⚠️ Not cleared for clinical use: pharmacist sign-off, regulatory review, and the
real validation holdout are pending (see `config/release_gates.json`). **Use
synthetic data only — do not photograph a real prescription.**

### What you can try
- **Home → 약봉지 촬영**: live camera with a document-boundary overlay and on-device
  quality guidance (blur / glare / clipping / text size). Korean OCR uses a bundled
  **offline** ML Kit model — no network, no upload.
- **Home → 검토 화면 미리보기 (합성 데이터)**: the mandatory review flow
  (confirm / reject / unresolved, photographed direction vs official candidates,
  plain-language confidence) and the DUR safety screen, using synthetic sample data.

> Note: the full capture → OCR → parse → match → review pipeline is not yet wired
> end-to-end. The review and safety screens use synthetic data in this preview.

### Install
- Android 8.0+ (minSdk 26).
- Allow "install unknown apps", then install **RxScan-v0.1.0-vc2.apk**.
- Private screens block screenshots/recents (FLAG_SECURE) by design.

### Privacy
On-device only, no account, no upload.
Policy: https://jeiel85.github.io/rxscan-android/privacy.html

### Build
`versionName 0.1.0` · `versionCode 2` · `applicationId io.github.jeiel85.rxscan` ·
signed with the release key.
