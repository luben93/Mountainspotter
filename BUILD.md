# Building Mountain Spotter

This Kotlin Multiplatform Mobile (KMM) project can be built for both Android and iOS platforms.

## Prerequisites

### For Android Development:
- Android Studio with Android SDK installed
- JDK 17 or higher
- Android SDK API level 34 (minimum API level 26)

### For iOS Development:
- macOS with Xcode installed
- JDK 17 or higher

## Local Development

### Android
1. Open the project in Android Studio
2. Let Gradle sync the project
3. Build the Android app: `./gradlew :androidApp:assembleDebug`
4. The APK will be generated at: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### iOS
1. First build the shared framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
2. Open `iosApp/MountainSpotterApp.xcodeproj` in Xcode
3. Build and run the iOS app

## Automated Builds (GitHub Actions)

The project includes GitHub Actions workflows for automated building:

### Main Build Workflow (`/.github/workflows/build.yml`)
- Triggers on pushes to `develop` and `copilot/**` branches
- Triggers on pull requests to `main` and `develop` branches
- Uses the reusable build workflow to build all platforms (Android and iOS)
- Builds debug and release APKs for Android
- Creates iOS archives (when running on macOS)
- Uploads build artifacts for download

### Release Workflow (`/.github/workflows/release.yml`)
- Triggers on pushes to `main` branch
- Uses the same reusable build workflow as the main build
- Creates GitHub releases with versioned APKs
- Automatically tags releases with version numbers

### Reusable Build Workflow (`/.github/workflows/build-reusable.yml`)
- Internal workflow that contains the shared build logic
- Supports both debug and release build types
- Handles Android and iOS builds with proper caching
- Can be called by other workflows to avoid code duplication

### Legacy Workflows
- `android.yml` and `ios.yml` are legacy workflows kept for manual dispatch only
- Use the main `build.yml` workflow for regular development

## Installation

### Android APK Installation
1. Download the APK from GitHub Actions artifacts
2. Enable "Install from unknown sources" on your Android device
3. Install the APK file

### iOS Installation  
iOS builds require proper provisioning profiles and signing certificates for installation on devices. The workflow creates development builds that can be tested in the iOS Simulator.

## Project Structure

- `shared/` - Kotlin Multiplatform shared code with business logic
- `shared-core/` - Core utilities (JVM only)
- `androidApp/` - Android-specific UI using Jetpack Compose
- `iosApp/` - iOS-specific UI using SwiftUI
- `.github/workflows/` - CI/CD automation for building APK and iOS archives

## Features

The Mountain Spotter app uses:
- GPS location services
- Device compass/magnetometer
- Mountain peak database
- Visibility calculations based on elevation and distance
- Real-time peak identification