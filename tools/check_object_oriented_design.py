"""Audit the Northeastern CS3500 Fall 2019 draft without generating content."""
import importlib.util
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
COURSE = ROOT / "curriculum/drafts/neu_ood_2019.json"
MANIFEST = ROOT / "docs/curriculum/neu-ood-source-sessions.json"
VALIDATOR = ROOT / "tools/validate_curriculum.py"


def main() -> int:
    spec = importlib.util.spec_from_file_location("validate_curriculum", VALIDATOR)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    course = json.loads(COURSE.read_text(encoding="utf-8"))
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    errors = module.validate_course(course, manifest)

    exercises = [e for lesson in course["lessons"] for e in lesson["exercises"]]
    ids = [e.get("id") for e in exercises]
    if len(ids) != len(set(ids)) or any(not isinstance(i, str) or not i.strip() for i in ids):
        print("FAIL: exercise IDs are empty or duplicated")
        return 1
    if any(not e.get("explanation", "").strip() or not e.get("hint", "").strip() for e in exercises):
        print("FAIL: an exercise lacks a worked explanation or hint")
        return 1

    missing = sorted({s["number"] for s in manifest["sessions"]} -
                     {l["sourceSessionNumber"] for l in course["lessons"]})
    if len(course["lessons"]) == manifest["sessionCount"] and not errors:
        print(f"PASS: {len(course['lessons'])} sessions validated; {len(exercises)} exercises")
        return 0
    print(f"PARTIAL: {len(course['lessons'])}/{manifest['sessionCount']} lessons; missing source session(s): {missing}")
    for error in errors:
        print(f"validator: {error}")
    print("The draft remains outside the catalog. Session 24 is intentionally unauthored because no Fall 2019 JavaScript teaching material was found.")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
