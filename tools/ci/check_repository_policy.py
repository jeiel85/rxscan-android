from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]

FORBIDDEN_GRADLE_TERMS = (
    "firebase-analytics",
    "play-services-ads",
    "appsflyer",
    "adjust-android",
    "facebook-android-sdk",
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def fail(message: str) -> int:
    print(f"policy check failed: {message}", file=sys.stderr)
    return 1


def check_no_internet_permission() -> int:
    manifests = list(ROOT.glob("apps/android/**/src/main/AndroidManifest.xml"))
    for manifest in manifests:
        if "android.permission.INTERNET" in read_text(manifest):
            return fail(f"INTERNET permission found in {manifest.relative_to(ROOT)}")
    return 0


def check_no_analytics_or_ads_dependencies() -> int:
    gradle_files = list(ROOT.glob("**/*.gradle.kts")) + list(ROOT.glob("gradle/*.toml"))
    for gradle_file in gradle_files:
        text = read_text(gradle_file).lower()
        for term in FORBIDDEN_GRADLE_TERMS:
            if term in text:
                return fail(f"forbidden dependency term {term!r} in {gradle_file.relative_to(ROOT)}")
    return 0


def check_no_real_fixture_folder() -> int:
    protected_folder = ROOT / "testdata" / "consented-deidentified"
    if protected_folder.exists():
        return fail(f"protected corpus folder must not be committed: {protected_folder}")
    return 0


def main() -> int:
    checks = (
        check_no_internet_permission,
        check_no_analytics_or_ads_dependencies,
        check_no_real_fixture_folder,
    )
    for check in checks:
        result = check()
        if result != 0:
            return result
    print("repository policy checks passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

