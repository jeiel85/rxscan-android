from __future__ import annotations

import argparse
from pathlib import Path

from rxscan_data.contracts import load_manifest_contract


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
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    contract = load_manifest_contract(args.schema)

    if args.describe:
        print(contract.describe())
        return 0

    print("RxScan data builder bootstrap. Use --describe for contract details.")
    return 0

