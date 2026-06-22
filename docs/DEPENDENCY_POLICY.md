# Dependency Policy

RxScan keeps dependencies minimal because the application handles sensitive
local health-adjacent data.

## Blocked categories

- advertising SDKs;
- behavioral analytics;
- Firebase Analytics;
- remote configuration able to alter medical wording or matching thresholds;
- account or social-login SDKs before a reviewed product need exists;
- cloud OCR or scan-derived server search clients.

## Current Goal 00 dependency surface

- Android Gradle Plugin;
- Kotlin;
- AndroidX Core;
- AndroidX Activity Compose;
- Jetpack Compose UI and Material 3;
- JUnit for local unit tests.

The repository policy check scans Gradle files for known analytics/ad SDK terms
and verifies that the app manifest does not request Internet permission in Goal
00.

