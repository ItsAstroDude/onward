# CLAUDE.md — Onward

Native Android app: a **forgiving** heart-healthy eating-pattern tracker. Read
`README.md` for what it does; this file is architecture + conventions.

## The one design rule that overrides everything

**No failure language, no shame states, nothing the user can "fail."** A missed day is absorbed
by a streak freeze or simply starts a new run ("New run starts today" — never "streak lost").
The optional calorie tracker has **no target** on purpose — do not add goals, budgets, red
over-limit states, or streak penalties to it. The default experience must stay one tap per day.

## Toolchain (hard-won on this Windows machine — do not "upgrade" casually)

- AGP **8.11.1** + Gradle **8.13** + Kotlin **2.0.21** + Compose BOM **2024.12.01** + KSP
  2.0.21-1.0.28. AGP 9 breaks this setup (built-in-Kotlin collision) — don't use it here.
- compileSdk 36 needs `android.suppressUnsupportedCompileSdk=36` in `gradle.properties`.
- Build with JDK 21: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"`
  then `.\gradlew.bat :app:assembleDebug`.
- `local.properties` must point at `C:\Users\andre\AppData\Local\Android\Sdk`.

## Release signing & OTA (v0.2+) — the rules that can't be broken

- Keystore: `C:\Users\andre\.android\onward-release.keystore`, referenced by the gitignored
  `key.properties` at repo root. **Losing this keystore = installed devices can never update in
  place.** Never sign a release with a different key.
- OTA: `updates/Updates.kt` polls `https://api.github.com/repos/ItsAstroDude/onward/releases/latest`
  (repo must stay PUBLIC or the unauthenticated check breaks), compares the tag against
  `BuildConfig.VERSION_NAME`, downloads the release's `.apk` asset via DownloadManager, and
  `UpdateInstallReceiver` hands it to the package installer.
- Ship flow: bump `versionCode`/`versionName` → `.\gradlew.bat :app:assembleRelease` →
  `gh release create vX.Y.Z <apk>` → installed apps see it within a day (or "Check now").

## Architecture

Single module, MVVM-lite, no DI framework. `OnwardApp` (Application) exposes `database` +
`scheduler`; ViewModels grab it via `viewModel { X(this[APPLICATION_KEY] as OnwardApp) }`.

```
com.astro.onward
├── OnwardApp.kt            Application: notification channel, seed + reminder sync on start
├── MainActivity.kt         single Activity; notification deep links via EXTRA_DESTINATION
├── data/
│   ├── Entities.kt         Room entities (DayEntry, PlanDay, MealOption, ShoppingItem,
│   │                       ReminderSetting, AppSettings single-row, CalorieEntry)
│   ├── Daos.kt             Flow-based DAOs
│   ├── OnwardDatabase.kt   Room db + idempotent ensureSeeded() (seedVersion-gated, IGNORE
│   │                       conflict strategy so user edits are never clobbered)
│   ├── SeedData.kt         ALL baked-in content: 7-day rotation, meal option pools,
│   │                       reminder defaults, shopping staples. Bump SEED_VERSION to add.
│   └── StreakCalculator.kt pure streak engine — see below
├── reminders/
│   ├── Notifications.kt    channel + permission-checked show(); deep-link extras
│   ├── ReminderScheduler.kt per-reminder unique OneTimeWork delayed to next occurrence
│   └── ReminderWorker.kt   fires notification, reschedules itself (no boot receiver needed —
│                           WorkManager persists across reboots)
├── updates/Updates.kt      GitHub-releases self-updater (see OTA section above)
├── widget/                 Glance widget: 4 responsive breakpoints (tiny→full), pine/citrus,
│                           one-tap check-off via ActionCallback; WidgetRefreshWorker re-renders
│                           after midnight; every streak write calls OnwardWidget().updateAll()
└── ui/
    ├── OnwardRoot.kt       onboarding gate → 4-tab scaffold (today/plan/sheet/shopping)
    │                       + pushed settings/history routes; deep-link consumption
    ├── theme/              pine #1E4635 · citrus #E8813A (STREAK/POSITIVE MOMENTS ONLY) ·
    │                       off-white #F5F6F1; Fraunces (display) + Inter (body), bundled
    │                       variable TTFs in res/font; rememberReducedMotion()
    └── today|plan|sheet|shopping|settings|onboarding/  screen + ViewModel pairs
```

## The streak engine (`StreakCalculator`)

Pure function `calculate(Map<epochDay, DayStatus>, today) -> Result`, derived on read — freezes
are never written to the DB. Rules: HIT extends the run; a miss (answered "not really" OR a past
unanswered day) is absorbed by a freeze if none was used in the trailing 7 days and there's a run
to protect (frozen days keep the run alive but don't grow it); a second miss in the rolling week
resets the run to 0; **today unanswered is pending, never a miss**. Also produces longest run,
hits this month, and month % (80%+ = the "win" framing). Unit tests in
`app/src/test/.../StreakCalculatorTest.kt` — run `.\gradlew.bat :app:testDebugUnitTest` after
touching it.

## Reminders

All gated by `AppSettings.started` (master) AND per-reminder `enabled`. First run: `started=false`
until onboarding's "I'm ready" — the user may still be gathering supplies; **never enable
reminders by default**. `ReminderScheduler.syncAll()` is the single reconciliation point — call it
after any settings/reminder change. The 21:30 check-off nudge deep-links to Today, the Sunday
grocery one to the shopping tab.

## Conventions

- Copy tone: warm, brief, zero guilt. Romanian food terms (ciorbă, smântână, macrou) are
  intentional — keep them.
- Citrus (`tertiary`) is reserved for streak/positive moments; everything else uses pine/sage.
- Seeds: content changes go in `SeedData` + SEED_VERSION bump; conflict strategy is IGNORE so
  existing user rows always win.
- Respect `rememberReducedMotion()` for any new decorative animation.
- Target device is a Galaxy Z Flip 7 — test tall/narrow and cover-screen-adjacent layouts.
