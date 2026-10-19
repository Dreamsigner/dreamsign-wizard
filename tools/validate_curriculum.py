"""Validate authored source-session coverage, then optionally generate Kotlin data.

python tools/validate_curriculum.py [--generate]
No network requests and no runtime content generation in the application.
"""
from collections import Counter
from pathlib import Path
from urllib.parse import urlparse
import argparse
import json
import re

ROOT = Path(__file__).resolve().parents[1]
GENERATED = ROOT / "composeApp/src/commonMain/kotlin/com/duolingo/app/data/generated"
CATEGORIES = ("MATHEMATICS", "COMPUTER_SCIENCE", "LANGUAGES")


def nonblank(value):
    return isinstance(value, str) and bool(value.strip())


def https(value):
    return isinstance(value, str) and urlparse(value).scheme == "https" and bool(urlparse(value).netloc)


def validate_course(course, manifest):
    errors = []
    course_id = course.get("courseId", "?")

    def fail(message):
        errors.append(f"{course_id}: {message}")

    if not isinstance(course_id, str) or not re.fullmatch(r"[a-z][a-z0-9_]*", course_id):
        fail("invalid course ID")
    for field in ("title", "sourceTitle", "edition"):
        if not nonblank(course.get(field)):
            fail(f"missing {field}")
    if course.get("category") not in CATEGORIES:
        fail("unsupported category")
    if not https(course.get("sourceUrl")) or course.get("sourceUrl") != manifest.get("sourceUrl"):
        fail("course source does not match the selected edition")
    sessions = manifest.get("sessions", [])
    if not sessions or manifest.get("sessionCount") != len(sessions):
        fail("source manifest count is invalid")
    if course.get("expectedLessonCount", len(sessions)) != len(sessions):
        fail("declared count does not match source")
    lessons = course.get("lessons", [])
    if not isinstance(lessons, list):
        fail("lessons must be a list")
        return errors
    numbers = [s.get("number") for s in sessions]
    if [lesson.get("sourceSessionNumber") for lesson in lessons] != numbers:
        fail("lesson sequence/count differs from source sequence")
    if len(set(numbers)) != len(numbers):
        fail("duplicate source session number")
    sources = {session["number"]: session for session in sessions}
    lesson_ids, exercise_ids, notes_seen = set(), set(), set()
    for lesson in lessons:
        lesson_id = lesson.get("id", "?")
        if not nonblank(lesson_id) or lesson_id in lesson_ids:
            fail(f"duplicate or empty lesson ID {lesson_id}")
        lesson_ids.add(lesson_id)
        if type(lesson.get("sourceSessionNumber")) is not int:
            fail(f"{lesson_id}: invalid source session number")
        reward = lesson.get("xpReward", 20)
        if type(reward) is not int or not 0 <= reward <= 1000:
            fail(f"{lesson_id}: invalid XP reward")
        source = sources.get(lesson.get("sourceSessionNumber"))
        if source is None:
            fail(f"{lesson_id}: unknown source session")
            continue
        if lesson.get("sourceTitle") != source.get("sourceTitle") or lesson.get("kind") != source.get("kind"):
            fail(f"{lesson_id}: source title/kind mismatch")
        urls = lesson.get("sourceUrls", [])
        if not isinstance(urls, list) or source.get("sourceUrl") not in urls or not all(https(url) for url in urls):
            fail(f"{lesson_id}: precise session source is missing or invalid")
        for field in ("title", "objective", "learningNotes"):
            if not nonblank(lesson.get(field)):
                fail(f"{lesson_id}: missing {field}")
        notes = lesson.get("learningNotes", "")
        if not isinstance(notes, str) or len(notes.split()) < 40:
            fail(f"{lesson_id}: teaching notes are not substantive")
        elif notes.strip() in notes_seen:
            fail(f"{lesson_id}: duplicate teaching notes")
        else:
            notes_seen.add(notes.strip())
        if isinstance(notes, str) and re.search(r"lorem ipsum|à compléter|not_authored|\bTODO\b", notes, re.I):
            fail(f"{lesson_id}: placeholder content")
        exercises = lesson.get("exercises", [])
        if not isinstance(exercises, list) or not exercises:
            fail(f"{lesson_id}: no playable exercises")
            continue
        for exercise in exercises:
            eid = exercise.get("id", "?")
            if not nonblank(eid) or eid in exercise_ids:
                fail(f"{lesson_id}: duplicate or empty exercise ID {eid}")
            exercise_ids.add(eid)
            kind = exercise.get("type")
            if kind not in ("math", "choice", "code"):
                fail(f"{eid}: unsupported exercise type {kind}")
            for field in ("prompt", "explanation", "hint"):
                if not nonblank(exercise.get(field)):
                    fail(f"{eid}: missing {field}")
            options = exercise.get("options", [])
            if not isinstance(options, list) or len(options) < 2 or not all(nonblank(x) for x in options):
                fail(f"{eid}: answer options are missing")
            elif len({x.strip().casefold() for x in options}) != len(options):
                fail(f"{eid}: duplicate answer options")
            answer = exercise.get("correctOptionIndex")
            if type(answer) is not int or not isinstance(options, list) or not 0 <= answer < len(options):
                fail(f"{eid}: invalid correct answer index")
            if kind == "code" and not nonblank(exercise.get("codeSnippet")):
                fail(f"{eid}: missing code snippet")
    return errors


def kotlin_string(value):
    return json.dumps(value, ensure_ascii=False).replace("$", "\\$")


def exercise_kotlin(exercise):
    kind = {"math": "MathChallenge", "choice": "MultipleChoice", "code": "CodeChallenge"}[exercise["type"]]
    fields = [f"id = {kotlin_string(exercise['id'])}", f"prompt = {kotlin_string(exercise['prompt'])}"]
    if exercise["type"] == "math" and exercise.get("formula"):
        fields.append(f"formula = {kotlin_string(exercise['formula'])}")
    if exercise["type"] == "code":
        fields.append(f"codeSnippet = {kotlin_string(exercise['codeSnippet'])}")
    fields += ["options = listOf(" + ", ".join(kotlin_string(x) for x in exercise["options"]) + ")",
               f"correctOptionIndex = {exercise['correctOptionIndex']}",
               f"explanation = {kotlin_string(exercise['explanation'])}", f"hint = {kotlin_string(exercise['hint'])}"]
    return f"Exercise.{kind}(" + ", ".join(fields) + ")"


def generate(courses):
    GENERATED.mkdir(parents=True, exist_ok=True)
    category_units = {category: [] for category in CATEGORIES}
    for course, manifest in courses:
        obj = "Course_" + course["courseId"]
        category = course["category"]
        category_units[category].append(obj)
        number = len(category_units[category])
        methods = []
        calls = []
        for index, lesson in enumerate(course["lessons"]):
            method = f"lesson{index + 1:03d}"
            calls.append(method + "()")
            params = [f"id = {kotlin_string(lesson['id'])}", f"title = {kotlin_string(lesson['title'])}",
                      f"xpReward = {lesson.get('xpReward', 20)}", "isLocked = false",
                      "exercises = listOf(\n            " + ",\n            ".join(exercise_kotlin(x) for x in lesson["exercises"]) + "\n        )",
                      f"objective = {kotlin_string(lesson['objective'])}", f"learningNotes = {kotlin_string(lesson['learningNotes'])}",
                      "sourceUrls = listOf(" + ", ".join(kotlin_string(url) for url in lesson["sourceUrls"]) + ")",
                      f"requiresPreviousLesson = {'false' if index == 0 else 'true'}",
                      f"sourceSessionNumber = {lesson['sourceSessionNumber']}", f"sourceTitle = {kotlin_string(lesson['sourceTitle'])}",
                      f"sessionKind = {kotlin_string(lesson['kind'])}"]
            methods.append(f"    private fun {method}() = Lesson(\n        " + ",\n        ".join(params) + "\n    )")
        header = "// Generated by tools/validate_curriculum.py --generate. Edit curriculum/courses/*.json.\npackage com.duolingo.app.data.generated\n\nimport com.duolingo.app.model.*\n\n"
        unit = f"internal object {obj} {{\n    fun unit() = LearningUnit(\n"
        unit += f"        id = {kotlin_string(course['courseId'])}, unitNumber = {number},\n        title = {kotlin_string(course['title'])},\n"
        unit += f"        description = {kotlin_string(str(len(calls)) + ' séances du programme source, avec révisions et évaluations identifiées.')},\n"
        unit += "        lessons = listOf(" + ", ".join(calls) + "),\n"
        unit += f"        sourceUrl = {kotlin_string(course['sourceUrl'])}, courseCode = {kotlin_string(manifest.get('courseCode', course['courseId']))},\n"
        unit += f"        edition = {kotlin_string(course['edition'])}, expectedLessonCount = {len(calls)}\n    )\n\n"
        (GENERATED / f"{obj}.kt").write_text(header + unit + "\n\n".join(methods) + "\n}\n", encoding="utf-8")
    registry = "// Generated course registry.\npackage com.duolingo.app.data.generated\n\nimport com.duolingo.app.model.*\n\nobject GeneratedCourses {\n    fun unitsFor(category: SubjectCategory): List<LearningUnit> = when (category) {\n"
    for category, objects in category_units.items():
        registry += f"        SubjectCategory.{category} -> listOf(" + ", ".join(obj + ".unit()" for obj in objects) + ")\n"
    (GENERATED / "GeneratedCourses.kt").write_text(registry + "    }\n}\n", encoding="utf-8")


def load_authored():
    courses = []
    errors = []
    all_ids = set()
    for path in sorted((ROOT / "curriculum/courses").glob("*.json")):
        course = json.loads(path.read_text(encoding="utf-8"))
        manifest_path = (ROOT / course["manifest"]).resolve()
        if not manifest_path.is_relative_to(ROOT):
            errors.append(f"{path.name}: manifest is outside repository")
            continue
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        errors.extend(validate_course(course, manifest))
        ids = [course.get("courseId")] + [lesson.get("id") for lesson in course.get("lessons", [])]
        ids += [exercise.get("id") for lesson in course.get("lessons", []) for exercise in lesson.get("exercises", [])]
        for value in ids:
            if value in all_ids:
                errors.append(f"{path.name}: duplicate global ID {value}")
            all_ids.add(value)
        courses.append((course, manifest))
    return courses, errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--generate", action="store_true")
    args = parser.parse_args()
    courses, errors = load_authored()
    if not courses:
        errors.append("No authored courses available; source manifests alone are not playable content.")
    if errors:
        for error in errors:
            print(error)
        return 1
    if args.generate:
        generate(courses)
    print(json.dumps({"validatedCourses": len(courses), "lessons": sum(len(c["lessons"]) for c, _ in courses),
                      "exercises": sum(len(lesson["exercises"]) for c, _ in courses for lesson in c["lessons"]),
                      "globalOssuCoverageComplete": False}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
