# Building and Deploying Consecutor

## Prerequisites

- **Android Studio**: Latest stable version (e.g., Koala | 2024.1.1 or later).
- **JDK**: Version 11 or higher.
- **Git**: For cloning and managing the repository.

## Setting Up the Development Environment

1. **Install Android Studio**: Download from [developer.android.com/studio](https://developer.android.com/studio).
2. **Install JDK 11**: Ensure it’s available in your PATH or configured in Android Studio.
3. **Clone the Repository**: Run `git clone https://github.com/squalor-xyz/consecutor.git`.
4. **Open the Project**: Launch Android Studio, select “Open an existing project,” and choose the cloned directory.

## Building the App

1. **Sync Gradle**: In Android Studio, go to `File > Sync Project with Gradle Files`.
2. **Build the App**:
   - **UI**: Click `Build > Make Project`.
   - **Terminal**: Run `./gradlew build` in the project root.
3. **Run the App**:
   - Select a device/emulator in Android Studio and click the `Run` button.
   - Alternatively, use `./gradlew installDebug` to install on a connected device.

## Building Different Variants

- **Debug**: `./gradlew assembleDebug` (outputs to `app/build/outputs/apk/debug/`).
- **Release**: `./gradlew assembleRelease` (requires signing; see below).

## Signing the APK

1. **Generate a Signing Key**:
   - In Android Studio: `Build > Generate Signed Bundle/APK > APK > Create New`.
   - Fill in the key details (e.g., alias, password, keystore path).
2. **Configure Signing**:
   - Edit `app/build.gradle`:
     android {
     signingConfigs {
     release {
     storeFile file('path/to/keystore.jks')
     storePassword 'your_store_password'
     keyAlias 'your_key_alias'
     keyPassword 'your_key_password'
     }
     }
     buildTypes {
     release {
     signingConfig signingConfigs.release
     }
     }
     }
3. **Build Signed APK**: Run `./gradlew assembleRelease`.

## Deploying to Google Play Store

1. **Prepare the App**:
   - Update `versionCode` and `versionName` in `app/build.gradle`.
   - Ensure the APK is signed (see above).
2. **Upload to Play Console**:
   - Sign up for a Google Play Developer account at [play.google.com/console](https://play.google.com/console).
   - Create a new app, upload the signed APK, and complete the store listing.
3. **Publish**: Submit for review and publish once approved.

## Automated Builds with GitHub Actions

A workflow is included in `.github/workflows/build.yml`:
- **Triggers**: Runs on push or pull request to the main branch.
- **Steps**: Sets up JDK 11, builds the APK, and runs unit tests.
- **Releasing**: Tag a commit (e.g., `git tag v1.0.0`), push it (`git push origin v1.0.0`), and create a GitHub Release with the APK.

## Manual Deployment (Alternative)

- **Copy APK**: Find the APK in `app/build/outputs/apk/debug/app-debug.apk`.
- **Install via ADB**: Connect a device and run `adb install app-debug.apk`.

## Troubleshooting

- **Gradle Sync Fails**: Ensure JDK 11 is set in `File > Project Structure > SDK Location`.
- **SQLCipher Errors**: Verify native libraries load correctly (check `ConsecutorApp.onCreate`).
- **Emulator Issues**: Use a device with API 21+ and sufficient storage.