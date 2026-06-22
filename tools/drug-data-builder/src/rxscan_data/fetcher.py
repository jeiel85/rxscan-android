from __future__ import annotations

import hashlib
import json
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol, TypeAlias, cast

from rxscan_data.mfds import OperationSpec, SourceSpec


JsonValue: TypeAlias = None | bool | int | float | str | list["JsonValue"] | dict[str, "JsonValue"]
JsonObject: TypeAlias = dict[str, JsonValue]


class DataBuilderError(Exception):
    """Base error for deterministic data-builder failures."""


class MissingServiceKeyError(DataBuilderError):
    pass


class FetchTimeoutError(DataBuilderError):
    pass


class MalformedResponseError(DataBuilderError):
    pass


class SchemaChangedError(DataBuilderError):
    pass


class DuplicatePageError(DataBuilderError):
    pass


class PageDiscontinuityError(DataBuilderError):
    pass


class SourceResponseError(DataBuilderError):
    pass


class Transport(Protocol):
    def __call__(self, url: str, timeout_seconds: float) -> bytes:
        ...


@dataclass(frozen=True)
class ParsedPage:
    page_no: int
    total_count: int | None
    raw_sha256: str
    raw_text: str
    records: tuple[JsonObject, ...]


@dataclass(frozen=True)
class SnapshotPage:
    source_id: str
    operation_id: str
    endpoint: str
    page_no: int
    fetched_at_epoch_ms: int
    request_url_redacted: str
    raw_sha256: str
    raw_text: str
    records: tuple[JsonObject, ...]


@dataclass(frozen=True)
class BuildArtifacts:
    raw_pages: tuple[Path, ...]
    normalized_path: Path
    report_path: Path
    record_count: int


def redacted_url(url: str) -> str:
    parsed = urllib.parse.urlsplit(url)
    query_pairs = urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
    redacted_pairs: list[tuple[str, str]] = []
    for key, value in query_pairs:
        if key.lower() in {"servicekey", "api_key", "apikey"}:
            redacted_pairs.append((key, "<redacted>"))
        else:
            redacted_pairs.append((key, value))
    query = urllib.parse.urlencode(redacted_pairs, safe="<>")
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, query, parsed.fragment))


def build_request_url(
    source: SourceSpec,
    operation: OperationSpec,
    service_key: str,
    page_no: int,
    num_of_rows: int,
) -> str:
    endpoint = source.endpoint_for(operation)
    query = urllib.parse.urlencode(
        (
            ("serviceKey", service_key),
            ("pageNo", str(page_no)),
            ("numOfRows", str(num_of_rows)),
            ("type", "json"),
        )
    )
    return f"{endpoint}?{query}"


def urllib_transport(url: str, timeout_seconds: float) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "rxscan-data-builder/0.1"})
    with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
        return response.read()


def fetch_operation(
    source: SourceSpec,
    operation: OperationSpec,
    service_key: str,
    *,
    transport: Transport = urllib_transport,
    num_of_rows: int = 100,
    max_pages: int | None = None,
    timeout_seconds: float = 20.0,
    retries: int = 2,
) -> tuple[SnapshotPage, ...]:
    if service_key.strip() == "":
        raise MissingServiceKeyError(f"{source.service_key_env} is required for live fetches")

    pages: list[SnapshotPage] = []
    seen_pages: set[int] = set()
    page_no = 1

    while True:
        url = build_request_url(source, operation, service_key, page_no, num_of_rows)
        raw = _fetch_with_retries(url, transport, timeout_seconds, retries)
        parsed = parse_mfds_json_page(raw, operation, expected_page_no=page_no)

        if parsed.page_no in seen_pages:
            raise DuplicatePageError(f"{source.source_id}/{operation.operation_id} returned duplicate page {parsed.page_no}")
        if parsed.page_no != page_no:
            raise PageDiscontinuityError(
                f"{source.source_id}/{operation.operation_id} expected page {page_no} but received {parsed.page_no}"
            )

        seen_pages.add(parsed.page_no)
        pages.append(
            SnapshotPage(
                source_id=source.source_id,
                operation_id=operation.operation_id,
                endpoint=source.endpoint_for(operation),
                page_no=parsed.page_no,
                fetched_at_epoch_ms=int(time.time() * 1000),
                request_url_redacted=redacted_url(url),
                raw_sha256=parsed.raw_sha256,
                raw_text=parsed.raw_text,
                records=parsed.records,
            )
        )

        if max_pages is not None and page_no >= max_pages:
            break
        if parsed.total_count is not None and page_no * num_of_rows >= parsed.total_count:
            break
        if len(parsed.records) < num_of_rows:
            break
        page_no += 1

    return tuple(pages)


def parse_mfds_json_page(raw: bytes, operation: OperationSpec, *, expected_page_no: int) -> ParsedPage:
    raw_sha256 = hashlib.sha256(raw).hexdigest()
    try:
        raw_text = raw.decode("utf-8-sig")
    except UnicodeDecodeError as exc:
        raise MalformedResponseError("MFDS response is not valid UTF-8") from exc

    stripped = raw_text.lstrip()
    if stripped.startswith("<"):
        raise MalformedResponseError("MFDS response was XML while JSON was requested")

    try:
        loaded: JsonValue = json.loads(raw_text)
    except json.JSONDecodeError as exc:
        raise MalformedResponseError("MFDS response is not valid JSON") from exc

    root = _require_object(loaded, "response root")
    response = root.get("response")
    if isinstance(response, dict):
        root = cast(JsonObject, response)

    header = _optional_object(root.get("header"), "header")
    if header is not None:
        code = header.get("resultCode")
        if isinstance(code, str) and code not in {"00", "0", "0000"}:
            message = header.get("resultMsg")
            detail = message if isinstance(message, str) else "unknown public-data error"
            raise SourceResponseError(f"MFDS returned resultCode={code}: {detail}")

    body = _require_child_object(root, "body")
    page_no = _optional_int(body.get("pageNo"), expected_page_no)
    total_count = _optional_int_or_none(body.get("totalCount"))
    records = _extract_records(body)

    for index, record in enumerate(records):
        for field in operation.required_fields:
            value = record.get(field)
            if value is None or value == "":
                raise SchemaChangedError(
                    f"{operation.operation_id} record {index} is missing required field {field}"
                )

    return ParsedPage(
        page_no=page_no,
        total_count=total_count,
        raw_sha256=raw_sha256,
        raw_text=raw_text,
        records=tuple(records),
    )


def normalized_json_lines(source: SourceSpec, operation: OperationSpec, records: tuple[JsonObject, ...]) -> str:
    wrapped: list[JsonObject] = [
        {
            "sourceId": source.source_id,
            "operationId": operation.operation_id,
            "official": record,
        }
        for record in records
    ]
    wrapped.sort(key=_canonical_json)
    return "".join(f"{_canonical_json(item)}\n" for item in wrapped)


def write_build_artifacts(
    source: SourceSpec,
    operation: OperationSpec,
    pages: tuple[SnapshotPage, ...],
    out_dir: Path,
) -> BuildArtifacts:
    source_dir = out_dir / source.source_id / operation.operation_id
    raw_dir = source_dir / "raw"
    normalized_dir = source_dir / "normalized"
    report_dir = source_dir / "reports"
    raw_dir.mkdir(parents=True, exist_ok=True)
    normalized_dir.mkdir(parents=True, exist_ok=True)
    report_dir.mkdir(parents=True, exist_ok=True)

    raw_paths: list[Path] = []
    all_records: list[JsonObject] = []
    for page in pages:
        raw_path = raw_dir / f"page-{page.page_no:04}.json"
        raw_path.write_text(page.raw_text, encoding="utf-8", newline="\n")
        raw_paths.append(raw_path)
        all_records.extend(page.records)

    normalized_path = normalized_dir / "records.jsonl"
    normalized_text = normalized_json_lines(source, operation, tuple(all_records))
    normalized_path.write_text(normalized_text, encoding="utf-8", newline="\n")

    report = {
        "sourceId": source.source_id,
        "operationId": operation.operation_id,
        "endpoint": source.endpoint_for(operation),
        "requiredSource": source.required,
        "recordCount": len(all_records),
        "pageCount": len(pages),
        "rawSha256": [page.raw_sha256 for page in pages],
        "normalizedSha256": hashlib.sha256(normalized_text.encode("utf-8")).hexdigest(),
        "requestUrls": [page.request_url_redacted for page in pages],
        "fetchedAtEpochMs": [page.fetched_at_epoch_ms for page in pages],
    }
    report_text = _canonical_json(cast(JsonObject, report)) + "\n"
    report_path = report_dir / "build-report.json"
    report_path.write_text(report_text, encoding="utf-8", newline="\n")

    return BuildArtifacts(
        raw_pages=tuple(raw_paths),
        normalized_path=normalized_path,
        report_path=report_path,
        record_count=len(all_records),
    )


def build_from_fixture(
    source: SourceSpec,
    operation: OperationSpec,
    fixture_path: Path,
    out_dir: Path,
) -> BuildArtifacts:
    raw = fixture_path.read_bytes()
    parsed = parse_mfds_json_page(raw, operation, expected_page_no=1)
    snapshot = SnapshotPage(
        source_id=source.source_id,
        operation_id=operation.operation_id,
        endpoint=source.endpoint_for(operation),
        page_no=parsed.page_no,
        fetched_at_epoch_ms=0,
        request_url_redacted=f"{source.endpoint_for(operation)}?serviceKey=<redacted>&pageNo=1&numOfRows=fixture&type=json",
        raw_sha256=parsed.raw_sha256,
        raw_text=parsed.raw_text,
        records=parsed.records,
    )
    return write_build_artifacts(source, operation, (snapshot,), out_dir)


def _fetch_with_retries(url: str, transport: Transport, timeout_seconds: float, retries: int) -> bytes:
    attempts = retries + 1
    for attempt in range(1, attempts + 1):
        try:
            return transport(url, timeout_seconds)
        except TimeoutError as exc:
            if attempt == attempts:
                raise FetchTimeoutError(f"Timed out fetching {redacted_url(url)}") from exc
    raise FetchTimeoutError(f"Timed out fetching {redacted_url(url)}")


def _canonical_json(value: JsonObject) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def _require_object(value: JsonValue, label: str) -> JsonObject:
    if not isinstance(value, dict):
        raise MalformedResponseError(f"{label} must be an object")
    return cast(JsonObject, value)


def _require_child_object(parent: JsonObject, key: str) -> JsonObject:
    return _require_object(parent.get(key), key)


def _optional_object(value: JsonValue, label: str) -> JsonObject | None:
    if value is None:
        return None
    return _require_object(value, label)


def _optional_int(value: JsonValue, fallback: int) -> int:
    parsed = _optional_int_or_none(value)
    return fallback if parsed is None else parsed


def _optional_int_or_none(value: JsonValue) -> int | None:
    if value is None:
        return None
    if isinstance(value, bool):
        raise MalformedResponseError("numeric field must not be boolean")
    if isinstance(value, int):
        return value
    if isinstance(value, str) and value.strip().isdigit():
        return int(value)
    raise MalformedResponseError("numeric field must be an integer")


def _extract_records(body: JsonObject) -> tuple[JsonObject, ...]:
    items = body.get("items")
    if items is None:
        return ()

    candidate: JsonValue
    if isinstance(items, list):
        candidate = items
    elif isinstance(items, dict):
        candidate = items.get("item")
    else:
        raise MalformedResponseError("items must be an object or array")

    if candidate is None:
        return ()
    if isinstance(candidate, dict):
        return (_require_object(candidate, "item"),)
    if isinstance(candidate, list):
        records: list[JsonObject] = []
        for index, item in enumerate(candidate):
            records.append(_require_object(item, f"item[{index}]"))
        return tuple(records)

    raise MalformedResponseError("item must be an object or array")
