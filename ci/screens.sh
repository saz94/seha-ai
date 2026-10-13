#!/usr/bin/env bash
# Runs inside the CI emulator: installs the app and photographs its screens with sample data.
set -u
APK=app/build/outputs/apk/debug/app-debug.apk
PKG=ai.seha.app
OUT=shots
mkdir -p "$OUT"
adb install -r "$APK"
adb shell settings put system screen_off_timeout 1800000 || true
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put secure anr_show_background 0 || true
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell input keyevent KEYCODE_HOME; sleep 20
TODAY=$(adb shell date +%F | tr -d '\r')

shot() { # route name [wait]
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  adb shell am start -S -W -n $PKG/.MainActivity --ez demo true --es route "$1" >/dev/null
  sleep "${3:-9}"
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2"
}

adb shell cmd uimode night no
adb shell cmd locale set-app-locales $PKG --locales en || true

# first run, with no data: the onboarding
adb shell am start -S -W -n $PKG/.MainActivity >/dev/null; sleep 25
adb exec-out screencap -p > "$OUT/warmup.png"
adb shell am start -S -W -n $PKG/.MainActivity >/dev/null; sleep 9
adb exec-out screencap -p > "$OUT/en_onboarding.png"

shot today en_today 12
adb shell input swipe 540 1900 540 500 400; sleep 2
adb exec-out screencap -p > "$OUT/en_today_2.png"
shot checkin en_checkin
shot meals en_meals
shot plan en_plan
shot "meal/$TODAY/m2" en_recipe
shot shopping en_shopping
shot coach en_coach
shot progress en_progress
adb shell input swipe 540 1900 540 500 400; sleep 2
adb exec-out screencap -p > "$OUT/en_progress_2.png"
shot measure en_measure
shot more en_settings
adb shell input swipe 540 1900 540 500 400; sleep 2
adb exec-out screencap -p > "$OUT/en_settings_2.png"
shot ai en_ai
shot profile en_profile
shot medical en_medical
shot health en_health

adb shell cmd uimode night yes
shot today dark_today
shot meals dark_meals
shot "meal/$TODAY/m2" dark_recipe
shot coach dark_coach
shot progress dark_progress
adb shell cmd uimode night no

adb shell cmd locale set-app-locales $PKG --locales ar || true
sleep 2
shot today ar_today 12
shot meals ar_meals
shot "meal/$TODAY/m2" ar_recipe
shot coach ar_coach
shot progress ar_progress
shot checkin ar_checkin
adb shell cmd uimode night yes
shot today ar_dark_today
adb shell cmd uimode night no
adb shell cmd locale set-app-locales $PKG --locales en || true

# widgets: ask the launcher to place each one, press its "Add" button, then photograph the home screen
pin() { # route
  adb shell am start -S -W -n $PKG/.MainActivity --ez demo true --es route "$1" >/dev/null
  sleep 10
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > /tmp/ui.xml || true
  XY=$(python3 - <<'PY'
import re
x = open('/tmp/ui.xml', encoding='utf-8', errors='ignore').read()
best = None
for m in re.finditer(r'text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    t = m.group(1).strip().lower()
    if t in ('add to home screen', 'add automatically', 'add'):
        a, b, c, d = map(int, m.groups()[1:])
        best = ((a + c) // 2, (b + d) // 2)
print('%d %d' % best if best else '')
PY
)
  echo "pin button at: $XY"
  if [ -n "$XY" ]; then adb shell input tap $XY; sleep 4; fi
}
pin pintoday
pin pinmeal
adb shell input keyevent KEYCODE_HOME; sleep 6
adb exec-out screencap -p > "$OUT/widgets_home.png"
adb shell input swipe 800 1200 200 1200 300; sleep 3
adb exec-out screencap -p > "$OUT/widgets_home_2.png"

# any crash during the run ends up here
adb logcat -d -s AndroidRuntime:E > "$OUT/crashes.txt" || true
ls -la "$OUT"
