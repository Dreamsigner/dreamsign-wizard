from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import verify_bundled_audio as checker


class BundledAudioTest(unittest.TestCase):
    def test_actual_archive_must_contain_the_current_source_bytes(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / "source"
            source.mkdir()
            (source / "document.wav").write_bytes(b"original audio fixture")
            apk = root / "application.apk"
            with patch.object(checker, "SOURCE", source):
                with zipfile.ZipFile(apk, "w") as z:
                    z.writestr("assets/composeResources/files/audio/document.wav", b"original audio fixture")
                checker.verify(archive=apk)
                with zipfile.ZipFile(apk, "w") as z:
                    z.writestr("assets/composeResources/files/audio/document.wav", b"stale")
                with self.assertRaisesRegex(ValueError, "stale"):
                    checker.verify(archive=apk)
                with zipfile.ZipFile(apk, "w") as z:
                    z.writestr("unrelated.txt", "no audio")
                with self.assertRaisesRegex(ValueError, "missing"):
                    checker.verify(archive=apk)

    def test_desktop_jars_and_ios_bundle_are_checked(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / "source"
            source.mkdir()
            (source / "document.wav").write_bytes(b"fixture")
            product = root / "product"
            product.mkdir()
            with zipfile.ZipFile(product / "application.jar", "w") as z:
                z.writestr("composeResources/files/audio/document.wav", b"fixture")
            (product / "document.wav").write_bytes(b"fixture")
            with patch.object(checker, "SOURCE", source):
                checker.verify(directory=product, desktop=True)
                checker.verify(directory=product)


if __name__ == "__main__":
    unittest.main()
