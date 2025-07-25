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
1. First build the shared framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` (for simulator) or `./gradlew :shared:linkDebugFrameworkIosArm64` (for device)
2. Open `iosApp/MountainSpotterApp.xcodeproj` in Xcode
3. Build and run the iOS app (the build script will automatically build the appropriate framework)

**Note:** The Xcode project includes a build script that automatically builds the correct shared framework based on your target (simulator vs device) and configuration (debug vs release).

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
- Builds shared frameworks for all iOS targets (simulator and device)
- Builds iOS app for both simulator and device
- Creates archive for iOS with proper code signing configuration
- Exports IPA when possible (requires proper signing setup)
- Uploads both xcarchive and IPA as artifacts

## Installation

### Android APK Installation
1. Download the APK from GitHub Actions artifacts
2. Enable "Install from unknown sources" on your Android device
3. Install the APK file

### iOS Installation  
iOS builds create an xcarchive that can be used for development and testing. For distribution, proper code signing certificates and provisioning profiles are required. The workflow creates unsigned archives that can be:

1. **Development**: Opened in Xcode for device installation via development provisioning
2. **Simulator**: Built automatically for iOS Simulator testing  
3. **Enterprise/Ad-hoc**: Requires additional signing configuration in the workflow

To enable full IPA creation with signing:
- Add Apple Developer Team ID to Xcode project settings
- Configure proper provisioning profiles
- Add signing certificates to GitHub Actions (for CI/CD)

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