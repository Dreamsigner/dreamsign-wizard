#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/android-evidence
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb logcat -c
trap 'adb logcat -d > build/android-evidence/logcat.txt' EXIT
adb shell am start -W -n com.dreamsigner.wizard/.MainActivity
sleep 5
adb shell pidof com.dreamsigner.wizard
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml build/android-evidence/window.xml
grep -q 'Cours' build/android-evidence/window.xml
grep -q 'Profil' build/android-evidence/window.xml
adb exec-out screencap -p > build/android-evidence/home-phone.png
gradle :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.dreamsigner.wizard.ui.LearningAndroidUiTest \
  --stacktrace
