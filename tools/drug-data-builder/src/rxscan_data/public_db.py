from __future__ import annotations

import base64
import gzip
import hashlib
import json
import re
import shutil
import sqlite3
import io
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import TypeAlias, cast

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey

from rxscan_data.contracts import load_manifest_contract
from rxscan_data.fetcher import DataBuilderError


BUILDER_VERSION = "rxscan-data-builder/0.2.0"
SCHEMA_VERSION = 1

# `e약은요` text is stored verbatim in this bootstrap: only surrounding whitespace
# is trimmed, no HTML transformation is applied. The version string records that
# provenance honestly. An allowlist HTML sanitizer must be introduced (and this
# version bumped) before ingesting live `e약은요` HTML, per 05_DATA_PLATFORM.md §5.
EASY_INFO_SANITIZER_VERSION = "verbatim-passthrough-v1"

EPOCH_UTC = "1970-01-01T00:00:00Z"


def epoch_ms_to_iso_utc(epoch_ms: int) -> str:
    """Convert epoch milliseconds to a deterministic RFC3339 UTC timestamp."""
    return datetime.fromtimestamp(epoch_ms / 1000, tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

JsonValue: TypeAlias = None | bool | int | float | str | list["JsonValue"] | dict[str, "JsonValue"]
JsonObject: TypeAlias = dict[str, JsonValue]


class PublicDbBuildError(DataBuilderError):
    pass


class PublicDbVerifyError(DataBuilderError):
    pass


@dataclass(frozen=True)
class PublicDbBuildResult:
    sqlite_path: Path
    compressed_path: Path
    manifest_path: Path
    signature_path: Path
    conflict_report_path: Path
    record_count: int
    product_count: int


@dataclass(frozen=True)
class ProductRow:
    item_code: str
    product_name: str
    manufacturer_name: str | None
    professional_class: str | None
    approval_date: str | None
    source_updated_at: str | None
    source_id: str


@dataclass(frozen=True)
class IngredientRow:
    ingredient_code: str
    ingredient_name: str


@dataclass(frozen=True)
class DrugIngredientRow:
    item_code: str
    ingredient_code: str
    amount_value: float | None
    amount_unit: str | None


@dataclass(frozen=True)
class EasyInfoRow:
    item_code: str
    efficacy_html: str | None
    use_method_html: str | None
    warning_html: str | None
    caution_html: str | None
    interaction_html: str | None
    adverse_effect_html: str | None
    storage_html: str | None
    open_date: str | None
    update_date: str | None
    content_sha256: str
    sanitizer_version: str
    source_id: str


@dataclass(frozen=True)
class DurRuleRow:
    dur_rule_id: str
    dur_type: str
    subject_ingredient_code: str | None
    related_ingredient_code: str | None
    item_code: str | None
    dosage_form: str | None
    notice_date: str | None
    content_text: str
    source_id: str


@dataclass(frozen=True)
class SourceSnapshotRow:
    source_id: str
    snapshot_id: str
    fetched_at_utc: str
    source_updated_at: str | None
    endpoint: str
    raw_sha256: str
    record_count: int
    builder_version: str


@dataclass(frozen=True)
class DatabaseRows:
    products: tuple[ProductRow, ...]
    ingredients: tuple[IngredientRow, ...]
    drug_ingredients: tuple[DrugIngredientRow, ...]
    easy_infos: tuple[EasyInfoRow, ...]
    dur_rules: tuple[DurRuleRow, ...]
    source_snapshots: tuple[SourceSnapshotRow, ...]
    conflicts: tuple[JsonObject, ...]
    normalized_record_count: int


def generate_ed25519_private_key() -> Ed25519PrivateKey:
    return Ed25519PrivateKey.generate()


def private_key_to_base64(private_key: Ed25519PrivateKey) -> str:
    raw = private_key.private_bytes(
        encoding=serialization.Encoding.Raw,
        format=serialization.PrivateFormat.Raw,
        encryption_algorithm=serialization.NoEncryption(),
    )
    return base64.b64encode(raw).decode("ascii")


def public_key_to_base64(public_key: Ed25519PublicKey) -> str:
    raw = public_key.public_bytes(
        encoding=serialization.Encoding.Raw,
        format=serialization.PublicFormat.Raw,
    )
    return base64.b64encode(raw).decode("ascii")


def load_private_key_from_base64(value: str) -> Ed25519PrivateKey:
    try:
        raw = base64.b64decode(value, validate=True)
    except ValueError as exc:
        raise PublicDbBuildError("Signing private key must be base64") from exc
    if len(raw) != 32:
        raise PublicDbBuildError("Ed25519 raw private key must be 32 bytes")
    return Ed25519PrivateKey.from_private_bytes(raw)


def load_public_key_from_base64(value: str) -> Ed25519PublicKey:
    try:
        raw = base64.b64decode(value, validate=True)
    except ValueError as exc:
        raise PublicDbVerifyError("Verification public key must be base64") from exc
    if len(raw) != 32:
        raise PublicDbVerifyError("Ed25519 raw public key must be 32 bytes")
    return Ed25519PublicKey.from_public_bytes(raw)


def build_public_database(
    *,
    input_dir: Path,
    out_dir: Path,
    schema_path: Path,
    manifest_schema_path: Path,
    private_key: Ed25519PrivateKey,
    signing_key_id: str,
    artifact_version: str,
    created_at_utc: str,
    min_app_version: str,
    max_app_version: str | None = None,
) -> PublicDbBuildResult:
    if not re.fullmatch(r"[0-9]{8}-[0-9]+", artifact_version):
        raise PublicDbBuildError("artifact_version must match YYYYMMDD-N")

    rows = load_database_rows(input_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    artifact_dir = out_dir / "artifacts"
    artifact_dir.mkdir(parents=True, exist_ok=True)

    sqlite_path = artifact_dir / f"rxscan-drugs-{artifact_version}.sqlite"
    compressed_path = artifact_dir / f"rxscan-drugs-{artifact_version}.sqlite.gz"
    manifest_path = out_dir / "manifest.json"
    signature_path = out_dir / "manifest.sig"
    conflict_report_path = out_dir / "conflict-report.json"

    for path in (sqlite_path, compressed_path, manifest_path, signature_path, conflict_report_path):
        if path.exists():
            path.unlink()

    create_sqlite_database(sqlite_path, schema_path, rows, artifact_version)
    quick_check_sqlite(sqlite_path)

    uncompressed_bytes = sqlite_path.read_bytes()
    gzip_bytes = gzip_deterministic(uncompressed_bytes)
    compressed_path.write_bytes(gzip_bytes)

    conflict_report = {
        "artifactVersion": artifact_version,
        "conflictCount": len(rows.conflicts),
        "conflicts": list(rows.conflicts),
    }
    write_json(conflict_report_path, cast(JsonObject, conflict_report))

    manifest = build_manifest(
        rows=rows,
        artifact_version=artifact_version,
        created_at_utc=created_at_utc,
        min_app_version=min_app_version,
        max_app_version=max_app_version,
        signing_key_id=signing_key_id,
        compressed_path=compressed_path,
        sqlite_path=sqlite_path,
        artifact_relative_path=f"artifacts/{compressed_path.name}",
    )
    assert_manifest_matches_contract(manifest, manifest_schema_path)
    manifest_bytes = canonical_json_bytes(manifest)
    manifest_path.write_bytes(manifest_bytes)
    signature_path.write_bytes(private_key.sign(manifest_bytes))

    return PublicDbBuildResult(
        sqlite_path=sqlite_path,
        compressed_path=compressed_path,
        manifest_path=manifest_path,
        signature_path=signature_path,
        conflict_report_path=conflict_report_path,
        record_count=rows.normalized_record_count,
        product_count=len(rows.products),
    )


def verify_public_dataset(
    *,
    manifest_path: Path,
    signature_path: Path,
    artifact_root: Path,
    public_key: Ed25519PublicKey,
) -> None:
    manifest_bytes = manifest_path.read_bytes()
    signature = signature_path.read_bytes()
    try:
        public_key.verify(signature, manifest_bytes)
    except InvalidSignature as exc:
        raise PublicDbVerifyError("Manifest signature verification failed") from exc

    manifest = require_object(json.loads(manifest_bytes.decode("utf-8")), "manifest")
    artifact = require_child_object(manifest, "artifact")
    artifact_path = require_string(artifact, "path")
    compressed_path = safe_artifact_path(artifact_root, artifact_path)
    compressed_bytes = compressed_path.read_bytes()

    expected_compressed_sha = require_string(artifact, "compressedSha256")
    actual_compressed_sha = sha256_bytes(compressed_bytes)
    if actual_compressed_sha != expected_compressed_sha:
        raise PublicDbVerifyError("Compressed artifact SHA-256 mismatch")

    expected_compressed_bytes = require_int(artifact, "compressedBytes")
    if len(compressed_bytes) != expected_compressed_bytes:
        raise PublicDbVerifyError("Compressed artifact size mismatch")

    try:
        uncompressed_bytes = gzip.decompress(compressed_bytes)
    except OSError as exc:
        raise PublicDbVerifyError("Compressed artifact is not valid gzip") from exc

    expected_uncompressed_sha = require_string(artifact, "uncompressedSha256")
    if sha256_bytes(uncompressed_bytes) != expected_uncompressed_sha:
        raise PublicDbVerifyError("Uncompressed artifact SHA-256 mismatch")

    expected_uncompressed_bytes = require_int(artifact, "uncompressedBytes")
    if len(uncompressed_bytes) != expected_uncompressed_bytes:
        raise PublicDbVerifyError("Uncompressed artifact size mismatch")


def load_database_rows(input_dir: Path) -> DatabaseRows:
    normalized_records = read_normalized_records(input_dir)
    reports = read_build_reports(input_dir)

    products: dict[str, ProductRow] = {}
    ingredients: dict[str, IngredientRow] = {}
    drug_ingredients: dict[tuple[str, str], DrugIngredientRow] = {}
    easy_infos: dict[str, EasyInfoRow] = {}
    dur_rules: dict[str, DurRuleRow] = {}
    conflicts: list[JsonObject] = []

    for record in normalized_records:
        source_id = require_string(record, "sourceId")
        operation_id = require_string(record, "operationId")
        official = require_child_object(record, "official")

        item_code = first_string(official, ("ITEM_SEQ", "itemSeq"))
        product_name = first_string(official, ("ITEM_NAME", "itemName", "PRDUCT"))
        manufacturer_name = first_string(official, ("ENTP_NAME", "entpName", "ENTRPS"))

        if item_code is not None and product_name is not None:
            candidate = ProductRow(
                item_code=item_code,
                product_name=product_name,
                manufacturer_name=manufacturer_name,
                professional_class=first_string(official, ("ETC_OTC_CODE", "ETC_OTC_NAME", "spcltyPblc")),
                approval_date=first_string(official, ("ITEM_PERMIT_DATE", "itemPermitDate")),
                source_updated_at=first_string(official, ("updateDe", "CHANGE_DATE")),
                source_id=source_id,
            )
            previous = products.get(item_code)
            if previous is None:
                products[item_code] = candidate
            elif normalize_search_text(previous.product_name) != normalize_search_text(candidate.product_name):
                conflicts.append(
                    {
                        "type": "product_name_conflict",
                        "itemCode": item_code,
                        "kept": previous.product_name,
                        "quarantined": candidate.product_name,
                        "sourceId": source_id,
                        "operationId": operation_id,
                    }
                )

        add_ingredient_rows(official, item_code, ingredients, drug_ingredients)

        if source_id == "mfds_easy_drug" and item_code is not None:
            easy_infos[item_code] = make_easy_info(item_code, official, source_id)

        if source_id.startswith("mfds_dur"):
            dur = make_dur_rule(source_id, operation_id, official, item_code)
            if dur is not None:
                dur_rules[dur.dur_rule_id] = dur

    snapshots = make_source_snapshots(reports)

    return DatabaseRows(
        products=tuple(sorted(products.values(), key=lambda item: item.item_code)),
        ingredients=tuple(sorted(ingredients.values(), key=lambda item: item.ingredient_code)),
        drug_ingredients=tuple(sorted(drug_ingredients.values(), key=lambda item: (item.item_code, item.ingredient_code))),
        easy_infos=tuple(sorted(easy_infos.values(), key=lambda item: item.item_code)),
        dur_rules=tuple(sorted(dur_rules.values(), key=lambda item: item.dur_rule_id)),
        source_snapshots=snapshots,
        conflicts=tuple(conflicts),
        normalized_record_count=len(normalized_records),
    )


def read_normalized_records(input_dir: Path) -> tuple[JsonObject, ...]:
    records: list[JsonObject] = []
    for path in sorted(input_dir.rglob("records.jsonl")):
        if path.parent.name != "normalized":
            continue
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            if not line.strip():
                continue
            try:
                value = json.loads(line)
            except json.JSONDecodeError as exc:
                raise PublicDbBuildError(f"{path}:{line_number} is not valid JSON") from exc
            records.append(require_object(value, f"{path}:{line_number}"))
    if not records:
        raise PublicDbBuildError(f"No normalized records found under {input_dir}")
    return tuple(records)


def read_build_reports(input_dir: Path) -> tuple[JsonObject, ...]:
    reports: list[JsonObject] = []
    for path in sorted(input_dir.rglob("build-report.json")):
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            raise PublicDbBuildError(f"{path} is not valid JSON") from exc
        reports.append(require_object(value, str(path)))
    return tuple(reports)


def make_source_snapshots(reports: tuple[JsonObject, ...]) -> tuple[SourceSnapshotRow, ...]:
    snapshots: list[SourceSnapshotRow] = []
    for report in reports:
        source_id = require_string(report, "sourceId")
        operation_id = require_string(report, "operationId")
        raw_hashes = require_string_list(report, "rawSha256")
        fetched_values = require_int_list(report, "fetchedAtEpochMs")
        # Use the latest page fetch time as the snapshot's fetch timestamp. Inputs
        # come from fixed build-report files, so this stays deterministic; the
        # synthetic fixture path records 0, which maps to the Unix epoch.
        fetched_at = epoch_ms_to_iso_utc(max(fetched_values)) if fetched_values else EPOCH_UTC
        snapshots.append(
            SourceSnapshotRow(
                source_id=source_id,
                snapshot_id=operation_id,
                fetched_at_utc=fetched_at,
                source_updated_at=None,
                endpoint=require_string(report, "endpoint"),
                raw_sha256=sha256_text("\n".join(raw_hashes)),
                record_count=require_int(report, "recordCount"),
                builder_version=BUILDER_VERSION,
            )
        )
    if not snapshots:
        snapshots.append(
            SourceSnapshotRow(
                source_id="synthetic",
                snapshot_id="manual",
                fetched_at_utc=EPOCH_UTC,
                source_updated_at=None,
                endpoint="https://example.invalid/synthetic",
                raw_sha256=sha256_text("synthetic"),
                record_count=0,
                builder_version=BUILDER_VERSION,
            )
        )
    return tuple(sorted(snapshots, key=lambda item: (item.source_id, item.snapshot_id)))


def add_ingredient_rows(
    official: JsonObject,
    item_code: str | None,
    ingredients: dict[str, IngredientRow],
    drug_ingredients: dict[tuple[str, str], DrugIngredientRow],
) -> None:
    if item_code is None:
        return
    ingredient_name = first_string(official, ("MTRAL_NM", "INGR_NAME", "INGR_KOR_NAME", "itemIngrName"))
    if ingredient_name is None:
        return
    ingredient_code = first_string(official, ("MTRAL_CODE", "INGR_CODE")) or sha256_text(ingredient_name)[:16]
    ingredients[ingredient_code] = IngredientRow(ingredient_code=ingredient_code, ingredient_name=ingredient_name)
    drug_ingredients[(item_code, ingredient_code)] = DrugIngredientRow(
        item_code=item_code,
        ingredient_code=ingredient_code,
        amount_value=None,
        amount_unit=first_string(official, ("INGD_UNIT_CD", "UNIT")),
    )


def make_easy_info(item_code: str, official: JsonObject, source_id: str) -> EasyInfoRow:
    content = {
        "efcyQesitm": official.get("efcyQesitm"),
        "useMethodQesitm": official.get("useMethodQesitm"),
        "atpnWarnQesitm": official.get("atpnWarnQesitm"),
        "atpnQesitm": official.get("atpnQesitm"),
        "intrcQesitm": official.get("intrcQesitm"),
        "seQesitm": official.get("seQesitm"),
        "depositMethodQesitm": official.get("depositMethodQesitm"),
    }
    return EasyInfoRow(
        item_code=item_code,
        efficacy_html=optional_string(official.get("efcyQesitm")),
        use_method_html=optional_string(official.get("useMethodQesitm")),
        warning_html=optional_string(official.get("atpnWarnQesitm")),
        caution_html=optional_string(official.get("atpnQesitm")),
        interaction_html=optional_string(official.get("intrcQesitm")),
        adverse_effect_html=optional_string(official.get("seQesitm")),
        storage_html=optional_string(official.get("depositMethodQesitm")),
        open_date=optional_string(official.get("openDe")),
        update_date=optional_string(official.get("updateDe")),
        content_sha256=sha256_text(canonical_json(content)),
        sanitizer_version=EASY_INFO_SANITIZER_VERSION,
        source_id=source_id,
    )


def make_dur_rule(source_id: str, operation_id: str, official: JsonObject, item_code: str | None) -> DurRuleRow | None:
    content = first_string(official, ("PROHBT_CONTENT", "REMARK"))
    if content is None:
        return None
    rule_key = canonical_json(
        {
            "sourceId": source_id,
            "operationId": operation_id,
            "official": official,
        }
    )
    return DurRuleRow(
        dur_rule_id=sha256_text(rule_key),
        dur_type=first_string(official, ("TYPE_NAME",)) or operation_id,
        subject_ingredient_code=first_string(official, ("INGR_CODE",)),
        related_ingredient_code=first_string(official, ("MIXTURE_INGR_CODE",)),
        item_code=item_code,
        dosage_form=first_string(official, ("FORM_NAME",)),
        notice_date=first_string(official, ("NOTIFICATION_DATE", "CHANGE_DATE")),
        content_text=content,
        source_id=source_id,
    )


def create_sqlite_database(path: Path, schema_path: Path, rows: DatabaseRows, artifact_version: str) -> None:
    connection = sqlite3.connect(path)
    try:
        connection.execute("PRAGMA page_size = 4096")
        connection.execute("PRAGMA journal_mode = OFF")
        connection.execute("PRAGMA synchronous = OFF")
        connection.execute("PRAGMA foreign_keys = ON")
        connection.executescript(schema_path.read_text(encoding="utf-8"))
        insert_rows(connection, rows, artifact_version)
        connection.commit()
        connection.execute("VACUUM")
    finally:
        connection.close()


def insert_rows(connection: sqlite3.Connection, rows: DatabaseRows, artifact_version: str) -> None:
    metadata = (
        ("artifact_version", artifact_version),
        ("builder_version", BUILDER_VERSION),
        ("schema_version", str(SCHEMA_VERSION)),
    )
    connection.executemany("INSERT INTO db_metadata(key, value) VALUES(?, ?)", metadata)

    connection.executemany(
        """
        INSERT INTO source_snapshot(
            source_id, snapshot_id, fetched_at_utc, source_updated_at, endpoint,
            raw_sha256, record_count, builder_version
        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
        """,
        [
            (
                item.source_id,
                item.snapshot_id,
                item.fetched_at_utc,
                item.source_updated_at,
                item.endpoint,
                item.raw_sha256,
                item.record_count,
                item.builder_version,
            )
            for item in rows.source_snapshots
        ],
    )

    connection.executemany(
        """
        INSERT INTO drug_product(
            item_code, product_name, product_name_normalized,
            manufacturer_name, manufacturer_name_normalized, strength_value,
            strength_unit, dosage_form, route, professional_class, status,
            approval_date, source_updated_at, source_id
        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        [
            (
                item.item_code,
                item.product_name,
                normalize_search_text(item.product_name),
                item.manufacturer_name,
                normalize_search_text(item.manufacturer_name) if item.manufacturer_name else None,
                # strength_value, strength_unit, dosage_form, route: deferred. The
                # structured approval-detail source is not ingested in this bootstrap,
                # and strength is never parsed from the free-text product name (that
                # would violate AGENTS.md "never guess a missing medicine field").
                # See tools/drug-data-builder/README.md "Normalized fields deferred".
                None,
                None,
                None,
                None,
                item.professional_class,
                "active_or_unknown",
                item.approval_date,
                item.source_updated_at,
                item.source_id,
            )
            for item in rows.products
        ],
    )

    connection.executemany(
        "INSERT INTO drug_alias(item_code, alias, alias_normalized, alias_type) VALUES(?, ?, ?, ?)",
        [(item.item_code, item.product_name, normalize_search_text(item.product_name), "official_name") for item in rows.products],
    )

    connection.executemany(
        "INSERT INTO ingredient(ingredient_code, ingredient_name, ingredient_name_normalized) VALUES(?, ?, ?)",
        [(item.ingredient_code, item.ingredient_name, normalize_search_text(item.ingredient_name)) for item in rows.ingredients],
    )

    connection.executemany(
        "INSERT INTO drug_ingredient(item_code, ingredient_code, amount_value, amount_unit) VALUES(?, ?, ?, ?)",
        [(item.item_code, item.ingredient_code, item.amount_value, item.amount_unit) for item in rows.drug_ingredients],
    )

    connection.executemany(
        """
        INSERT INTO easy_drug_info(
            item_code, efficacy_html, use_method_html, warning_html, caution_html,
            interaction_html, adverse_effect_html, storage_html, open_date,
            update_date, content_sha256, sanitizer_version, source_id
        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        [
            (
                item.item_code,
                item.efficacy_html,
                item.use_method_html,
                item.warning_html,
                item.caution_html,
                item.interaction_html,
                item.adverse_effect_html,
                item.storage_html,
                item.open_date,
                item.update_date,
                item.content_sha256,
                item.sanitizer_version,
                item.source_id,
            )
            for item in rows.easy_infos
        ],
    )

    connection.executemany(
        """
        INSERT INTO dur_rule(
            dur_rule_id, dur_type, subject_ingredient_code, related_ingredient_code,
            item_code, dosage_form, notice_date, content_text, source_id
        ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        [
            (
                item.dur_rule_id,
                item.dur_type,
                item.subject_ingredient_code,
                item.related_ingredient_code,
                item.item_code,
                item.dosage_form,
                item.notice_date,
                item.content_text,
                item.source_id,
            )
            for item in rows.dur_rules
        ],
    )

    connection.executemany(
        "INSERT INTO source_link(entity_type, entity_id, label, url, source_id) VALUES(?, ?, ?, ?, ?)",
        [
            ("drug_product", item.item_code, f"source:{snapshot.snapshot_id}", snapshot.endpoint, item.source_id)
            for item in rows.products
            for snapshot in rows.source_snapshots
            if snapshot.source_id == item.source_id
        ],
    )

    for product in rows.products:
        ingredient_names = [
            ingredient.ingredient_name
            for relation in rows.drug_ingredients
            for ingredient in rows.ingredients
            if relation.item_code == product.item_code and relation.ingredient_code == ingredient.ingredient_code
        ]
        connection.execute(
            """
            INSERT INTO drug_search_fts(item_code, product_name, aliases, ingredients, manufacturer)
            VALUES(?, ?, ?, ?, ?)
            """,
            (
                product.item_code,
                product.product_name,
                product.product_name,
                " ".join(sorted(ingredient_names)),
                product.manufacturer_name,
            ),
        )


def quick_check_sqlite(path: Path) -> None:
    connection = sqlite3.connect(path)
    try:
        quick_check = connection.execute("PRAGMA quick_check").fetchone()
        if quick_check is None or quick_check[0] != "ok":
            raise PublicDbBuildError(f"SQLite quick_check failed: {quick_check}")
        foreign_keys = connection.execute("PRAGMA foreign_key_check").fetchall()
        if foreign_keys:
            raise PublicDbBuildError(f"SQLite foreign_key_check failed: {foreign_keys}")
    finally:
        connection.close()


def build_manifest(
    *,
    rows: DatabaseRows,
    artifact_version: str,
    created_at_utc: str,
    min_app_version: str,
    max_app_version: str | None,
    signing_key_id: str,
    compressed_path: Path,
    sqlite_path: Path,
    artifact_relative_path: str,
) -> JsonObject:
    compressed_bytes = compressed_path.read_bytes()
    uncompressed_bytes = sqlite_path.read_bytes()
    manifest = {
        "manifestVersion": 1,
        "artifactVersion": artifact_version,
        "schemaVersion": SCHEMA_VERSION,
        "createdAtUtc": created_at_utc,
        "minAppVersion": min_app_version,
        "maxAppVersion": max_app_version,
        "freshness": "CURRENT",
        "artifact": {
            "path": artifact_relative_path,
            "compressedSha256": sha256_bytes(compressed_bytes),
            "uncompressedSha256": sha256_bytes(uncompressed_bytes),
            "compressedBytes": len(compressed_bytes),
            "uncompressedBytes": len(uncompressed_bytes),
        },
        "sourceSnapshots": [
            {
                "sourceId": item.source_id,
                "snapshotId": item.snapshot_id,
                "fetchedAtUtc": item.fetched_at_utc,
                "endpoint": item.endpoint,
                "sourceUpdatedAt": item.source_updated_at,
                "rawSha256": item.raw_sha256,
                "recordCount": item.record_count,
                "builderVersion": item.builder_version,
            }
            for item in rows.source_snapshots
        ],
        "revokedVersions": [],
        "emergencyMessageId": None,
        "signingKeyId": signing_key_id,
    }
    return cast(JsonObject, manifest)


def assert_manifest_matches_contract(manifest: JsonObject, manifest_schema_path: Path) -> None:
    contract = load_manifest_contract(manifest_schema_path)
    for field in contract.required_fields:
        if field not in manifest:
            raise PublicDbBuildError(f"Manifest missing required field: {field}")
    artifact = require_child_object(manifest, "artifact")
    path = require_string(artifact, "path")
    if re.fullmatch(contract.artifact_path_pattern, path) is None:
        raise PublicDbBuildError(f"Manifest artifact path is invalid: {path}")
    snapshots = manifest.get("sourceSnapshots")
    if not isinstance(snapshots, list) or not snapshots:
        raise PublicDbBuildError("Manifest sourceSnapshots must be a non-empty array")
    for snapshot in snapshots:
        snapshot_object = require_object(snapshot, "sourceSnapshot")
        for field in contract.source_snapshot_required_fields:
            if field not in snapshot_object:
                raise PublicDbBuildError(f"Source snapshot missing required field: {field}")


def gzip_deterministic(value: bytes) -> bytes:
    output = io.BytesIO()
    with gzip.GzipFile(filename="", mode="wb", fileobj=output, mtime=0) as gzip_file:
        gzip_file.write(value)
    return output.getvalue()


# Mirrors the artifact.path pattern in schemas/dataset_manifest.schema.json. The
# charset bans backslashes and colons, so Windows separators and drive specifiers
# (e.g. "C:\\x", "D:foo") cannot appear, and the lookaheads reject leading slashes
# and any ".." segment. Re-applied here as a read-side defense even though the
# manifest is signed, so a path is validated before it is ever joined to the root.
SAFE_ARTIFACT_PATH = re.compile(r"^(?!/)(?!.*(?:^|/)\.\.(?:/|$))[A-Za-z0-9._/-]+$")


def safe_artifact_path(root: Path, relative_path: str) -> Path:
    if SAFE_ARTIFACT_PATH.fullmatch(relative_path) is None:
        raise PublicDbVerifyError("Manifest artifact path is not relative-safe")
    resolved_root = root.resolve()
    candidate = (resolved_root / relative_path).resolve()
    try:
        candidate.relative_to(resolved_root)
    except ValueError as exc:
        raise PublicDbVerifyError("Manifest artifact path escapes artifact root") from exc
    return candidate


def write_json(path: Path, value: JsonObject) -> None:
    path.write_bytes(canonical_json_bytes(value))


def canonical_json_bytes(value: JsonObject) -> bytes:
    return (canonical_json(value) + "\n").encode("utf-8")


def canonical_json(value: JsonObject) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_text(value: str) -> str:
    return sha256_bytes(value.encode("utf-8"))


def normalize_search_text(value: str) -> str:
    return "".join(value.casefold().split())


def first_string(parent: JsonObject, keys: tuple[str, ...]) -> str | None:
    for key in keys:
        value = parent.get(key)
        text = optional_string(value)
        if text is not None:
            return text
    return None


def optional_string(value: JsonValue) -> str | None:
    if isinstance(value, str):
        stripped = value.strip()
        return stripped if stripped else None
    return None


def require_object(value: object, label: str) -> JsonObject:
    if not isinstance(value, dict):
        raise PublicDbBuildError(f"{label} must be an object")
    return cast(JsonObject, value)


def require_child_object(parent: JsonObject, key: str) -> JsonObject:
    return require_object(parent.get(key), key)


def require_string(parent: JsonObject, key: str) -> str:
    value = parent.get(key)
    if not isinstance(value, str):
        raise PublicDbBuildError(f"{key} must be a string")
    return value


def require_int(parent: JsonObject, key: str) -> int:
    value = parent.get(key)
    if isinstance(value, bool) or not isinstance(value, int):
        raise PublicDbBuildError(f"{key} must be an integer")
    return value


def require_string_list(parent: JsonObject, key: str) -> list[str]:
    value = parent.get(key)
    if not isinstance(value, list):
        raise PublicDbBuildError(f"{key} must be a string array")
    result: list[str] = []
    for item in value:
        if not isinstance(item, str):
            raise PublicDbBuildError(f"{key} must be a string array")
        result.append(item)
    return result


def require_int_list(parent: JsonObject, key: str) -> list[int]:
    value = parent.get(key)
    if not isinstance(value, list):
        raise PublicDbBuildError(f"{key} must be an integer array")
    result: list[int] = []
    for item in value:
        if isinstance(item, bool) or not isinstance(item, int):
            raise PublicDbBuildError(f"{key} must be an integer array")
        result.append(item)
    return result


def copy_public_dataset_to_static_dir(result: PublicDbBuildResult, static_dir: Path) -> None:
    static_dir.mkdir(parents=True, exist_ok=True)
    (static_dir / "artifacts").mkdir(parents=True, exist_ok=True)
    shutil.copy2(result.compressed_path, static_dir / "artifacts" / result.compressed_path.name)
    shutil.copy2(result.manifest_path, static_dir / "manifest.json")
    shutil.copy2(result.signature_path, static_dir / "manifest.sig")
