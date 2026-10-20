"""Discovery scans must not overwrite reviewed source inventories."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import inventory_sources


class DiscoveryPreservationTest(unittest.TestCase):
    def test_fresh_scan_preserves_audited_inventory_and_published_manifest(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            audited = root / "ossu-course-inventory.json"
            manifest = root / "mit-18-01sc-source-sessions.json"
            audited.write_text('{"reviewed":"keep syllabus and provenance"}', encoding="utf-8")
            manifest.write_text('{"frozen":"published source mapping"}', encoding="utf-8")
            originals = audited.read_bytes(), manifest.read_bytes()
            calculus = {"sessionCount": 1, "sessions": [{"number": 1}]}
            with patch.object(inventory_sources, "OUT", root), \
                 patch.object(inventory_sources, "ossu_courses", return_value=[{"title": "One"}]), \
                 patch.object(inventory_sources, "discover", return_value={"title": "One", "sessionCount": None}), \
                 patch.object(inventory_sources, "calculus_sessions", return_value=calculus), \
                 patch("sys.argv", ["inventory_sources.py"]), contextlib.redirect_stdout(io.StringIO()):
                inventory_sources.main()
            self.assertEqual(originals, (audited.read_bytes(), manifest.read_bytes()))
            discovery = json.loads((root / "ossu-course-discovery.json").read_text(encoding="utf-8"))
            self.assertEqual(1, discovery["referenceCount"])
            self.assertNotIn("courseCount", discovery)
            self.assertTrue((root / "mit-18-01sc-source-discovery.json").exists())


if __name__ == "__main__":
    unittest.main()
