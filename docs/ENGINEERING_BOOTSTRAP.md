# Engineering Bootstrap

This repository starts at Goal 00 from the design bundle.

## Goal 00 implementation plan

1. Keep the design bundle and strengthen the manifest contract gaps found during review.
2. Create a minimal Android monorepo that builds and launches to a non-medical placeholder home.
3. Add a typed Python package for the official-data builder without fetching live APIs yet.
4. Add CI, dependency locking configuration, policy checks, and synthetic-only test data rules.
5. Create the public README and GitHub Pages landing surface without unsupported medical claims.

## Android module boundary

Gradle exposes the module names from `02_SYSTEM_ARCHITECTURE.md`:

- `:app`
- `:core:model`, `:core:ui`, `:core:logging`, `:core:security`, `:core:testing`
- `:engine:imagequality`, `:engine:document`, `:engine:ocr`, `:engine:parser`, `:engine:matcher`, `:engine:dur`
- `:data:publicdb`, `:data:privatedb`, `:data:updater`
- `:feature:home`, `:feature:scan`, `:feature:review`, `:feature:drugdetail`, `:feature:safety`, `:feature:history`, `:feature:settings`

The files live under `apps/android/` so the repository can also contain the
data builder, distribution infrastructure, documentation, and synthetic test
data.

## Dependency and SDK choices

- Min SDK: 26, from `config/supported_scope.yaml`.
- Compile SDK: 36, the latest installed stable Android SDK in this workstation.
- Target SDK: 36. Checked on 2026-06-22: Google Play requires new apps and app updates to target Android 15/API 35 or higher from 2025-08-31, and Android 16 is API 36.
- Android dependencies are intentionally minimal: AndroidX Core, Activity Compose, Compose UI, and Material 3.
- No analytics, ads, accounts, Firebase Analytics, or remote configuration dependency is included.

## Goal 00 verification commands

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:assembleDebug
python -m unittest discover -s tools/drug-data-builder/tests
npx --yes pyright tools/drug-data-builder
python tools/ci/check_repository_policy.py
```

## Remaining risks

- The app name and package ID are placeholders until the owner selects final branding.
- Official source schemas, terms, and traffic limits must be re-verified before Goal 01 and before release.
- The landing page describes an engineering preview, not a released medical product.
