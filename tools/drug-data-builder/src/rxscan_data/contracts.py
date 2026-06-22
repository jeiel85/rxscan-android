from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import TypeAlias, cast


JsonValue: TypeAlias = None | bool | int | float | str | list["JsonValue"] | dict[str, "JsonValue"]
JsonObject: TypeAlias = dict[str, JsonValue]


@dataclass(frozen=True)
class ManifestContract:
    required_fields: tuple[str, ...]
    artifact_path_pattern: str
    source_snapshot_required_fields: tuple[str, ...]

    def describe(self) -> str:
        required = ", ".join(self.required_fields)
        source_required = ", ".join(self.source_snapshot_required_fields)
        return (
            "Manifest required fields: "
            f"{required}\nSource snapshot required fields: {source_required}"
        )


def load_json_object(path: Path) -> JsonObject:
    value: object = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise TypeError(f"{path} must contain a JSON object")
    return cast(JsonObject, value)


def require_object(parent: JsonObject, key: str) -> JsonObject:
    value = parent.get(key)
    if not isinstance(value, dict):
        raise TypeError(f"{key} must be an object")
    return cast(JsonObject, value)


def require_string(parent: JsonObject, key: str) -> str:
    value = parent.get(key)
    if not isinstance(value, str):
        raise TypeError(f"{key} must be a string")
    return value


def require_string_tuple(parent: JsonObject, key: str) -> tuple[str, ...]:
    value = parent.get(key)
    if not isinstance(value, list):
        raise TypeError(f"{key} must be a string array")

    items: list[str] = []
    for item in value:
        if not isinstance(item, str):
            raise TypeError(f"{key} must be a string array")
        items.append(item)
    return tuple(items)


def load_manifest_contract(schema_path: Path) -> ManifestContract:
    schema = load_json_object(schema_path)
    required = require_string_tuple(schema, "required")

    properties = require_object(schema, "properties")

    artifact = require_object(properties, "artifact")
    artifact_properties = require_object(artifact, "properties")
    path_property = require_object(artifact_properties, "path")
    path_pattern = require_string(path_property, "pattern")

    snapshots = require_object(properties, "sourceSnapshots")
    snapshot_items = require_object(snapshots, "items")
    snapshot_required = require_string_tuple(snapshot_items, "required")

    return ManifestContract(
        required_fields=required,
        artifact_path_pattern=path_pattern,
        source_snapshot_required_fields=snapshot_required,
    )
