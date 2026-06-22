from __future__ import annotations

import re
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "drug-data-builder" / "src"))

from rxscan_data.contracts import load_manifest_contract


class ManifestContractTest(unittest.TestCase):
    def test_manifest_requires_integrity_and_revocation_fields(self) -> None:
        contract = load_manifest_contract(ROOT / "schemas" / "dataset_manifest.schema.json")

        for field in (
            "freshness",
            "revokedVersions",
            "emergencyMessageId",
            "signingKeyId",
            "maxAppVersion",
        ):
            with self.subTest(field=field):
                self.assertIn(field, contract.required_fields)

    def test_artifact_path_rejects_absolute_and_parent_segments(self) -> None:
        contract = load_manifest_contract(ROOT / "schemas" / "dataset_manifest.schema.json")
        pattern = re.compile(contract.artifact_path_pattern)

        self.assertIsNotNone(pattern.fullmatch("artifacts/rxscan-drugs-20260622-1.sqlite.zst"))
        self.assertIsNone(pattern.fullmatch("/artifacts/rxscan.sqlite.zst"))
        self.assertIsNone(pattern.fullmatch("../rxscan.sqlite.zst"))
        self.assertIsNone(pattern.fullmatch("artifacts/../rxscan.sqlite.zst"))

    def test_source_snapshots_keep_fetch_provenance(self) -> None:
        contract = load_manifest_contract(ROOT / "schemas" / "dataset_manifest.schema.json")

        self.assertIn("endpoint", contract.source_snapshot_required_fields)
        self.assertIn("builderVersion", contract.source_snapshot_required_fields)


if __name__ == "__main__":
    unittest.main()
