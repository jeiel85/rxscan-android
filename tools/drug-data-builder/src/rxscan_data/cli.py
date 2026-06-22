from __future__ import annotations

import argparse
import os
from pathlib import Path

from rxscan_data.contracts import load_manifest_contract
from rxscan_data.fetcher import DataBuilderError, build_from_fixture, fetch_operation, write_build_artifacts
from rxscan_data.mfds import find_operation, find_source, list_sources


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="rxscan-data",
        description="RxScan official-data builder bootstrap CLI.",
    )
    parser.add_argument(
        "--schema",
        type=Path,
        default=Path("schemas/dataset_manifest.schema.json"),
        help="Path to the dataset manifest JSON Schema.",
    )
    parser.add_argument(
        "--describe",
        action="store_true",
        help="Print the current manifest contract summary.",
    )
    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser(
        "list-sources",
        help="List configured official sources and MFDS operations.",
    )

    fetch = subparsers.add_parser(
        "fetch",
        help="Fetch one official source operation using DATA_GO_KR_SERVICE_KEY.",
    )
    fetch.add_argument("--source", required=True, help="Source ID from the MFDS source registry.")
    fetch.add_argument("--operation", required=True, help="Operation ID to fetch.")
    fetch.add_argument("--out", type=Path, required=True, help="Output directory for snapshot artifacts.")
    fetch.add_argument("--page-size", type=int, default=100, help="numOfRows request size.")
    fetch.add_argument("--max-pages", type=int, default=None, help="Optional page limit for dry runs.")
    fetch.add_argument("--timeout-seconds", type=float, default=20.0, help="Per-request timeout.")
    fetch.add_argument("--retries", type=int, default=2, help="Timeout retries per page.")

    fixture = subparsers.add_parser(
        "build-fixture",
        help="Build deterministic artifacts from a synthetic API-shaped fixture.",
    )
    fixture.add_argument("--source", required=True, help="Source ID from the MFDS source registry.")
    fixture.add_argument("--operation", required=True, help="Operation ID to parse.")
    fixture.add_argument("--fixture", type=Path, required=True, help="Synthetic fixture response path.")
    fixture.add_argument("--out", type=Path, required=True, help="Output directory for fixture artifacts.")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    contract = load_manifest_contract(args.schema)

    if args.describe:
        print(contract.describe())
        return 0

    if args.command == "list-sources":
        for source in list_sources():
            requirement = "required" if source.required else "optional"
            print(f"{source.source_id} ({requirement})")
            for operation in source.operations:
                print(f"  - {operation.operation_id}: {operation.summary}")
        return 0

    if args.command == "fetch":
        try:
            source = find_source(args.source)
            operation = find_operation(source, args.operation)
            service_key = os.environ.get(source.service_key_env, "")
            pages = fetch_operation(
                source,
                operation,
                service_key,
                num_of_rows=args.page_size,
                max_pages=args.max_pages,
                timeout_seconds=args.timeout_seconds,
                retries=args.retries,
            )
            artifacts = write_build_artifacts(source, operation, pages, args.out)
        except (DataBuilderError, KeyError) as exc:
            print(f"error: {exc}")
            return 2
        print(f"wrote {artifacts.record_count} records to {artifacts.normalized_path}")
        print(f"report: {artifacts.report_path}")
        return 0

    if args.command == "build-fixture":
        try:
            source = find_source(args.source)
            operation = find_operation(source, args.operation)
            artifacts = build_from_fixture(source, operation, args.fixture, args.out)
        except (DataBuilderError, KeyError) as exc:
            print(f"error: {exc}")
            return 2
        print(f"wrote {artifacts.record_count} fixture records to {artifacts.normalized_path}")
        print(f"report: {artifacts.report_path}")
        return 0

    print("RxScan data builder bootstrap. Use --describe for contract details.")
    return 0
