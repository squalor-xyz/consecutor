# Developers

This guide is for building, testing, signing, and shipping Consecutor.

## Scope

The current repository is Android-first:

- Jetpack Compose UI
- Room over SQLite
- manual dependency wiring
- local reminder notifications
- CSV export and JSON backup import/export

Future plans such as iOS support and shared-core extraction live in [ROADMAP.md](ROADMAP.md). This repository does not yet include an iOS client or Kotlin Multiplatform module.

## Requirements

- Android Studio Koala or newer
- JDK 17
- Android SDK 34
- Android build-tools installed through the SDK manager
- A device or emulator running Android 8.0+ for testing

## macOS Setup

Recommended:

1. Install Android Studio.
2. Install JDK 17 if Android Studio is not already using one.
3. Install Android SDK Platform 34, Platform Tools, and Build Tools from Android Studio.
4. Accept Android SDK licenses.

Typical package-manager path:

- `brew install --cask android-studio`
- `brew install openjdk@17`

After installing a JDK through Homebrew, ensure Android Studio or your shell can see it. Verify with:

- `java -version`
- `./gradlew -version`

Apple Silicon notes:

- Prefer an ARM64 emulator image when testing on Apple Silicon.
- If you test on a physical iPhone later for future iOS work, that is outside the scope of this repo today.

## Linux Setup

Recommended:

1. Install Android Studio.
2. Install OpenJDK 17.
3. Install Android SDK Platform 34, Platform Tools, and Build Tools.
4. Accept Android SDK licenses.

Typical package-manager commands vary by distribution, but common examples are:

- Debian/Ubuntu: `sudo apt install openjdk-17-jdk`
- Fedora: `sudo dnf install java-17-openjdk-devel`
- Arch: `sudo pacman -S jdk17-openjdk`

Verify with:

- `java -version`
- `./gradlew -version`

Linux emulator notes:

- Hardware acceleration usually requires KVM.
- Ensure your user has access to virtualization support if emulator performance is poor.
- Physical-device testing over `adb` is often simpler than emulator setup on locked-down Linux machines.

## Repository Setup

1. Clone the repository.
2. Open it in Android Studio.
3. Confirm Gradle uses JDK 17.
4. Let Gradle sync.
5. Install or start an emulator, or connect a physical Android device.
6. Run the `app` configuration.

## Command Line Build And Test

Core commands:

- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew installDebug`
- `./gradlew assembleRelease`
- `./gradlew bundleRelease`

Useful inspection commands:

- `./gradlew tasks`
- `./gradlew dependencies`

If Gradle fails immediately with a Java runtime error, the environment is missing a working JDK 17 configuration.

## Testing Strategy

At minimum, changes should be verified against:

- streak calculations
- tracker creation and editing
- entry add/edit/delete behavior
- backup export/import round-tripping
- reminder scheduling behavior
- CSV export formatting

Current unit coverage includes analytics logic in:

- `app/src/test/java/com/squalor/consecutor/TrackerAnalyticsTest.kt`

## Local Test Runs

Unit tests:

- `./gradlew test`

Instrumented tests, when present and configured:

- `./gradlew connectedAndroidTest`

Manual install to a connected device:

- `./gradlew installDebug`

## Manual QA Checklist

Before merging or shipping a release:

1. Create each tracker type: `YES_NO`, `COUNT`, `MEASURE`.
2. Add entries for today and past dates.
3. Edit and delete entries and confirm summaries update.
4. Archive a tracker and confirm it no longer behaves like an active tracker.
5. Export CSV and inspect the output for correctness.
6. Export a backup, clear app data or reinstall, then import the backup.
7. Enable reminders and verify notification behavior.
8. On Android 13+, confirm notification permission flow.
9. Verify backup import failure handling with an invalid file.

## Release Artifacts

For internal testing or sideloading:

- Debug APK: `app/build/outputs/apk/debug/`
- Release APK: `app/build/outputs/apk/release/`

For Google Play:

- Release App Bundle: `app/build/outputs/bundle/release/`

Important:

- An `.apk` can be installed on devices.
- An `.aab` is the publishing format for Google Play and cannot be installed directly on a device.

Google’s official docs state that new Google Play apps are required to publish with Android App Bundles and that Play App Signing is required for new apps.

Sources:

- https://developer.android.com/appbundle
- https://developer.android.com/guide/publishing/app-signing.html

## Signing For Release

Android requires release artifacts to be signed.

### Generate a Keystore

In Android Studio:

1. `Build` > `Generate Signed Bundle / APK`
2. Choose `Android App Bundle` or `APK`
3. Create a new keystore if needed
4. Save the keystore somewhere secure and back it up

Google’s official guidance recommends using an upload key with Play App Signing for Google Play releases.

### Configure Signing In Gradle

This project does not yet commit release signing config. Add it locally or through CI secrets before shipping.

Typical `app/build.gradle` shape:

```groovy
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("CONSECUTOR_UPLOAD_KEYSTORE"))
            storePassword System.getenv("CONSECUTOR_UPLOAD_STORE_PASSWORD")
            keyAlias System.getenv("CONSECUTOR_UPLOAD_KEY_ALIAS")
            keyPassword System.getenv("CONSECUTOR_UPLOAD_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

Do not hardcode keystore passwords in the repository.

## Building Signed Release Files

Signed APK:

- `./gradlew assembleRelease`

Signed App Bundle:

- `./gradlew bundleRelease`

Before distributing, verify:

- package name
- version code
- version name
- signing config
- release notes
- privacy docs
- screenshots/store metadata

## Google Play Deployment

### What To Upload

For Google Play, upload the signed `.aab` built by `bundleRelease`.

Google Play will generate device-specific APKs from the app bundle. Official Android docs describe `.aab` as the publishing format for Google Play and explain that Play generates APKs for delivery.

### First Release Flow

1. Create a Google Play developer account.
2. Create a new app in Play Console.
3. Opt in to Play App Signing.
4. Upload the signed `.aab`.
5. Complete store listing fields.
6. Complete app content, privacy, and policy declarations.
7. Create a testing track or production release.
8. Roll out the release.

### Play App Signing Notes

Per Android’s official documentation:

- sign the uploaded bundle with your upload key
- Play App Signing manages the app signing key used for distribution
- if you want the same signing key across multiple stores, provide your own signing key when setting up Play App Signing

Official reference:

- https://developer.android.com/guide/publishing/app-signing.html

### Store Listing Preparation

Expect to prepare:

- app name
- short description
- full description
- screenshots
- privacy policy URL
- app icon and feature graphic
- content rating questionnaire
- data safety form

The exact Play Console forms can change, so verify against the current console before submitting.

## F-Droid Deployment

F-Droid is viable for this project because the app is intended to stay free/libre and avoid proprietary service dependencies.

### Basic F-Droid Requirements

- source code must be available
- licensing must be compatible
- builds should be reproducible or at least buildable from source
- app metadata must be supplied

Official F-Droid references:

- https://fdroid.gitlab.io/jekyll-fdroid/docs/
- https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/
- https://fdroid.gitlab.io/jekyll-fdroid/docs/Build_Metadata_Reference/

### Typical F-Droid Submission Path

1. Make sure the app builds cleanly from source.
2. Ensure all dependencies and build steps are compatible with F-Droid policies.
3. Add app metadata in the source repository or via F-Droid metadata files.
4. Submit the app to the F-Droid data repository following their quick-start guide.
5. Respond to review comments or build issues.

### F-Droid-Specific Practical Notes

- Avoid proprietary SDK dependencies.
- Avoid closed-source analytics or crash-reporting libraries.
- Ensure release builds do not depend on secrets that F-Droid cannot access.
- Make sure versioning and tags are clean so F-Droid can track releases.

### What You Will Likely Need

- stable application ID
- signed tags or at least clean version tags
- app description and screenshots
- license clarity
- reproducible release process

## Sideloading And Direct Distribution

If you want to distribute directly:

1. Build a signed release APK.
2. Share the APK through your own site, repo release, or testing channel.
3. Users install the APK manually.

Be aware:

- direct APK distribution does not give you Play-managed delivery
- users must trust your distribution path
- updates are your responsibility

## Deployment Checklist

Before shipping anywhere:

1. Run `./gradlew test`.
2. Run `./gradlew assembleRelease` and `./gradlew bundleRelease`.
3. Verify installability of the release APK on a real device.
4. Verify backup export/import on a real device.
5. Verify reminder behavior on a real device.
6. Verify notification permission flow on Android 13+.
7. Update `README.md`, `PRIVACY.md`, and release notes if behavior changed.
8. Confirm license and dependency compatibility.
9. Tag the release in git.

## Architecture Notes

- Dependency injection is manual on purpose.
- `ConsecutorApp` owns app-wide singletons.
- `TrackerViewModelFactory` wires the `ViewModel`.
- Business logic that may eventually be shared with iOS should stay platform-light where possible.
- Android-only concerns like intents, notifications, and content resolvers should stay at the app/platform boundary.

## Persistence Notes

- Room schema version is currently `2`.
- `fallbackToDestructiveMigration()` is enabled.
- That is acceptable during this rebuild phase, but production releases should replace destructive migration with explicit migrations once the schema stabilizes.

## Privacy Notes

- Automatic Android backup is disabled.
- There is no account system or cloud sync in the MVP.
- Exported files are user-managed and may contain sensitive data.
- The current MVP does not add extra at-rest encryption on top of the local Room database.

## Key Project Files

- `app/src/main/java/com/squalor/consecutor/AppDatabase.kt`
  Room database definition
- `app/src/main/java/com/squalor/consecutor/TrackerModels.kt`
  entities and core app models
- `app/src/main/java/com/squalor/consecutor/TrackerRepository.kt`
  persistence, import/export, and mutation logic
- `app/src/main/java/com/squalor/consecutor/TrackerAnalytics.kt`
  streak and summary calculations
- `app/src/main/java/com/squalor/consecutor/TrackerViewModel.kt`
  app actions and UI-facing state
- `app/src/main/java/com/squalor/consecutor/MainScreen.kt`
  Compose app shell and feature screens
- `app/src/main/java/com/squalor/consecutor/ReminderScheduler.kt`
  local notification scheduling

## License

This repository is licensed under `MPL-2.0`. New dependencies, copied code, and imported snippets should be checked for license compatibility before merging.
