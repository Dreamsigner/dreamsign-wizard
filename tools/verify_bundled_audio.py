"""Check that actual native build products contain the authored listening documents."""
import argparse
import hashlib
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "composeApp/src/commonMain/composeResources/files/audio"


def verify(archive=None, directory=None, desktop=False):
    expected = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCE.glob("*.wav")}
    if not expected:
        raise ValueError("No authored listening documents found")
    actual = {}
    if archive or desktop:
        paths = [Path(archive)] if archive else sorted(Path(directory).rglob("*.jar"))
        for path in paths:
            with zipfile.ZipFile(path) as z:
                for name in z.namelist():
                    filename = name.rsplit("/", 1)[-1]
                    if filename in expected:
                        actual.setdefault(filename, set()).add(hashlib.sha256(z.read(name)).hexdigest())
    else:
        for path in Path(directory).rglob("*.wav"):
            if path.name in expected:
                actual.setdefault(path.name, set()).add(hashlib.sha256(path.read_bytes()).hexdigest())
    for filename, digest in expected.items():
        if actual.get(filename) != {digest}:
            raise ValueError(f"Bundled listening document missing, stale or conflicting: {filename}")
    print(f"PASS: {len(expected)} authored audio assets present with exact source hashes")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--apk")
    group.add_argument("--ios-app")
    group.add_argument("--desktop-dir")
    args = parser.parse_args()
    verify(args.apk, args.ios_app or args.desktop_dir, bool(args.desktop_dir))
