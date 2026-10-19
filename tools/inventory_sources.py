"""Record supplied course references and factual source-session inventories.

Uses the Python standard library. Does not invent sessions or learning content.
Run from the repository root: python tools/inventory_sources.py
"""
from concurrent.futures import ThreadPoolExecutor
from datetime import date
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urljoin
from urllib.request import Request, urlopen
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "curriculum"
OSSU = "https://github.com/ossu/computer-science"
RAW = "https://raw.githubusercontent.com/ossu/computer-science/master/README.md"
CALCULUS = "https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/"


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.current = None

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self.current = [dict(attrs).get("href", ""), []]

    def handle_data(self, data):
        if self.current is not None:
            self.current[1].append(data)

    def handle_endtag(self, tag):
        if tag == "a" and self.current is not None:
            href, parts = self.current
            self.links.append((href, " ".join(" ".join(parts).split())))
            self.current = None


def fetch(url):
    request = Request(url, headers={"User-Agent": "CurriculumSourceInventory/1.0"})
    with urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", errors="replace"), response.url


def links(url):
    html, final_url = fetch(url)
    parser = Links()
    parser.feed(html)
    return [(urljoin(final_url, href), text) for href, text in parser.links], final_url


def ossu_courses():
    markdown, _ = fetch(RAW)
    section = ""
    subsection = ""
    result = []
    for line in markdown.splitlines():
        if line.startswith("## "):
            section = line[3:].strip()
            subsection = ""
        if line.startswith("### "):
            subsection = line[4:].strip()
        if section not in ("Intro CS", "Core CS", "Advanced CS", "Final project"):
            continue
        if not line.startswith("["):
            continue
        match = re.match(r"\[([^]]+)\]\(([^)]+)\)", line)
        if not match or "|" not in line:
            continue
        title, url = match.groups()
        result.append({
            "title": title, "sourceUrl": urljoin(OSSU + "/blob/master/", url),
            "category": "math" if "math" in subsection.lower() else "computer_science",
            "section": section, "subsection": subsection,
            "optional": section in ("Advanced CS", "Final project"),
            "discoveryStatus": "pending", "sessionCount": None,
            "provenance": OSSU,
        })
    return result


def discover(course):
    try:
        entries, final_url = links(course["sourceUrl"])
        found = {}
        for href, title in entries:
            if re.search(r"syllabus|calendar|schedule|lectures?|chapters?|sessions?|lessons?", title, re.I):
                found[href] = title
        return dict(course, resolvedUrl=final_url, discoveryStatus="landing_accessible",
                    candidateIndexes=[{"title": title, "url": href} for href, title in found.items()])
    except Exception as error:
        return dict(course, discoveryStatus="unavailable", accessError=str(error))


def calculus_sessions():
    entries, _ = links(CALCULUS)
    parts = list(dict.fromkeys(href for href, title in entries
                              if title.startswith("Part ") and href.startswith(CALCULUS)))
    sessions = {}
    assessments = {}
    for href, title in entries:
        if "exam" in title.lower() and href.startswith(CALCULUS):
            assessments[href] = title
    indexes = list(dict.fromkeys(parts + list(assessments)))
    with ThreadPoolExecutor(max_workers=6) as pool:
        pages = list(pool.map(links, indexes))
    for page_entries, _ in pages:
        for href, title in page_entries:
            match = re.match(r"Session\s+(\d+):\s*(.+)", title)
            if match and href.startswith(CALCULUS):
                number = int(match.group(1))
                value = {"number": number, "sourceTitle": match.group(2), "sourceUrl": href,
                         "kind": "exam" if "Materials for" in match.group(2) else "review" if "Review" in match.group(2) else "lesson",
                         "contentStatus": "not_authored"}
                if number in sessions and sessions[number]["sourceUrl"] != href:
                    raise ValueError(f"Conflicting source URLs for session {number}")
                sessions[number] = value
    # The final exam is a session itself, rather than an index linking to itself.
    final_exam = next(url for url, title in assessments.items() if title == "Final Exam")
    final_html, _ = fetch(final_exam)
    match = re.search(r'<h[12][^>]*>\s*Session\s+(\d+):\s*([^<]+)', final_html)
    if not match:
        raise ValueError("Final exam source session heading was not found")
    number = int(match.group(1))
    sessions[number] = {"number": number, "sourceTitle": match.group(2).strip(),
                        "sourceUrl": final_exam, "kind": "exam", "contentStatus": "not_authored"}
    numbers = sorted(sessions)
    if not numbers or numbers != list(range(1, max(numbers) + 1)):
        raise ValueError(f"Source session sequence is incomplete: {numbers}")
    return {"courseCode": "MIT 18.01SC", "edition": "Fall 2010", "sourceUrl": CALCULUS,
            "checkedOn": date.today().isoformat(), "sessionCount": len(sessions),
            "sessions": [sessions[n] for n in numbers],
            "assessmentIndexes": [{"sourceTitle": title, "sourceUrl": href}
                                  for href, title in assessments.items()]}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    if "--calculus-only" not in sys.argv:
        courses = ossu_courses()
        with ThreadPoolExecutor(max_workers=8) as pool:
            inventory = list(pool.map(discover, courses))
        payload = {"checkedOn": date.today().isoformat(), "sourceUrl": OSSU,
                   "courseCount": len(inventory), "courses": inventory}
        (OUT / "ossu-course-inventory.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    calculus = calculus_sessions()
    (OUT / "mit-18-01sc-source-sessions.json").write_text(json.dumps(calculus, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"calculusSessions": calculus["sessionCount"]}))


if __name__ == "__main__":
    main()
