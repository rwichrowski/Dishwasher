# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**"Kto ma dyżur na zmywarkę?"** — A Polish family chore rotation app for tracking dishwasher duty. Single-screen app that cycles through family members ("Mama", "Tata", "Radosław") and persists the current index via SharedPreferences.

- Package: `pl.radoslaw.zmywarka`
- Min SDK: 26 (Android 8.0), Target/Compile SDK: 34 (Android 14)
- Language: Kotlin with View Binding

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run lint checks
./gradlew lint

# Clean build
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

Single-activity app, no tests, no ViewModel or dependency injection. All logic lives in `MainActivity.kt`:

- **State persistence:** SharedPreferences stores the person list and current index
- **UI binding:** View Binding (enabled in `app/build.gradle.kts`)
- **Layout:** `activity_main.xml` — CoordinatorLayout with AppBar, two Material CardViews (current duty + queue), and a "Done" button
- **Menu:** `main_menu.xml` — single "Reset" option to restart from the first person

The family member list is hardcoded in `MainActivity.kt`. To add/remove members, edit that list directly.

## Key Files

- `app/src/main/java/pl/radoslaw/zmywarka/MainActivity.kt` — all app logic
- `app/src/main/res/layout/activity_main.xml` — full UI layout
- `app/src/main/res/values/strings.xml` — Polish UI strings
- `app/src/main/res/values/themes.xml` — Material DayNight theme

## Dependencies

- `androidx.core:core-ktx:1.12.0`
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.11.0`
- `androidx.constraintlayout:constraintlayout:2.1.4`

No Compose, no Room, no coroutines, no networking libraries.
