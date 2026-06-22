from __future__ import annotations

import functools
import http.server
import json
import shutil
import sqlite3
import sys
import tempfile
import threading
import unittest
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "drug-data-builder" / "src"))

from rxscan_data.fetcher import build_from_fixture
from rxscan_data.mfds import find_operation, find_source
from rxscan_data.public_db import (
    PublicDbVerifyError,
    build_public_database,
    copy_public_dataset_to_static_dir,
    epoch_ms_to_iso_utc,
    generate_ed25519_private_key,
    private_key_to_base64,
    safe_artifact_path,
    verify_public_dataset,
)


CREATED_AT = "2026-06-23T00:00:00Z"
ARTIFACT_VERSION = "20260623-1"


class _QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format: str, *args: object) -> None:
        del format, args


class PublicDbTest(unittest.TestCase):
    def test_builds_signed_reproducible_sqlite_and_search_index(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            staging = build_easy_fixture_staging(temp / "staging")
            private_key = generate_ed25519_private_key()

            first = build_public_database(
                input_dir=staging,
                out_dir=temp / "out-a",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )
            second = build_public_database(
                input_dir=staging,
                out_dir=temp / "out-b",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )

            self.assertEqual(first.sqlite_path.read_bytes(), second.sqlite_path.read_bytes())
            self.assertEqual(first.compressed_path.read_bytes(), second.compressed_path.read_bytes())
            self.assertEqual(first.product_count, 1)

            connection = sqlite3.connect(first.sqlite_path)
            try:
                self.assertEqual(connection.execute("PRAGMA quick_check").fetchone(), ("ok",))
                self.assertEqual(connection.execute("PRAGMA foreign_key_check").fetchall(), [])
                rows = connection.execute(
                    "SELECT item_code FROM drug_search_fts WHERE drug_search_fts MATCH ?",
                    ("합성테스트정",),
                ).fetchall()
                self.assertEqual(rows, [("SYNTH-0001",)])
            finally:
                connection.close()

            verify_public_dataset(
                manifest_path=first.manifest_path,
                signature_path=first.signature_path,
                artifact_root=first.manifest_path.parent,
                public_key=private_key.public_key(),
            )

            private_key_b64 = private_key_to_base64(private_key)
            for text_path in (first.manifest_path, first.conflict_report_path):
                self.assertNotIn(private_key_b64, text_path.read_text(encoding="utf-8"))

    def test_manifest_and_artifact_tampering_fails_verification(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            staging = build_easy_fixture_staging(temp / "staging")
            private_key = generate_ed25519_private_key()
            result = build_public_database(
                input_dir=staging,
                out_dir=temp / "out",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )

            tampered_manifest = temp / "manifest-tampered.json"
            shutil.copy2(result.manifest_path, tampered_manifest)
            tampered_manifest.write_bytes(result.manifest_path.read_bytes() + b" ")
            with self.assertRaises(PublicDbVerifyError):
                verify_public_dataset(
                    manifest_path=tampered_manifest,
                    signature_path=result.signature_path,
                    artifact_root=result.manifest_path.parent,
                    public_key=private_key.public_key(),
                )

            result.compressed_path.write_bytes(result.compressed_path.read_bytes() + b"0")
            with self.assertRaises(PublicDbVerifyError):
                verify_public_dataset(
                    manifest_path=result.manifest_path,
                    signature_path=result.signature_path,
                    artifact_root=result.manifest_path.parent,
                    public_key=private_key.public_key(),
                )

    def test_missing_easy_drug_info_is_not_generated(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            staging = temp / "staging"
            write_approval_only_staging(staging)
            private_key = generate_ed25519_private_key()
            result = build_public_database(
                input_dir=staging,
                out_dir=temp / "out",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )

            connection = sqlite3.connect(result.sqlite_path)
            try:
                product_count = connection.execute("SELECT COUNT(*) FROM drug_product").fetchone()
                easy_count = connection.execute("SELECT COUNT(*) FROM easy_drug_info").fetchone()
                self.assertEqual(product_count, (1,))
                self.assertEqual(easy_count, (0,))
            finally:
                connection.close()

    def test_static_server_fixture_supports_android_download_and_verify(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            staging = build_easy_fixture_staging(temp / "staging")
            private_key = generate_ed25519_private_key()
            result = build_public_database(
                input_dir=staging,
                out_dir=temp / "out",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )

            static_dir = temp / "static"
            copy_public_dataset_to_static_dir(result, static_dir)
            artifact_name = result.compressed_path.name
            self.assertTrue((static_dir / "manifest.json").exists())
            self.assertTrue((static_dir / "manifest.sig").exists())
            self.assertTrue((static_dir / "artifacts" / artifact_name).exists())

            download_dir = self.serve_and_download(
                static_dir,
                ("manifest.json", "manifest.sig", f"artifacts/{artifact_name}"),
                temp / "download",
            )

            # The Android client downloads over HTTP, then verifies the signature
            # and hashes entirely offline against its embedded public key.
            verify_public_dataset(
                manifest_path=download_dir / "manifest.json",
                signature_path=download_dir / "manifest.sig",
                artifact_root=download_dir,
                public_key=private_key.public_key(),
            )

            tampered = download_dir / "artifacts" / artifact_name
            tampered.write_bytes(tampered.read_bytes() + b"x")
            with self.assertRaises(PublicDbVerifyError):
                verify_public_dataset(
                    manifest_path=download_dir / "manifest.json",
                    signature_path=download_dir / "manifest.sig",
                    artifact_root=download_dir,
                    public_key=private_key.public_key(),
                )

    def test_manifest_records_real_fetch_timestamp(self) -> None:
        self.assertEqual(epoch_ms_to_iso_utc(0), "1970-01-01T00:00:00Z")
        self.assertEqual(epoch_ms_to_iso_utc(1000), "1970-01-01T00:00:01Z")
        epoch_ms = 1_750_000_000_000
        expected = epoch_ms_to_iso_utc(epoch_ms)
        self.assertTrue(expected.startswith("2025-") and expected.endswith("Z"))

        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            staging = temp / "staging"
            write_normalized_source(
                staging,
                source_id="mfds_drug_approval",
                operation_id="getDrugPrdtPrmsnInq07",
                official={"ITEM_SEQ": "SYNTH-TS-0001", "ITEM_NAME": "합성시각정", "ENTP_NAME": "합성제약"},
                endpoint="https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07/getDrugPrdtPrmsnInq07",
                fetched_at_epoch_ms=epoch_ms,
            )
            private_key = generate_ed25519_private_key()
            result = build_public_database(
                input_dir=staging,
                out_dir=temp / "out",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )

            manifest = json.loads(result.manifest_path.read_text(encoding="utf-8"))
            snapshots = manifest["sourceSnapshots"]
            self.assertEqual(len(snapshots), 1)
            self.assertEqual(snapshots[0]["fetchedAtUtc"], expected)
            self.assertNotEqual(snapshots[0]["fetchedAtUtc"], "1970-01-01T00:00:00Z")

    def test_safe_artifact_path_rejects_traversal_and_siblings(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir) / "out"
            (root / "artifacts").mkdir(parents=True)
            (root.parent / "out_evil").mkdir()

            self.assertEqual(
                safe_artifact_path(root, "artifacts/db.sqlite.gz"),
                (root / "artifacts" / "db.sqlite.gz").resolve(),
            )
            for bad in ("/etc/passwd", "../out_evil/x", "a/../../x", "C:\\Windows\\x", "D:evil", "..\\x"):
                with self.assertRaises(PublicDbVerifyError):
                    safe_artifact_path(root, bad)

    def test_builds_ingredient_and_dur_rows(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            staging = temp / "staging"
            write_normalized_source(
                staging,
                source_id="mfds_drug_approval",
                operation_id="getDrugPrdtMcpnDtlInq07",
                official={
                    "ITEM_SEQ": "SYNTH-ING-0001",
                    "ITEM_NAME": "합성성분정",
                    "ENTP_NAME": "합성제약",
                    "MTRAL_NM": "아세트아미노펜",
                    "MTRAL_CODE": "ING-001",
                    "INGD_UNIT_CD": "mg",
                },
                endpoint="https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07/getDrugPrdtMcpnDtlInq07",
            )
            write_normalized_source(
                staging,
                source_id="mfds_dur_product",
                operation_id="getUsjntTabooInfoList03",
                official={
                    "ITEM_SEQ": "SYNTH-ING-0001",
                    "ITEM_NAME": "합성성분정",
                    "TYPE_NAME": "병용금기",
                    "PROHBT_CONTENT": "합성 fixture 병용금기 설명입니다.",
                    "INGR_CODE": "ING-001",
                    "MIXTURE_INGR_CODE": "ING-002",
                },
                endpoint="https://apis.data.go.kr/1471000/DURPrdlstInfoService03/getUsjntTabooInfoList03",
            )

            private_key = generate_ed25519_private_key()
            result = build_public_database(
                input_dir=staging,
                out_dir=temp / "out",
                schema_path=ROOT / "schemas" / "drug_db_schema.sql",
                manifest_schema_path=ROOT / "schemas" / "dataset_manifest.schema.json",
                private_key=private_key,
                signing_key_id="test-key-1",
                artifact_version=ARTIFACT_VERSION,
                created_at_utc=CREATED_AT,
                min_app_version="0.1.0",
            )

            connection = sqlite3.connect(result.sqlite_path)
            try:
                self.assertEqual(connection.execute("PRAGMA quick_check").fetchone(), ("ok",))
                self.assertEqual(connection.execute("PRAGMA foreign_key_check").fetchall(), [])
                self.assertEqual(connection.execute("SELECT COUNT(*) FROM drug_product").fetchone(), (1,))
                self.assertEqual(
                    connection.execute("SELECT ingredient_name FROM ingredient").fetchall(),
                    [("아세트아미노펜",)],
                )
                self.assertEqual(
                    connection.execute(
                        "SELECT item_code, ingredient_code, amount_unit FROM drug_ingredient"
                    ).fetchall(),
                    [("SYNTH-ING-0001", "ING-001", "mg")],
                )
                dur_rows = connection.execute(
                    "SELECT dur_type, subject_ingredient_code, related_ingredient_code, item_code FROM dur_rule"
                ).fetchall()
                self.assertEqual(dur_rows, [("병용금기", "ING-001", "ING-002", "SYNTH-ING-0001")])
                fts_rows = connection.execute(
                    "SELECT item_code FROM drug_search_fts WHERE drug_search_fts MATCH ?",
                    ("ingredients:아세트아미노펜",),
                ).fetchall()
                self.assertEqual(fts_rows, [("SYNTH-ING-0001",)])
            finally:
                connection.close()

    def serve_and_download(self, static_dir: Path, relative_paths: tuple[str, ...], download_dir: Path) -> Path:
        handler = functools.partial(_QuietHandler, directory=str(static_dir))
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
        port = server.server_address[1]
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            for relative in relative_paths:
                destination = download_dir / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                with urllib.request.urlopen(f"http://127.0.0.1:{port}/{relative}", timeout=10) as response:
                    destination.write_bytes(response.read())
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)
        return download_dir


def build_easy_fixture_staging(out_dir: Path) -> Path:
    source = find_source("mfds_easy_drug")
    operation = find_operation(source, "getDrbEasyDrugList")
    fixture = ROOT / "tools" / "drug-data-builder" / "fixtures" / "synthetic" / "mfds_easy_drug_minimal.json"
    build_from_fixture(source, operation, fixture, out_dir)
    return out_dir


def write_approval_only_staging(out_dir: Path) -> None:
    write_normalized_source(
        out_dir,
        source_id="mfds_drug_approval",
        operation_id="getDrugPrdtPrmsnInq07",
        official={"ITEM_SEQ": "SYNTH-APPROVAL-0001", "ITEM_NAME": "합성허가정", "ENTP_NAME": "합성제약"},
        endpoint="https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07/getDrugPrdtPrmsnInq07",
    )


def write_normalized_source(
    out_dir: Path,
    *,
    source_id: str,
    operation_id: str,
    official: dict[str, str],
    endpoint: str,
    fetched_at_epoch_ms: int = 0,
) -> None:
    base = out_dir / source_id / operation_id
    normalized = base / "normalized"
    report_dir = base / "reports"
    normalized.mkdir(parents=True, exist_ok=True)
    report_dir.mkdir(parents=True, exist_ok=True)
    record = {"sourceId": source_id, "operationId": operation_id, "official": official}
    (normalized / "records.jsonl").write_text(
        json.dumps(record, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    report = {
        "sourceId": source_id,
        "operationId": operation_id,
        "endpoint": endpoint,
        "recordCount": 1,
        "rawSha256": ["0" * 64],
        "fetchedAtEpochMs": [fetched_at_epoch_ms],
    }
    (report_dir / "build-report.json").write_text(
        json.dumps(report, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n",
        encoding="utf-8",
        newline="\n",
    )


if __name__ == "__main__":
    unittest.main()
