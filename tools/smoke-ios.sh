#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/ios
project=iosApp/iosApp.xcodeproj
app=build/ios/DerivedData/Build/Products/Debug-iphonesimulator/DuolingoKmpApp.app
# Inventory the actual runner; SDK names alone do not prove installed runtimes.
xcrun simctl list runtimes -j > build/ios/simulator-runtimes.json
xcrun simctl list devicetypes -j > build/ios/simulator-device-types.json
xcrun simctl list devices available -j > build/ios/simulator-devices.json
python3 - "${IOS_SIMULATOR_RUNTIME_VERSION_PREFIX:-26.0}" <<'PY' > build/ios/selected-devices.tsv
import json
import sys
from pathlib import Path
base = Path("build/ios")
prefix = sys.argv[1]
runtimes = json.loads((base / "simulator-runtimes.json").read_text())["runtimes"]
matching = [r for r in runtimes if r.get("isAvailable") and r.get("name", "").startswith("iOS ")
            and (r.get("version") == prefix or r.get("version", "").startswith(prefix + "."))]
if not matching:
    raise SystemExit(f"No available iOS runtime matching {prefix}; inspect simulator-runtimes.json")
runtime = sorted(matching, key=lambda r: tuple(int(v) for v in r["version"].split(".")))[-1]
devices = json.loads((base / "simulator-devices.json").read_text())["devices"].get(runtime["identifier"], [])
selected = []
for label, family in (("phone", "iPhone"), ("tablet", "iPad")):
    candidates = [d for d in devices if d.get("isAvailable") and d.get("name", "").startswith(family)]
    if not candidates:
        raise SystemExit(f"No available {family} in {runtime['identifier']}; inspect simulator-devices.json")
    device = sorted(candidates, key=lambda d: d["name"])[0]
    selected.append({"label": label, **device})
(base / "selected-simulators.json").write_text(json.dumps({"runtime": runtime, "devices": selected}, indent=2) + "\n")
for device in selected:
    print(device["label"], device["udid"], device["state"], sep="\t")
    print(f"Selected {device['name']} ({device['udid']}) on {runtime['name']} / {runtime['version']}", file=sys.stderr)
PY

tests_failed=0
booted_by_script=""
cleanup() {
  for simulator in $booted_by_script; do
    xcrun simctl shutdown "$simulator" >/dev/null 2>&1 || true
  done
}
trap cleanup EXIT
while IFS=$'\t' read -r label udid state; do
  if [[ "$state" != Booted ]]; then
    xcrun simctl boot "$udid"
    booted_by_script="$booted_by_script $udid"
  fi
  xcrun simctl bootstatus "$udid" -b
  test_status=0
  xcodebuild -project "$project" -scheme iosApp -configuration Debug \
    -destination "platform=iOS Simulator,id=$udid" \
    -derivedDataPath build/ios/DerivedData \
    -resultBundlePath "build/ios/$label.xcresult" \
    CODE_SIGNING_ALLOWED=NO test || test_status=$?
  if [[ -d "$app" ]]; then
    if xcrun simctl install "$udid" "$app" && xcrun simctl launch "$udid" com.duolingo.app; then
      sleep 5
    else
      tests_failed=1
    fi
  else
    echo "Simulator application missing: $app" >&2
    tests_failed=1
  fi
  xcrun simctl io "$udid" screenshot "build/ios/home-$label.png" || tests_failed=1
  if [[ "$state" != Booted ]]; then xcrun simctl shutdown "$udid" || true; fi
  if [[ "$test_status" != 0 ]]; then tests_failed=1; fi
done < build/ios/selected-devices.tsv
if [[ -d "$app" ]]; then
  ditto -c -k --sequesterRsrc --keepParent "$app" build/ios/DuolingoKmpApp-simulator.zip
fi
exit "$tests_failed"
