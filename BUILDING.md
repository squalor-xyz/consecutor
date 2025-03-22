# Building and Deploying Consecutor

## Prerequisites

- Android Studio (latest stable version)
- JDK 11
- Git

## Building Locally

1. Clone the repository: git clone https://github.com/squalor-xyz/consecutor.git
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build the app:
    - Via UI: Build > Make Project
    - Via terminal: ./gradlew build
5. Run on a device or emulator:
    - Select a device in Android Studio and click Run

## Automated Builds with GitHub Actions

A GitHub Actions workflow is provided for free CI/CD:

1. Push your code to the GitHub repository
2. The workflow (.github/workflows/build.yml) automatically:
    - Sets up JDK 11
    - Builds the APK
    - Runs unit tests

To create a release:
1. Tag a commit (e.g., git tag v1.0.0)
2. Push the tag: git push origin v1.0.0
3. Manually create a release on GitHub with the built APK

## Deploying to Google Play Store

1. Build a signed APK or App Bundle:
    - Build > Generate Signed Bundle/APK
    - Follow prompts to create or use a signing key
2. Upload to Google Play Console
3. Publish after review

## Alternative: Manual Deployment

- Copy the APK from app/build/outputs/apk/ to your device
- Install via ADB: adb install app-debug.apk