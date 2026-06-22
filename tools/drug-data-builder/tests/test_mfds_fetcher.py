from __future__ import annotations

import json
import sys
import tempfile
import unittest
import urllib.parse
from pathlib import Path
from typing import TypeAlias, cast


ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "drug-data-builder" / "src"))

from rxscan_data.fetcher import (
    DuplicatePageError,
    FetchTimeoutError,
    MalformedResponseError,
    MissingServiceKeyError,
    SchemaChangedError,
    build_from_fixture,
    fetch_operation,
    normalized_json_lines,
    parse_mfds_json_page,
    write_build_artifacts,
)
from rxscan_data.mfds import find_operation, find_source


JsonValue: TypeAlias = None | bool | int | float | str | list["JsonValue"] | dict[str, "JsonValue"]
JsonObject: TypeAlias = dict[str, JsonValue]


def fixture_bytes(page_no: int, total_count: int, items: list[JsonObject]) -> bytes:
    body = cast(JsonObject, {
        "response": {
            "header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE."},
            "body": {
                "pageNo": page_no,
                "numOfRows": 1,
                "totalCount": total_count,
                "items": {"item": items},
            },
        }
    })
    return json.dumps(body, ensure_ascii=False, sort_keys=True).encode("utf-8")


class MfdsFetcherTest(unittest.TestCase):
    def setUp(self) -> None:
        self.source = find_source("mfds_easy_drug")
        self.operation = find_operation(self.source, "getDrbEasyDrugList")

    def test_fetches_paginated_source_and_redacts_service_key(self) -> None:
        secret = "SECRET-KEY-123"
        calls: list[str] = []

        def transport(url: str, timeout_seconds: float) -> bytes:
            del timeout_seconds
            calls.append(url)
            query = urllib.parse.parse_qs(urllib.parse.urlsplit(url).query)
            page_no = int(query["pageNo"][0])
            if page_no == 1:
                return fixture_bytes(
                    1,
                    2,
                    [{"itemSeq": "SYNTH-0001", "itemName": "가나다정", "entpName": "합성제약"}],
                )
            return fixture_bytes(
                2,
                2,
                [{"itemSeq": "SYNTH-0002", "itemName": "라마바캡슐", "entpName": "합성제약"}],
            )

        pages = fetch_operation(
            self.source,
            self.operation,
            secret,
            transport=transport,
            num_of_rows=1,
            retries=0,
        )

        self.assertEqual([page.page_no for page in pages], [1, 2])
        self.assertIn(secret, calls[0])
        self.assertNotIn(secret, pages[0].request_url_redacted)
        self.assertIn("<redacted>", pages[0].request_url_redacted)

        with tempfile.TemporaryDirectory() as temp_dir:
            artifacts = write_build_artifacts(self.source, self.operation, pages, Path(temp_dir))
            self.assertEqual(artifacts.record_count, 2)
            for path in (*artifacts.raw_pages, artifacts.normalized_path, artifacts.report_path):
                self.assertNotIn(secret, path.read_text(encoding="utf-8"))

    def test_missing_service_key_fails_before_live_fetch(self) -> None:
        def transport(url: str, timeout_seconds: float) -> bytes:
            del url, timeout_seconds
            return b"{}"

        with self.assertRaises(MissingServiceKeyError):
            fetch_operation(self.source, self.operation, "", transport=transport)

    def test_timeout_retries_and_redacts_url(self) -> None:
        calls = 0

        def transport(url: str, timeout_seconds: float) -> bytes:
            nonlocal calls
            del url, timeout_seconds
            calls += 1
            raise TimeoutError("synthetic timeout")

        with self.assertRaises(FetchTimeoutError) as context:
            fetch_operation(
                self.source,
                self.operation,
                "SECRET-KEY-123",
                transport=transport,
                retries=1,
            )

        self.assertEqual(calls, 2)
        self.assertNotIn("SECRET-KEY-123", str(context.exception))
        self.assertIn("<redacted>", str(context.exception))

    def test_malformed_json_fails_closed(self) -> None:
        with self.assertRaises(MalformedResponseError):
            parse_mfds_json_page(b'{"response":', self.operation, expected_page_no=1)

    def test_xml_response_fails_closed_when_json_was_requested(self) -> None:
        with self.assertRaises(MalformedResponseError):
            parse_mfds_json_page(b"<response><body>", self.operation, expected_page_no=1)

    def test_changed_required_field_fails_closed(self) -> None:
        raw = fixture_bytes(1, 1, [{"itemName": "가나다정", "entpName": "합성제약"}])

        with self.assertRaises(SchemaChangedError):
            parse_mfds_json_page(raw, self.operation, expected_page_no=1)

    def test_duplicate_page_fails_closed(self) -> None:
        def transport(url: str, timeout_seconds: float) -> bytes:
            del url, timeout_seconds
            return fixture_bytes(
                1,
                2,
                [{"itemSeq": "SYNTH-0001", "itemName": "가나다정", "entpName": "합성제약"}],
            )

        with self.assertRaises(DuplicatePageError):
            fetch_operation(
                self.source,
                self.operation,
                "SECRET-KEY-123",
                transport=transport,
                num_of_rows=1,
                retries=0,
            )

    def test_korean_fixture_round_trips_as_utf8(self) -> None:
        parsed = parse_mfds_json_page(
            fixture_bytes(
                1,
                1,
                [{"itemSeq": "SYNTH-0001", "itemName": "가나다정", "entpName": "합성제약"}],
            ),
            self.operation,
            expected_page_no=1,
        )

        normalized = normalized_json_lines(self.source, self.operation, parsed.records)
        self.assertIn("가나다정", normalized)
        self.assertIn("합성제약", normalized)

    def test_normalization_is_byte_for_byte_deterministic(self) -> None:
        records: tuple[JsonObject, ...] = (
            {"itemSeq": "SYNTH-0002", "itemName": "라마바캡슐", "entpName": "합성제약"},
            {"itemSeq": "SYNTH-0001", "itemName": "가나다정", "entpName": "합성제약"},
        )

        first = normalized_json_lines(self.source, self.operation, records)
        second = normalized_json_lines(self.source, self.operation, tuple(reversed(records)))

        self.assertEqual(first.encode("utf-8"), second.encode("utf-8"))

    def test_build_from_synthetic_fixture_writes_report(self) -> None:
        fixture = ROOT / "tools" / "drug-data-builder" / "fixtures" / "synthetic" / "mfds_easy_drug_minimal.json"
        with tempfile.TemporaryDirectory() as temp_dir:
            artifacts = build_from_fixture(self.source, self.operation, fixture, Path(temp_dir))

            self.assertEqual(artifacts.record_count, 1)
            report_text = artifacts.report_path.read_text(encoding="utf-8")
            self.assertIn('"sourceId":"mfds_easy_drug"', report_text)
            self.assertIn('"recordCount":1', report_text)
            self.assertIn("<redacted>", report_text)


if __name__ == "__main__":
    unittest.main()
