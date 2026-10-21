"""Focused regression checks for the generated resource index."""
from __future__ import annotations

import json
import re
import unittest
from copy import deepcopy

import validate_course_resources as resources


class CourseResourcesGenerationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = json.loads(resources.CATALOG.read_text(encoding="utf-8"))

    def test_kotlin_generation_is_deterministic_and_helpers_are_small(self):
        first = resources.render_kotlin(self.catalog)
        second = resources.render_kotlin(self.catalog)
        self.assertEqual(first, second)
        starts = [match.start() for match in re.finditer(r"^    private fun ", first, re.MULTILINE)]
        self.assertGreater(len(starts), 0)
        for index, start in enumerate(starts):
            end = starts[index + 1] if index + 1 < len(starts) else len(first)
            helper = first[start:end]
            self.assertLessEqual(len(helper), resources.MAX_HELPER_SOURCE_CHARS)

    def test_pdf_kind_and_coverage_are_explicit(self):
        all_items = [r for c in self.catalog["courses"] for r in c["resources"]]
        all_items.extend(r for s in self.catalog["sessions"] for r in s["resources"])
        pdf_items = [r for r in all_items if resources.urlparse(r["url"]).path.lower().endswith(".pdf")]
        self.assertTrue(pdf_items)
        for item in pdf_items:
            self.assertIn(item["kind"], {"pdf", "course_notes_pdf", "textbook_pdf"}, item["id"])
            self.assertTrue(item.get("coverage"), item["id"])
            if item["coverage"] == "complete_textbook":
                self.assertEqual("textbook_pdf", item["kind"], item["id"])
            if item["kind"] == "textbook_pdf":
                self.assertEqual("complete_textbook", item["coverage"], item["id"])
        calculus = [r for r in all_items if r["id"].startswith("mit_18_01sc_2010")]
        self.assertFalse(any(r["coverage"] == "complete_textbook" for r in calculus))

    def test_video_links_are_specific_playable_urls_and_keep_page_provenance(self):
        cases = {
            "https://www.youtube.com/embed/Abc123_XYZ9": "https://www.youtube.com/watch?v=Abc123_XYZ9",
            "https://www.youtube-nocookie.com/embed/Abc123_XYZ9?rel=0": "https://www.youtube.com/watch?v=Abc123_XYZ9",
            "https://youtu.be/Abc123_XYZ9?t=5": "https://www.youtube.com/watch?v=Abc123_XYZ9",
            "https://www.youtube.com/watch?v=Abc123_XYZ9": "https://www.youtube.com/watch?v=Abc123_XYZ9",
            "https://archive.org/download/course/lecture.mp4": "https://archive.org/download/course/lecture.mp4",
        }
        for source, expected in cases.items():
            with self.subTest(source=source):
                self.assertEqual(expected, resources.direct_video_url(source))
                item = resources.make_resource("mit_demo", 1, "Lecture", expected, "https://ocw.mit.edu/lecture/", "en")
                self.assertEqual("video", item["kind"])
                self.assertEqual("https://ocw.mit.edu/lecture/", item["sourceUrl"])
        for non_video in (
            "https://www.youtube.com/channel/UC123456",
            "https://www.youtube.com/@MissingSemester",
            "https://www.youtube.com/mitocw",
            "https://evil-youtube.com/embed/Abc123_XYZ9",
            "https://youtube.com.evil.example/embed/Abc123_XYZ9",
        ):
            with self.subTest(non_video=non_video):
                self.assertIsNone(resources.direct_video_url(non_video))

    def test_video_parser_extracts_specific_youtube_and_mp4_but_ignores_channels(self):
        parser = resources.VideoParser("https://ocw.mit.edu/lecture/")
        parser.feed('''<iframe src="https://www.youtube.com/embed/Abc123_XYZ9"></iframe>
            <a href="https://archive.org/download/course/lecture.mp4">download video</a>
            <video data-downloadlink="http://www.archive.org/download/course/lecture2.mp4"></video>
            <a href="https://www.youtube.com/mitocw">channel</a>''')
        self.assertEqual({
            "https://www.youtube.com/watch?v=Abc123_XYZ9",
            "https://archive.org/download/course/lecture.mp4",
            "https://www.archive.org/download/course/lecture2.mp4",
        }, parser.urls)

    def test_only_explicit_video_pages_from_the_same_course_and_lesson_are_followed(self):
        parser = resources.VideoParser(
            "https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c1/c1s1/"
        )
        parser.feed('''
            <a href="/courses/6-004-computation-structures-spring-2017/pages/c1/c1s2/">1.2 Topic Videos</a>
            <a href="/courses/6-004-computation-structures-spring-2017/pages/c2/c2s2/">2.2 Topic Videos</a>
            <a href="/courses/6-004-computation-structures-spring-2017/pages/c1/c1s3/">1.3 Worksheet</a>
            <a href="/courses/18-06sc-linear-algebra-fall-2011/resources/other/">Watch the video lecture</a>
        ''')
        self.assertEqual({
            "https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c1/c1s2/"
        }, parser.video_page_links)

        linear_algebra = resources.VideoParser(
            "https://ocw.mit.edu/courses/18-06sc-linear-algebra-fall-2011/pages/unit/lesson/"
        )
        linear_algebra.feed('''<a href="/courses/18-06sc-linear-algebra-fall-2011/resources/the-geometry/">Watch the video lecture</a>''')
        self.assertEqual({
            "https://ocw.mit.edu/courses/18-06sc-linear-algebra-fall-2011/resources/the-geometry/"
        }, linear_algebra.video_page_links)

    def test_catalog_validator_rejects_channel_links_labeled_as_videos(self):
        data = deepcopy(self.catalog)
        video = next(
            resource
            for session in data["sessions"]
            for resource in session["resources"]
            if resource["kind"] == "video"
        )
        video["url"] = "https://www.youtube.com/mitocw"
        self.assertTrue(any("direct playable URL" in error for error in resources.validate(data)))


if __name__ == "__main__":
    unittest.main()
