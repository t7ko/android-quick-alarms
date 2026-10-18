# Notes for AI coding agents: quick-alarms

An Android alarm app for one user, with two flows on one main screen:

1. Wake up: tap one of a few saved times for a one-time alarm at its next occurrence.
2. Meetings: tap a meeting start time in a grid for today; the alarm rings 3 minutes
   before it (5 on a long press). The user reads the times off their calendar; there is
   deliberately no calendar integration.

The main screen is the one used every day; the alarm list is rarely needed.

Machine-specific notes, where they exist, are in the git-ignored `AGENTS.local.md`. Keep
paths, file names and other details of one machine out of tracked files.

## Decisions

- The app rings its own alarms rather than handing them to the Clock app: Clock's
  `ACTION_SET_ALARM` cannot delete or move an alarm afterwards and leaves every meeting
  time behind as a disabled alarm.
- `AlarmManager.setAlarmClock()` only, as Clock apps do: exact, exempt from Doze, shown
  in the status bar. Exact alarms come from `USE_EXACT_ALARM` (granted at install from
  Android 13; `SCHEDULE_EXACT_ALARM` on Android 12). Fine for a sideloaded app; Google
  Play would restrict it.
- Ringing: `AlarmReceiver` starts `AlarmService`, a `mediaPlayback` foreground service
  that plays the sound on the alarm stream (`Ringer`) and posts a full-screen
  notification that opens `RingActivity` over the lock screen.
- Alarms store local wall-clock times (`LocalDateTime`), so 7:00 stays 7:00 after a time
  zone change. `RescheduleReceiver` registers them again after a reboot, an app update or
  a clock change; `MainActivity` does too, because a force stop clears them.
- Everything is direct-boot aware and stored in device-protected storage, so alarms
  still ring after an overnight reboot before the phone is unlocked. If no sound file can
  be read then, `Ringer` beeps with `ToneGenerator`.
- Storage is JSON in `SharedPreferences`, written with `commit()` because receivers can
  exit right after a change. There are only a few alarms.
- On Xiaomi phones (`Permissions.isXiaomi`), `XiaomiCard` lists the MIUI/HyperOS
  settings alarms need (Autostart, battery saver, "Show on lock screen", background
  pop-ups). Apps cannot read them, so the card stays until the user taps Done
  (`Settings.xiaomiSetupDone`). Its buttons open App info and MIUI's permission editor,
  falling back to App info where that is missing or not exported.
- Time dialogs (`TimeDialog` in `ui/Inputs.kt`) open on scroll wheels (`ui/Wheel.kt`, a
  snapping `LazyColumn` that wraps around), minutes in 5-minute steps; "Type" switches to
  Material's `TimeInput` for any minute. Material 3 has no wheel picker.
- English only, strings in code.

## Layout

- `app/src/main/java/t7helpers/quickalarms/`: model (`Alarm`, `Settings`), pure time
  logic (`AlarmTimes`), storage (`Repositories`), scheduling, receivers, the service.
- `.../ui/`: Compose screens. `HomeScreen` with `HomeGrids`, `HomeCards`,
  `HomeDialogs` and `HomeActions` (what taps do, with Undo snackbars); `AlarmListScreen`,
  `SettingsScreen`, `RingActivity`. Navigation is a plain enum in `MainActivity`.
- `app/src/test/`: JVM unit tests for `AlarmTimes` and the storage format.

## Build and releases

Versions are in `gradle/libs.versions.toml`. The scripts are for Windows PowerShell.

- Published at <https://github.com/t7ko/android-quick-alarms>. The app is installed and
  updated through Obtainium from its GitHub releases.
- A release: raise `versionCode` and `versionName` in `app/build.gradle.kts`, commit, run
  `scripts/release.ps1`. It builds (`scripts/build.ps1`), tags `v<versionName>`, pushes
  `main` and the tag, creates the release and attaches `quick-alarms-<version>.apk`.
  Obtainium compares the tag with the installed versionName.
- Release builds are signed with a key kept outside the repository. The git-ignored
  `local.properties` names it: `release.storeFile`, `release.storePasswordFile` (a file
  holding the password) and `release.keyAlias`. Without them, release builds come out
  unsigned and `build.ps1` stops. Certificate SHA-256:
  `ffe6c4a14fce94626c74659b51652ba6b84ebdb963302f63eeb0d6ab0b272716`. Every update must
  use this key, or the app has to be uninstalled and loses its data.
- Publishing needs a fine-grained GitHub token with Contents read/write on this
  repository only, in the file that `github.tokenFile` in `local.properties` names.
  `release.ps1` hands it to git as an HTTP header through `GIT_CONFIG_*` environment
  variables. Never print a token or password, or put one on a command line.
- `build.ps1` uses `JAVA_HOME`, or else `java.home` from `local.properties`.
- Debug builds (`gradlew.bat assembleDebug`) are signed with the debug key, so a device
  needs `adb uninstall t7helpers.quickalarms` when switching between debug and release
  builds. `scripts/emulator-ui.py` drives the app over adb for manual tests.

## Tested

On 2026-10-17, version 0.1.0 on an Android 14 emulator (Pixel 6 profile, google_apis image):

- Permission card: Allow opens the notification prompt; the card goes once granted.
- A meeting slot alarm rings on time with the screen off behind a PIN: the screen turns
  on, `RingActivity` shows over the lock screen, the sound plays on the alarm stream with
  audio focus, and it vibrates.
- With the phone in use, the alarm shows as a heads-up notification (as Android intends)
  and the main screen shows the Ringing card. Tapping the notification opens the alarm
  screen; its Dismiss action works.
- Snooze stops the alarm, posts "Snoozed until", and rings again on time (also after a
  clock jump, so `RescheduleReceiver` handles TIME_SET); the snoozed notification goes.
- Dismiss stops sound, vibration, service and screen, and deletes the alarm.
- After a reboot, still locked (`RUNNING_LOCKED`), the alarm is registered again and
  rings. The chosen sound cannot be read then; `Ringer` falls back to the system's
  default alarm URI, which Android caches, so the real sound plays.

On 2026-10-18, version 0.2.0: installed over 0.1.0 (same key, data kept). With
`isXiaomi` forced on for the test, the Xiaomi card shows, App info opens, Permissions
falls back to App info (no MIUI on the emulator), Done hides it and Settings shows it
again. Without the forcing it stays hidden on the emulator.

On 2026-10-18, version 0.3.0: the time dialogs open on wheels at the expected time; slow
swipes move hours and minutes by whole rows (minutes by 5), a fling spins and snaps into
the highlight; Type shows the wheel time, typed 10:17 rounds to 10:15 on the way back to
the wheel, and Add from typing keeps 10:17 (alarm 10:14). Typing through adb loses keys
sent too fast; send them one `input keyevent` at a time.

The first signed release build (0.3.0, not debuggable) installs, sets an alarm, rings on
time over the lock screen and dismisses cleanly on the emulator.

Not tried yet: auto-stop after `ringMinutes` with its "Missed" notification, time zone
changes, Android 8-13, and MIUI/HyperOS on a real Xiaomi phone.
