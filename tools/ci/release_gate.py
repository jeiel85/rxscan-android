from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
GATES_PATH = ROOT / "config" / "release_gates.json"


def load_gates(path: Path) -> list[dict[str, object]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    gates = data.get("gates")
    if not isinstance(gates, list):
        raise ValueError("release_gates.json must contain a 'gates' array")
    return [gate for gate in gates if isinstance(gate, dict)]


def blocking_failures(gates: list[dict[str, object]]) -> list[dict[str, object]]:
    """Mandatory gates that are not satisfied block the release."""
    failures: list[dict[str, object]] = []
    for gate in gates:
        if gate.get("mandatory", True) and gate.get("status") != "pass":
            failures.append(gate)
    return failures


def render_report(gates: list[dict[str, object]]) -> str:
    lines = []
    for gate in gates:
        mandatory = "mandatory" if gate.get("mandatory", True) else "optional"
        status = str(gate.get("status", "unknown"))
        lines.append(f"[{status.upper():7}] ({mandatory}) {gate.get('id')}: {gate.get('name')}")
        note = gate.get("note")
        if note:
            lines.append(f"            note: {note}")
    return "\n".join(lines)


def run(path: Path) -> int:
    gates = load_gates(path)
    print(render_report(gates))
    failures = blocking_failures(gates)
    if failures:
        print()
        print(f"RELEASE BLOCKED: {len(failures)} mandatory gate(s) not satisfied:", file=sys.stderr)
        for gate in failures:
            print(f"  - {gate.get('id')}: {gate.get('name')} (status={gate.get('status')})", file=sys.stderr)
        return 1
    print()
    print("All mandatory release gates satisfied.")
    return 0


def selftest() -> int:
    """Verify the gating logic: any open mandatory gate must block; all-pass releases."""
    all_pass = [
        {"id": "a", "name": "A", "mandatory": True, "status": "pass"},
        {"id": "b", "name": "B", "mandatory": False, "status": "blocked"},
    ]
    one_blocked = [
        {"id": "a", "name": "A", "mandatory": True, "status": "pass"},
        {"id": "c", "name": "C", "mandatory": True, "status": "blocked"},
    ]
    assert blocking_failures(all_pass) == [], "all-pass must not block"
    assert len(blocking_failures(one_blocked)) == 1, "open mandatory gate must block"
    # An optional open gate must never block the release.
    assert blocking_failures([{"id": "o", "name": "O", "mandatory": False, "status": "blocked"}]) == []
    print("release_gate selftest passed")
    return 0


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
    return run(GATES_PATH)


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
