"""Discover official course links and generate a small Kotlin resource index.

Run with --generate to refresh only docs/curriculum/course-resources.json and
CourseResources.kt. Source files are read but never downloaded to disk.
"""
from __future__ import annotations

import argparse
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
COURSE_DIR = ROOT / "curriculum/courses"
CATALOG = ROOT / "curriculum/course-resources.json"
KOTLIN = ROOT / "composeApp/src/commonMain/kotlin/com/duolingo/app/data/CourseResources.kt"
MIT_LICENSE = "https://ocw.mit.edu/pages/privacy-and-terms-of-use/"
FORALLX_LICENSE = "https://creativecommons.org/licenses/by/4.0/"
MISSING_LICENSE = "https://missing.csail.mit.edu/license/"


class VideoParser(HTMLParser):
    def __init__(self, page_url: str):
        super().__init__(convert_charrefs=True)
        self.page_url = page_url
        self.urls: set[str] = set()
        self.video_page_links: set[str] = set()
        self._anchor_url: str | None = None
        self._anchor_text: list[str] = []
        self._list_item_text: list[str] = []
        self._inside_list_item = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "li":
            self._inside_list_item = True
            self._list_item_text = []
        if tag == "a":
            href = attrs.get("href")
            self._anchor_url = urljoin(self.page_url, href) if href else None
            self._anchor_text = []
        if tag not in ("iframe", "video", "source", "a"):
            return
        for key, value in attrs.items():
            if key not in ("src", "href", "data-downloadlink") or not value:
                continue
            full = urljoin(self.page_url, value)
            if key == "data-downloadlink" and urlparse(full).scheme == "http":
                parts = urlparse(full)
                if (parts.hostname or "").lower() in {"archive.org", "www.archive.org"}:
                    full = parts._replace(scheme="https").geturl()
            direct = direct_video_url(full)
            if direct:
                self.urls.add(direct)

    def handle_data(self, data):
        if self._inside_list_item:
            self._list_item_text.append(data)
        if self._anchor_url:
            self._anchor_text.append(data)

    def handle_endtag(self, tag):
        if tag == "a" and self._anchor_url:
            linked = urlparse(self._anchor_url)
            source = urlparse(self.page_url)
            text = " ".join(" ".join(self._list_item_text + self._anchor_text).lower().split())
            course_prefix = re.match(r"(/courses/[^/]+/)", source.path)
            same_course = bool(course_prefix and linked.path.startswith(course_prefix.group(1)))
            same_section = source.path.rstrip("/").rsplit("/", 1)[0] == linked.path.rstrip("/").rsplit("/", 1)[0]
            explicit_lecture_video = bool(re.search(r"\bwatch the video lecture\b", text))
            linked_topic_videos = bool(re.search(r"\btopic videos?\b", text)) and same_section
            if linked.hostname == "ocw.mit.edu" and same_course and (explicit_lecture_video or linked_topic_videos):
                self.video_page_links.add(self._anchor_url)
            self._anchor_url = None
            self._anchor_text = []
        if tag == "li":
            self._inside_list_item = False
            self._list_item_text = []


def direct_video_url(url: str) -> str | None:
    """Return a playable, specific video URL; discard channel and search links."""
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    path = [part for part in parsed.path.split("/") if part]
    if host in {"youtube.com", "youtube-nocookie.com"} or host.endswith((".youtube.com", ".youtube-nocookie.com")):
        video_id = None
        if len(path) == 2 and path[0] == "embed":
            video_id = path[1]
        elif len(path) == 2 and path[0] == "shorts":
            video_id = path[1]
        elif path == ["watch"]:
            from urllib.parse import parse_qs
            video_id = parse_qs(parsed.query).get("v", [None])[0]
        if video_id and re.fullmatch(r"[A-Za-z0-9_-]{6,20}", video_id):
            return f"https://www.youtube.com/watch?v={video_id}"
        return None
    if host == "youtu.be" and len(path) == 1 and re.fullmatch(r"[A-Za-z0-9_-]{6,20}", path[0]):
        return f"https://www.youtube.com/watch?v={path[0]}"
    if parsed.scheme == "https" and parsed.path.lower().endswith(".mp4"):
        return url
    return None


def language_for(course_id: str) -> str:
    return "fr" if course_id.startswith("tcf_canada_") else "en"


def license_for(course_id: str, url: str) -> tuple[str, str | None, str | None]:
    if course_id == "forallx_calgary_2025":
        return "CC BY 4.0 for the textbook; link only, no source file copied.", FORALLX_LICENSE, None
    if course_id == "learn_prolog_now_2026":
        return "No redistribution license verified; official external link only.", None, None
    if course_id == "mit_missing_semester_2026":
        return "CC BY-NC-SA 4.0; link only, no source file copied.", MISSING_LICENSE, None
    if course_id.startswith("mit_"):
        if course_id == "mit_6_042j_2010" and urlparse(url).path.lower().endswith(".pdf"):
            return "Copyright © 2010; all rights reserved; OCW states these notes are used with permission. Link only.", None, "https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/pages/readings/"
        return "MIT OCW site-level CC BY-NC-SA 4.0; individual assets can have exceptions. Link only.", MIT_LICENSE, None
    if course_id == "roughgarden_algorithmic_game_theory_2013":
        return "Lecture notes state personal use only; source link only, no copy or redistribution.", None, "https://timroughgarden.org/f13/l/l1.pdf"
    if course_id.startswith("tcf_canada_"):
        return "No redistribution license verified; France Éducation international link only.", None, None
    return "No redistribution license verified; source link only.", None, None


def make_resource(course_id, session_no, title, url, source_page, lang, coverage="session"):
    parsed = urlparse(url)
    path = parsed.path.lower()
    host = (parsed.hostname or "").lower()
    if path.endswith(".pdf"):
        kind = "pdf"
        label = "PDF lié"
    elif direct_video_url(url):
        kind = "video"
        label = "Vidéo"
    elif path.endswith((".py", ".java", ".zip", ".ipynb")):
        kind = "download"
        label = "Fichier de cours"
    else:
        kind = "course_section"
        label = "Section officielle"
    lic, lic_url, extra = license_for(course_id, url)
    item = {
        "id": f"{course_id}_s{session_no:03}_r{hashlib.sha256(url.encode('utf-8')).hexdigest()[:12]}",
        "title": f"{title} — {label}",
        "url": url,
        "kind": kind,
        "coverage": coverage,
        "language": lang,
        "sourceUrl": source_page,
        "licenseStatus": lic,
    }
    if lic_url:
        item["licenseEvidenceUrl"] = lic_url
    if extra:
        item["licenseNotesUrl"] = extra
    return item


def course_level_resources(course):
    cid, home = course["courseId"], course["sourceUrl"]
    lang = language_for(cid)
    lic, lic_url, extra = license_for(cid, home)
    result = [{
        "id": f"{cid}_course_site", "title": course["sourceTitle"] + " — index officiel",
        "url": home, "kind": "course_site", "coverage": "course_index",
        "language": lang, "sourceUrl": home, "licenseStatus": lic,
        **({"licenseEvidenceUrl": lic_url} if lic_url else {}),
        **({"licenseNotesUrl": extra} if extra else {}),
    }]
    if cid.startswith("mit_") and cid != "mit_missing_semester_2026":
        result.append({
            "id": f"{cid}_resource_index", "title": course["sourceTitle"] + " — collection des ressources OCW",
            "url": home.rstrip("/") + "/download/", "kind": "resource_collection",
            "coverage": "course_materials_index", "language": lang, "sourceUrl": home,
            "licenseStatus": "Index MIT OCW officiel ; contenus externes liés, non copiés.",
            "licenseEvidenceUrl": MIT_LICENSE,
        })
    if cid == "forallx_calgary_2025":
        result.append({
            "id": f"{cid}_textbook_pdf", "title": "forall x: Calgary — manuel complet, édition Fall 2025",
            "url": "https://forallx.openlogicproject.org/forallxyyc.pdf", "kind": "textbook_pdf",
            "coverage": "complete_textbook", "language": "en", "sourceUrl": home,
            "licenseStatus": "CC BY 4.0; link only, no copy stored.",
            "licenseEvidenceUrl": "https://forallx.openlogicproject.org/html/Chx1.html",
            "notes": "PDF direct consulté : 437 pages, Fall 2025. La version web peut évoluer.",
        })
    if cid == "learn_prolog_now_2026":
        result.append({
            "id": f"{cid}_textbook_html", "title": "Learn Prolog Now! — manuel en ligne",
            "url": home, "kind": "textbook_html", "coverage": "complete_html_textbook",
            "language": "en", "sourceUrl": home,
            "licenseStatus": "No redistribution license verified; official external link only.",
            "notes": "Le cours publié suit les 12 chapitres du livre en ligne ; aucun PDF complet autorisé n’a été vérifié.",
        })
    if cid == "mit_6_042j_2010":
        pdf = home.rstrip("/") + "/93cad640cf3ed0b23ef70688f452d4d5_MIT6_042JF10_notes.pdf"
        result.append({
            "id": f"{cid}_complete_notes_pdf", "title": "Mathematics for Computer Science — notes de cours PDF",
            "url": pdf, "kind": "course_notes_pdf", "coverage": "partial_textbook",
            "language": "en", "sourceUrl": home,
            "licenseStatus": "Copyright © 2010, all rights reserved; MIT OCW says used with permission. Link only.",
            "licenseNotesUrl": "https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/pages/readings/",
            "notes": "557 pages ; le chapitre 8 n’est pas disponible dans cette édition OCW. Ne pas présenter comme un manuel intégral librement redistribuable.",
        })
    return result


def fetch_video_page(page: str):
    try:
        req = Request(page, headers={"User-Agent": "course-resource-index/1.0"})
        with urlopen(req, timeout=12) as response:
            if "html" not in response.headers.get("Content-Type", "").lower():
                return None
            body = response.read(2_000_000).decode("utf-8", "ignore")
        return body
    except Exception:
        return None


def linked_videos(page: str):
    body = fetch_video_page(page)
    if body is None:
        return page, {}
    parser = VideoParser(page)
    parser.feed(body)
    videos = {url: page for url in parser.urls}
    # Follow only an explicit, same-course link labelled as a lecture-video page.
    # One hop is intentional: course navigation may link to every other course unit.
    for video_page in sorted(parser.video_page_links):
        linked_body = fetch_video_page(video_page)
        if linked_body is None:
            continue
        linked_parser = VideoParser(video_page)
        linked_parser.feed(linked_body)
        videos.update({url: video_page for url in linked_parser.urls})
    return page, videos


def discover(include_videos: bool):
    courses = []
    sessions = []
    source_files = sorted(COURSE_DIR.glob("*.json"))
    source_courses = [(file, json.loads(file.read_text(encoding="utf-8"))) for file in source_files]
    video_cache = {}
    if include_videos:
        pages = set()
        for _, course in source_courses:
            for lesson in course.get("lessons", []):
                pages.update(u for u in lesson.get("sourceUrls", [])
                             if not urlparse(u).path.lower().endswith(".pdf") and
                             not urlparse(u).path.lower().endswith((".py", ".java", ".zip", ".ipynb")))
        with ThreadPoolExecutor(max_workers=16) as pool:
            video_cache = dict(pool.map(linked_videos, sorted(pages)))
    for file, course in source_courses:
        cid, lang = course["courseId"], language_for(course["courseId"])
        courses.append({
            "courseId": cid, "title": course["title"], "sourceTitle": course["sourceTitle"],
            "edition": course["edition"], "sourceUrl": course["sourceUrl"],
            "resources": course_level_resources(course),
        })
        for lesson in course.get("lessons", []):
            n = lesson["sourceSessionNumber"]
            urls = list(dict.fromkeys(lesson.get("sourceUrls", [])))
            resources = []
            seen = set()
            for u in urls:
                if u in seen:
                    continue
                seen.add(u)
                resources.append(make_resource(cid, n, lesson["sourceTitle"], u, u, lang))
                for video in sorted(video_cache.get(u, ())):
                    if video not in seen:
                        seen.add(video)
                        resources.append(make_resource(cid, n, lesson["sourceTitle"], video, u, lang))
            sessions.append({
                "courseId": cid, "sessionNumber": n, "sourceTitle": lesson["sourceTitle"],
                "exerciseIds": [e["id"] for e in lesson.get("exercises", [])],
                "resources": resources,
            })
    return {"schemaVersion": 1, "checkedOn": "2026-10-20", "courses": courses, "sessions": sessions}


def refresh_videos_for_courses(data, course_ids):
    """Refresh video resources only, preserving all other catalog entries verbatim."""
    source_courses = {
        json.loads(path.read_text(encoding="utf-8"))["courseId"]: json.loads(path.read_text(encoding="utf-8"))
        for path in COURSE_DIR.glob("*.json")
        if json.loads(path.read_text(encoding="utf-8"))["courseId"] in set(course_ids)
    }
    unknown = set(course_ids) - set(source_courses)
    if unknown:
        raise ValueError(f"unknown course IDs: {sorted(unknown)}")
    page_sets = {}
    pages = set()
    for course_id, course in source_courses.items():
        for lesson in course.get("lessons", []):
            urls = [u for u in lesson.get("sourceUrls", [])
                    if not urlparse(u).path.lower().endswith((".pdf", ".py", ".java", ".zip", ".ipynb"))]
            page_sets[(course_id, lesson["sourceSessionNumber"])] = (lesson, urls)
            pages.update(urls)
    with ThreadPoolExecutor(max_workers=16) as pool:
        video_cache = dict(pool.map(linked_videos, sorted(pages)))
    for session in data["sessions"]:
        course_id = session["courseId"]
        if course_id not in source_courses:
            continue
        lesson, pages = page_sets[(course_id, session["sessionNumber"])]
        direct_videos = {}
        for page in pages:
            direct_videos.update(video_cache.get(page, {}))
        resources = [r for r in session["resources"] if r.get("kind") != "video"]
        for video_url, video_page in sorted(direct_videos.items()):
            resources.append(make_resource(
                course_id, session["sessionNumber"], lesson["sourceTitle"],
                video_url, video_page, language_for(course_id),
            ))
        session["resources"] = resources
    return data


def validate(data):
    errors = []
    course_ids = [c.get("courseId") for c in data.get("courses", [])]
    if len(course_ids) != len(set(course_ids)):
        errors.append("duplicate courseId")
    published = {json.loads(p.read_text(encoding="utf-8"))["courseId"] for p in COURSE_DIR.glob("*.json")}
    if set(course_ids) != published:
        errors.append(f"catalog courses differ from published curriculum files: missing={sorted(published-set(course_ids))}, extra={sorted(set(course_ids)-published)}")
    session_keys, resource_ids, exercise_ids = set(), set(), set()
    course_resource_ids = set()
    for course in data.get("courses", []):
        for resource in course.get("resources", []):
            resource_ids.add(resource.get("id"))
            course_resource_ids.add(resource.get("id"))
            if not resource.get("url", "").startswith("https://"):
                errors.append(f"{resource.get('id')}: non-HTTPS URL")
            if not all(resource.get(k) for k in ("title", "kind", "coverage", "language", "sourceUrl", "licenseStatus")):
                errors.append(f"{resource.get('id')}: missing metadata")
    for session in data.get("sessions", []):
        key = (session.get("courseId"), session.get("sessionNumber"))
        if key in session_keys:
            errors.append(f"duplicate session mapping: {key}")
        session_keys.add(key)
        cid, num = key
        source = json.loads((COURSE_DIR / f"{cid}.json").read_text(encoding="utf-8"))
        lessons = [l for l in source.get("lessons", []) if l.get("sourceSessionNumber") == num]
        if len(lessons) != 1 or lessons[0].get("sourceTitle") != session.get("sourceTitle"):
            errors.append(f"{cid} session {num}: source lesson/title mismatch")
            continue
        expected_exercises = [e["id"] for e in lessons[0].get("exercises", [])]
        if expected_exercises != session.get("exerciseIds"):
            errors.append(f"{cid} session {num}: exercise mapping mismatch")
        for exercise_id in session.get("exerciseIds", []):
            if not exercise_id or exercise_id in exercise_ids:
                errors.append(f"duplicate or empty exercise id: {exercise_id!r}")
            exercise_ids.add(exercise_id)
        allowed_urls = set(lessons[0].get("sourceUrls", []))
        for resource in session.get("resources", []):
            rid, url = resource.get("id"), resource.get("url", "")
            if rid in resource_ids:
                errors.append(f"duplicate resource id: {rid}")
            resource_ids.add(rid)
            if not url.startswith("https://"):
                errors.append(f"{rid}: non-HTTPS URL")
            if resource.get("kind") == "video":
                if direct_video_url(url) != url:
                    errors.append(f"{rid}: video URL is not a direct playable URL")
                elif any(urlparse(url).hostname.endswith(host) for host in ("youtube.com", "youtube-nocookie.com", "youtu.be")):
                    if not re.fullmatch(r"https://www\.youtube\.com/watch\?v=[A-Za-z0-9_-]{6,20}", url):
                        errors.append(f"{rid}: YouTube video URL must identify one watch page")
            elif url not in allowed_urls:
                errors.append(f"{rid}: session resource is not linked by its source lesson")
            if not all(resource.get(k) for k in ("title", "kind", "coverage", "language", "sourceUrl", "licenseStatus")):
                errors.append(f"{rid}: missing metadata")
    if len(resource_ids) != len(course_resource_ids) + sum(len(s.get("resources", [])) for s in data.get("sessions", [])):
        errors.append("resource IDs are not unique")
    return errors


def kstr(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def kotlin_resource(resource):
    fields = [
        f"id = {kstr(resource['id'])}", f"title = {kstr(resource['title'])}",
        f"url = {kstr(resource['url'])}", f"kind = {kstr(resource['kind'])}",
        f"coverage = {kstr(resource['coverage'])}", f"language = {kstr(resource['language'])}",
        f"licenseStatus = {kstr(resource['licenseStatus'])}",
        f"sourceUrl = {kstr(resource['sourceUrl'])}",
        f"licenseEvidenceUrl = {kstr(resource.get('licenseEvidenceUrl', ''))}",
        f"notes = {kstr(resource.get('notes', ''))}",
    ]
    return "CourseResource(" + ", ".join(fields) + ")"


MAX_HELPER_SOURCE_CHARS = 5900


def chunked_helpers(name, entries, entry_lines, opening="Map<String, CourseResource>"):
    """Render small helpers so no generated JVM method approaches the 64 KB limit."""
    helpers, batch = [], []
    for entry in entries:
        candidate = batch + [entry]
        body = entry_lines(candidate)
        helper = f"    private fun {name}000(): {opening} = mapOf(\n" + "\n".join(body) + "\n    )"
        if len(helper) > MAX_HELPER_SOURCE_CHARS and batch:
            helpers.append(batch)
            batch = [entry]
        else:
            batch = candidate
        single_body = entry_lines(batch)
        single_helper = f"    private fun {name}000(): {opening} = mapOf(\n" + "\n".join(single_body) + "\n    )"
        if len(single_helper) > MAX_HELPER_SOURCE_CHARS:
            raise ValueError(f"single generated entry too large for {name}: {len(single_helper)} chars")
    if batch:
        helpers.append(batch)
    rendered = []
    for index, group in enumerate(helpers):
        rendered.append(f"    private fun {name}{index:03d}(): {opening} = mapOf(")
        rendered.extend("\n".join(entry_lines(group)).splitlines())
        rendered.append("    )")
    return helpers, rendered


def resource_mapping_lines(entries):
    return [f"        {kstr(resource['id'])} to {kotlin_resource(resource)}," for resource in entries]


def render_kotlin(data):
    by_course = {c["courseId"]: [r["id"] for r in c["resources"]] for c in data["courses"]}
    by_session = {f"{s['courseId']}:{s['sessionNumber']}": [r["id"] for r in s["resources"]] for s in data["sessions"]}
    all_resources = [r for c in data["courses"] for r in c["resources"]]
    all_resources.extend(r for s in data["sessions"] for r in s["resources"])
    resource_groups, resource_helpers = chunked_helpers("resourceBatch", all_resources, resource_mapping_lines)

    def mapping_lines(pairs):
        lines = []
        for key, ids in pairs:
            values = ", ".join(f"resourcesById.getValue({kstr(resource_id)})" for resource_id in ids)
            lines.append(f"        {kstr(key)} to listOf({values}),")
        return lines

    session_groups, session_helpers = chunked_helpers(
        "sessionBatch", list(by_session.items()), mapping_lines, "Map<String, List<CourseResource>>"
    )
    exercise_to_session = {}
    for session in data["sessions"]:
        for exercise_id in session["exerciseIds"]:
            exercise_to_session[exercise_id] = f"{session['courseId']}:{session['sessionNumber']}"

    def exercise_lines(pairs):
        return [f"        {kstr(key)} to {kstr(value)}," for key, value in pairs]

    exercise_groups, exercise_helpers = chunked_helpers(
        "exerciseBatch", list(exercise_to_session.items()), exercise_lines, "Map<String, String>"
    )
    out = [
        "// Generated from curriculum/course-resources.json by tools/validate_course_resources.py.",
        "package com.duolingo.app.data", "", "data class CourseResource(",
        "    val id: String,", "    val title: String,", "    val url: String,",
        "    val kind: String,", "    val coverage: String,", "    val language: String,",
        "    val licenseStatus: String,", "    val sourceUrl: String,",
        "    val licenseEvidenceUrl: String = \"\",", "    val notes: String = \"\",", ")", "",
        "object CourseResources {",
        "    private val resourcesById: Map<String, CourseResource> = buildMap {",
    ]
    out.extend(f"        putAll(resourceBatch{index:03d}())" for index in range(len(resource_groups)))
    out.extend(("    }", "", "    val byCourseId: Map<String, List<CourseResource>> = mapOf("))
    for course_id, ids in by_course.items():
        values = ", ".join(f"resourcesById.getValue({kstr(resource_id)})" for resource_id in ids)
        out.append(f"        {kstr(course_id)} to listOf({values}),")
    out.extend(("    )", "", "    val bySession: Map<String, List<CourseResource>> = buildMap {"))
    out.extend(f"        putAll(sessionBatch{index:03d}())" for index in range(len(session_groups)))
    out.extend(("    }", "", "    private val sessionKeyByExerciseId: Map<String, String> = buildMap {"))
    out.extend(f"        putAll(exerciseBatch{index:03d}())" for index in range(len(exercise_groups)))
    out.extend(("    }", "", "    fun forCourse(courseId: String): List<CourseResource> = byCourseId[courseId].orEmpty()", "    fun forSession(courseId: String, sessionNumber: Int): List<CourseResource> = bySession[\"$courseId:$sessionNumber\"].orEmpty()", "    fun forExercise(exerciseId: String): List<CourseResource> = sessionKeyByExerciseId[exerciseId]?.let { bySession[it] }.orEmpty()", "", "    // Resource payloads are split into small functions to keep JVM methods below 8 KB.", ""))
    out.extend(resource_helpers)
    out.extend(session_helpers)
    out.extend(exercise_helpers)
    out.append("}")
    return "\n".join(out) + "\n"


def generate_kotlin(data):
    KOTLIN.parent.mkdir(parents=True, exist_ok=True)
    KOTLIN.write_text(render_kotlin(data), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--generate", action="store_true", help="refresh JSON and generated Kotlin from published lesson links")
    parser.add_argument("--with-videos", action="store_true", help="inspect linked HTML pages for specific YouTube videos and direct MP4 URLs")
    parser.add_argument("--refresh-videos-for", nargs="+", metavar="COURSE_ID", help="refresh only video records for the named courses, preserving all other catalog data")
    args = parser.parse_args()
    if args.with_videos and not args.generate:
        parser.error("--with-videos requires --generate")
    if args.refresh_videos_for and args.generate:
        parser.error("--refresh-videos-for cannot be combined with --generate")
    if args.refresh_videos_for:
        if not CATALOG.exists():
            print("Missing curriculum/course-resources.json; run with --generate.")
            return 1
        data = json.loads(CATALOG.read_text(encoding="utf-8"))
        data = refresh_videos_for_courses(data, args.refresh_videos_for)
        errors = validate(data)
        if errors:
            print("\n".join(errors))
            return 1
        CATALOG.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        generate_kotlin(data)
        print(f"Refreshed direct videos for {', '.join(args.refresh_videos_for)}.")
        return 0
    if args.generate:
        data = discover(args.with_videos)
        errors = validate(data)
        if errors:
            print("\n".join(errors))
            return 1
        CATALOG.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        generate_kotlin(data)
        print(f"Generated {len(data['courses'])} courses and {len(data['sessions'])} unit mappings.")
        return 0
    if not CATALOG.exists():
        print("Missing curriculum/course-resources.json; run with --generate.")
        return 1
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    errors = validate(data)
    if not KOTLIN.exists() or KOTLIN.read_text(encoding="utf-8") != render_kotlin(data):
        errors.append("Generated CourseResources.kt is stale; regenerate from the resource catalogue.")
    if errors:
        print("\n".join(errors))
        return 1
    nresources = sum(len(c["resources"]) for c in data["courses"]) + sum(len(s["resources"]) for s in data["sessions"])
    nexercises = sum(len(s["exerciseIds"]) for s in data["sessions"])
    print(f"PASS: {len(data['courses'])} courses, {len(data['sessions'])} units, {nresources} linked resources, {nexercises} exercise mappings.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
