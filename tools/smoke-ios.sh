#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/ios
project=iosApp/iosApp.xcodeproj
runtime=com.apple.CoreSimulator.SimRuntime.iOS-17-4
for device in 'iPhone 15' 'iPad (10th generation)'; do
  if [[ "$device" == iPhone* ]]; then
    label=phone
    device_type=com.apple.CoreSimulator.SimDeviceType.iPhone-15
  else
    label=tablet
    device_type=com.apple.CoreSimulator.SimDeviceType.iPad-10th-generation
  fi
  udid=$(xcrun simctl create "Duolingo-$label" "$device_type" "$runtime")
  xcrun simctl boot "$udid"
  xcrun simctl bootstatus "$udid" -b
  xcodebuild -project "$project" -scheme iosApp -configuration Debug \
    -destination "platform=iOS Simulator,id=$udid" \
    -derivedDataPath build/ios/DerivedData \
    -resultBundlePath "build/ios/$label.xcresult" \
    CODE_SIGNING_ALLOWED=NO test
  app=build/ios/DerivedData/Build/Products/Debug-iphonesimulator/DuolingoKmpApp.app
  xcrun simctl install "$udid" "$app"
  xcrun simctl launch "$udid" com.duolingo.app
  sleep 5
  xcrun simctl io "$udid" screenshot "build/ios/home-$label.png"
  xcrun simctl shutdown "$udid"
done
ditto -c -k --sequesterRsrc --keepParent "$app" build/ios/DuolingoKmpApp-simulator.zip
