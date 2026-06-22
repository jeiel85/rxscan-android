from __future__ import annotations

import argparse
import functools
import http.server
import os
import socketserver
from pathlib import Path

from rxscan_data.contracts import load_manifest_contract
from rxscan_data.fetcher import DataBuilderError, build_from_fixture, fetch_operation, write_build_artifacts
from rxscan_data.mfds import find_operation, find_source, list_sources
from rxscan_data.public_db import (
    PublicDbBuildError,
    PublicDbVerifyError,
    build_public_database,
    load_private_key_from_base64,
    load_public_key_from_base64,
    verify_public_dataset,
)


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

    public_db = subparsers.add_parser(
        "build-public-db",
        help="Build signed public SQLite artifact and manifest from normalized records.",
    )
    public_db.add_argument("--input", type=Path, required=True, help="Directory containing normalized records.jsonl files.")
    public_db.add_argument("--out", type=Path, required=True, help="Output directory for public DB artifacts.")
    public_db.add_argument("--artifact-version", required=True, help="Artifact version, e.g. 20260623-1.")
    public_db.add_argument("--created-at-utc", required=True, help="Manifest creation timestamp.")
    public_db.add_argument("--signing-key-id", required=True, help="Public signing key identifier.")
    public_db.add_argument("--min-app-version", default="0.1.0", help="Minimum compatible app version.")
    public_db.add_argument("--max-app-version", default=None, help="Optional maximum compatible app version.")
    public_db.add_argument(
        "--private-key-env",
        default="RXSCAN_DATASET_SIGNING_PRIVATE_KEY_B64",
        help="Env var containing raw Ed25519 private key bytes as base64.",
    )
    public_db.add_argument("--db-schema", type=Path, default=Path("schemas/drug_db_schema.sql"))
    public_db.add_argument("--manifest-schema", type=Path, default=Path("schemas/dataset_manifest.schema.json"))

    verify = subparsers.add_parser(
        "verify-public-db",
        help="Verify manifest signature and public DB artifact hashes.",
    )
    verify.add_argument("--manifest", type=Path, required=True)
    verify.add_argument("--signature", type=Path, required=True)
    verify.add_argument("--artifact-root", type=Path, required=True)
    verify.add_argument("--public-key-base64", required=True)

    serve = subparsers.add_parser(
        "serve-public-db",
        help="Serve a public DB artifact directory with Python's static HTTP server.",
    )
    serve.add_argument("--dir", type=Path, required=True)
    serve.add_argument("--port", type=int, default=8765)
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

    if args.command == "build-public-db":
        try:
            private_key_value = os.environ.get(args.private_key_env, "")
            private_key = load_private_key_from_base64(private_key_value)
            result = build_public_database(
                input_dir=args.input,
                out_dir=args.out,
                schema_path=args.db_schema,
                manifest_schema_path=args.manifest_schema,
                private_key=private_key,
                signing_key_id=args.signing_key_id,
                artifact_version=args.artifact_version,
                created_at_utc=args.created_at_utc,
                min_app_version=args.min_app_version,
                max_app_version=args.max_app_version,
            )
        except (PublicDbBuildError, DataBuilderError) as exc:
            print(f"error: {exc}")
            return 2
        print(f"sqlite: {result.sqlite_path}")
        print(f"artifact: {result.compressed_path}")
        print(f"manifest: {result.manifest_path}")
        print(f"signature: {result.signature_path}")
        print(f"products: {result.product_count}")
        return 0

    if args.command == "verify-public-db":
        try:
            public_key = load_public_key_from_base64(args.public_key_base64)
            verify_public_dataset(
                manifest_path=args.manifest,
                signature_path=args.signature,
                artifact_root=args.artifact_root,
                public_key=public_key,
            )
        except (PublicDbVerifyError, DataBuilderError) as exc:
            print(f"error: {exc}")
            return 2
        print("public dataset verification passed")
        return 0

    if args.command == "serve-public-db":
        handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(args.dir))
        with socketserver.TCPServer(("127.0.0.1", args.port), handler) as server:
            print(f"serving {args.dir} at http://127.0.0.1:{args.port}/")
            server.serve_forever()
        return 0

    print("RxScan data builder bootstrap. Use --describe for contract details.")
    return 0
