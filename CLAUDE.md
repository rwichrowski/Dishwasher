# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**"Kto ma dyżur na zmywarkę?"** — A Polish family chore rotation app tracking dishwasher duty, plus a secondary weight/calorie tracker screen backed by Firebase.

- Package: `pl.radoslaw.zmywarka`
- Min SDK: 26 (Android 8.0), Target/Compile SDK: 34 (Android 14)
- Language: Kotlin with View Binding

## Build Commands

```bat
gradlew.bat assembleDebug
gradlew.bat assembleRelease
gradlew.bat installDebug
gradlew.bat lint
gradlew.bat clean
```

## Architecture

Two-activity app with no tests, no ViewModel, and no dependency injection.

### MainActivity — Dishwasher Duty Rotation
Duty assignment is **purely date-based**, calculated each time from a hardcoded `referenceMonday` (`LocalDate`) and the current date. No SharedPreferences; state is implicit in the calendar.

- `people` list and `referenceMonday` are hardcoded at the top of `MainActivity.kt`. To change who rotates or the epoch week, edit those two fields.
- `updateUI()` computes the current week index via `ChronoUnit.WEEKS.between(referenceMonday, currentMonday) % people.size`.
- `buildCalendar()` renders a 5-week window (−2 to +2 from current) directly into `calendarLayout` (a `LinearLayout`) programmatically — no RecyclerView or adapter.
- Swiping left navigates to `WeightActivity` with a slide-right animation.

### WeightActivity — Weight Tracker
Firebase-backed screen accessible by swiping left from `MainActivity`. Swiping right (or pressing back) returns to `MainActivity`.

- **Auth:** Anonymous Firebase Auth (`signInAnonymously`). The userId is hardcoded as `"radek"`.
- **Firestore path:** `artifacts/weight-tracker-cloud/users/radek/weights/{date}` — documents keyed by ISO date string (YYYY-MM-DD).
- **Data:** Each document stores `date`, `weight` (Double), `timestamp`, and optional `calories` (Long).
- `listenToEntries()` attaches a real-time snapshot listener filtered to the last 30 days.
- The entry list is rendered programmatically into `listContainer` (`LinearLayout`) as `TextView`s.
- When a date is selected via `DatePickerDialog`, the form pre-fills from `cachedEntries` if an entry already exists, and the save button switches label between "Zapisz" / "Zaktualizuj".

### Navigation / Gesture System
Both activities implement the same swipe-gesture pattern via `GestureDetector` in `dispatchTouchEvent`. Slide animations live in `res/anim/` (slide_in_right, slide_out_left, slide_in_left, slide_out_right).

## Dependencies

- `androidx.core:core-ktx:1.12.0`
- `androidx.appcompat:appcompat:1.6.1`
- `com.google.android.material:material:1.11.0`
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `com.google.firebase:firebase-bom:32.7.2` (Firestore + Auth via BOM)

Firebase is configured via `google-services.json` (not checked in) and the `com.google.gms.google-services` Gradle plugin.
