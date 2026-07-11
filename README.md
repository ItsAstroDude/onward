# Onward

A native Android app for following a heart-healthy eating pattern **without it feeling like a diet**.
There is no "restart" in Onward — a missed day is never a failure, you're just *onward* to the next meal.

Built as a forgiving habit system, not a food-logging app:

- **One tap a day** — "Hit the pattern today?" Yes / Not really. The 80% rule: mostly followed the
  shape of the day = a yes.
- **A forgiving streak** — one free miss per rolling 7 days never breaks the run (a streak freeze).
  Even a broken streak just means "new run starts today" — no shame copy anywhere.
- **The week plan** — a rotating 7-day template (shake for breakfast, protein + veg + a carb after),
  every meal swappable.
- **Cheat sheet** — the breakfast shake + variations, the meal template, and the cardiologist swap
  list, always one tap away.
- **Shopping list** — seeded with the staples, tied to the Sunday grocery reminder.
- **Loose calorie tracker** *(optional, off by default)* — quick-add rough estimates and a 7-day
  trend. No targets, no "over budget" states, never touches the streak.
- **Home/cover-screen widget** — the streak at a glance, resizable in both dimensions from a tiny
  number up to a full card with today's meals and a one-tap check-off.
- **History calendar** — every month of hits and free misses (nothing is ever red), with optional
  per-day notes. A one-day "And yesterday?" grace chip catches forgotten check-ins.
- **Self-updating** — checks GitHub Releases once a day and installs new versions in-app.
- **Backup** — JSON export/import of everything, from Settings.

Fully offline and local (Room) apart from the update check. No backend, no accounts.

## Tech

- Kotlin + Jetpack Compose, Material 3 (custom pine/citrus theme, Fraunces + Inter type)
- Room for all persistence · WorkManager for reminder notifications
- minSdk 34, target/compile SDK 36 · single module, Gradle KTS

## Build & run

Requirements: JDK 21, Android SDK with platform android-36.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
.\gradlew.bat :app:assembleDebug        # APK → app/build/outputs/apk/debug/
.\gradlew.bat :app:assembleRelease      # signed release APK (needs key.properties)
.\gradlew.bat :app:testDebugUnitTest    # streak-engine unit tests
```

**Releases are signed.** `key.properties` (not in git) points at the release keystore in
`C:\Users\andre\.android\onward-release.keystore`. Ship releases via
`gh release create vX.Y.Z <apk>` — installed apps pick them up through the in-app updater.
Never ship a release signed with a different key: devices will refuse to update in place.

Install on a device/emulator:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

## First run

Onboarding asks "Ready to start?" — **reminders stay off** until you say yes there (or flip the
master switch in Settings later). All six reminder times are editable per-reminder.

## Disclaimer

Onward follows a cardiologist's general recommendations but isn't medical advice. Specific numbers
from your doctor (salt limits, cholesterol targets, calorie deficit) override anything in the app.
