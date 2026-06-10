# Building Consecutor

This file is the short entry point for building the app.

For the full developer workflow, including:

- macOS and Linux setup
- command-line build and test commands
- manual QA
- signing
- generating APKs and AABs
- Google Play deployment
- F-Droid submission

see [DEVELOPERS.md](DEVELOPERS.md).

## Quick Start

Requirements:

- Android Studio Koala or newer
- JDK 17
- Android SDK 34

Basic local flow:

1. Clone the repository.
2. Open it in Android Studio.
3. Confirm Gradle is using JDK 17.
4. Let the project sync.
5. Run the `app` configuration on a device or emulator running Android 8.0+.

Useful commands:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew installDebug`

## Fire It Up For MVP Testing

From a machine with Android Studio, JDK 17, and Android SDK 34 installed:

1. Open the repository in Android Studio.
2. Let Gradle sync finish.
3. Start an emulator from Device Manager, or connect an Android device with USB debugging enabled.
4. Run the `app` configuration from Android Studio.

Command-line flow:

1. Verify Java: `java -version`
2. Verify Gradle: `./gradlew -version`
3. Run unit tests: `./gradlew test`
4. Build a debug APK: `./gradlew assembleDebug`
5. Install to a connected emulator/device: `./gradlew installDebug`
6. Launch from the device launcher, or run `adb shell monkey -p com.squalor.consecutor -c android.intent.category.LAUNCHER 1`

Debug APK output:

- `app/build/outputs/apk/debug/app-debug.apk`

MVP smoke test:

1. Create a `YES_NO` tracker with a daily target, then quick-log it from the dashboard.
2. Create a `COUNT` tracker with a weekly target, add entries for current and past dates, and confirm streaks update.
3. Create a `MEASURE` tracker, open its detail screen, and log a numeric value.
4. Edit and delete at least one entry.
5. Archive a tracker and confirm it leaves the active dashboard.
6. Export CSV from Settings and confirm Android opens the share sheet.
7. Export a JSON backup, clear app data or reinstall, import that backup, and confirm trackers return.
8. On Android 13+, enable notifications and create a reminder to verify the permission prompt and local notification behavior.

## Build Notes

- The app is Android-first in the current repository.
- The persistence layer is Room over SQLite.
- Automatic Android backup is disabled intentionally.
- Portability is handled through explicit CSV export and JSON backup import/export.
- Reminder notifications require notification permission on Android 13+.
