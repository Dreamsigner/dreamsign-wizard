"""Focused regression checks for the generated resource index."""
from __future__ import annotations

import json
import re
import unittest

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


if __name__ == "__main__":
    unittest.main()
