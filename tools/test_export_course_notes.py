"""Regression tests for the published-course Markdown exporter."""
from __future__ import annotations

import json
from pathlib import Path
import unittest

import export_course_notes as exporter


class CourseNotesExportTests(unittest.TestCase):
    def test_teaching_notes_keep_headings_code_and_latex_as_markdown(self):
        source = "## Comprendre\n\n**Bash**\n\n```bash\npwd\n```\n\n$$x_1=b_1$$"
        rendered = exporter.learning_notes(source)
        self.assertIn("### Comprendre", rendered)
        self.assertIn("```bash\npwd\n```", rendered)
        self.assertIn("$$x_1=b_1$$", rendered)
        self.assertNotIn("<p>", rendered)

    def test_inline_code_and_bold_are_not_hidden_in_html_paragraphs(self):
        rendered = exporter.learning_notes("Appeler `vector<int>` et $x<1$.\n\nDu **code**, sans <script>.")
        self.assertIn("`vector<int>`", rendered)
        self.assertIn("$x<1$", rendered)
        self.assertIn("**code**", rendered)
        self.assertIn("&lt;script&gt;", rendered)
        self.assertNotIn("<p>", rendered)

    @classmethod
    def setUpClass(cls):
        cls.outputs = exporter.render_all()
        cls.course_map, _, cls.resources = exporter.load_inputs()

    def test_rendering_is_deterministic_and_covers_all_courses(self):
        self.assertEqual(self.outputs, exporter.render_all())
        self.assertEqual(16, len(self.outputs))
        for course_id, course in self.course_map.items():
            text = self.outputs[exporter.OUTPUT_DIR / f"{course_id}.md"]
            self.assertEqual(1, text.count("course-resources-notes.md"))
            self.assertNotIn("No redistribution license verified", text)
            for lesson in course["lessons"]:
                self.assertIn(f'<a id="{lesson["id"]}"></a>', text)
                self.assertIn(f"](#{lesson['id']})", text)
                for exercise in lesson["exercises"]:
                    self.assertIn(f'<a id="{exercise["id"]}"></a>', text)
                    self.assertIn(exercise["id"], text)

    def test_session_resources_are_rendered_and_pdfs_have_honest_coverage(self):
        for session in self.resources["sessions"]:
            course_text = self.outputs[exporter.OUTPUT_DIR / f"{session['courseId']}.md"]
            for resource in session["resources"]:
                self.assertIn(exporter.esc(resource["url"]), course_text)
        all_resources = [r for c in self.resources["courses"] for r in c["resources"]]
        all_resources.extend(r for s in self.resources["sessions"] for r in s["resources"])
        pdfs = [r for r in all_resources if exporter.urlparse(r["url"]).path.lower().endswith(".pdf")]
        self.assertTrue(pdfs)
        for item in pdfs:
            self.assertIn(item["kind"], {"pdf", "course_notes_pdf", "textbook_pdf"})
            self.assertTrue(item["coverage"])
            if item["coverage"] == "complete_textbook":
                self.assertEqual("textbook_pdf", item["kind"])
        calculus = [r for r in all_resources if r["id"].startswith("mit_18_01sc_2010")]
        self.assertFalse(any(r["coverage"] == "complete_textbook" for r in calculus))

    def test_html_content_is_escaped_and_tcf_audio_points_into_repo(self):
        self.assertEqual("&lt;script&gt;&amp;&quot;", exporter.esc('<script>&"'))
        listening = self.course_map["tcf_canada_listening_practice_2026"]
        for lesson in listening["lessons"]:
            for exercise in lesson["exercises"]:
                asset_url = "../../composeApp/src/commonMain/composeResources/" + exercise["audioResourcePath"]
                note = self.outputs[exporter.OUTPUT_DIR / "tcf_canada_listening_practice_2026.md"]
                self.assertIn(asset_url, note)


if __name__ == "__main__":
    unittest.main()
