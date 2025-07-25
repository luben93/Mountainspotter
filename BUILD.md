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

### Unified Build (foo)
**NEW:** Build both Android APK and iOS framework with a single command:
```bash
./gradlew foo
```
This will produce:
- Android APK at: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- iOS framework (if on macOS): `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework`

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

### Android Build (`/.github/workflows/android.yml`)
- Triggers on pushes to `main`, `develop`, and `copilot/**` branches
- Sets up Android SDK automatically
- Builds debug APK
- Uploads APK as artifact for download

### iOS Build (`/.github/workflows/ios.yml`)
- Triggers on pushes to `main`, `develop`, and `copilot/**` branches  
- Sets up Xcode on macOS runner
- Builds shared framework and iOS app
- Attempts to create archive for iOS

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