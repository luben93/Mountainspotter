# CI/CD Setup for Mountain Spotter

This document explains the Continuous Integration and Continuous Deployment setup for building and publishing both Android APK and iOS IPA files.

## 🚀 GitHub Actions Workflows

### Main Build and Publish Workflow
**File:** `.github/workflows/build-and-publish.yml`

This workflow automatically:
- Builds Android APK (debug and release) on every push/PR
- Builds iOS IPA on macOS runners
- Uploads both as artifacts for download
- Creates GitHub releases when tags are pushed

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` branch
- Git tags (for releases)

### Diagnostics Workflow
**File:** `.github/workflows/ci-test.yml`

This workflow helps debug build issues by:
- Testing project structure
- Checking network connectivity
- Validating Gradle configuration
- Testing dependency resolution

## 📱 Build Outputs

### Android APK
- **Debug APK:** Built for testing and development
- **Release APK:** Production-ready build (unsigned)
- **Location:** `androidApp/build/outputs/apk/`

### iOS IPA
- **Framework:** Kotlin Multiplatform shared framework
- **IPA:** iOS application package
- **Location:** `iosApp/` (after building)

## 🛠️ Local Development

### Build Scripts

#### `build-local.sh`
Comprehensive local build script that:
- Builds Android APK (debug & release)
- Builds iOS frameworks for all architectures
- Creates iOS IPA (on macOS with Xcode)
- Organizes outputs in `build-outputs/` directory

```bash
./build-local.sh
```

#### `test-ci-setup.sh`
Validates project structure for CI compatibility:
- Checks for required files and directories
- Validates Gradle and Xcode project setup
- Confirms GitHub Actions workflow presence

```bash
./test-ci-setup.sh
```

## 🔧 Project Structure

### iOS Project
The iOS app includes:
- **Xcode Project:** `iosApp/MountainSpotter.xcodeproj`
- **App Icons:** `iosApp/Assets.xcassets/AppIcon.appiconset/`
- **Export Configuration:** `iosApp/ExportOptions.plist`
- **Swift Source Files:** `iosApp/*.swift`

### Android Project
The Android app includes:
- **Gradle Build:** `androidApp/build.gradle.kts`
- **Source Code:** `androidApp/src/`
- **Resources:** `androidApp/src/main/res/`

### Shared Module
Kotlin Multiplatform shared code:
- **Common Code:** `shared/src/commonMain/`
- **Android Specific:** `shared/src/androidMain/`
- **iOS Specific:** `shared/src/iosMain/`

## 🔐 Code Signing

### Android
- Debug APK uses debug keystore (no additional setup needed)
- Release APK is unsigned (requires signing for distribution)

### iOS
- CI builds use automatic signing with no development team
- For distribution, you'll need:
  - Apple Developer Account
  - Provisioning profiles
  - Code signing certificates

## 📦 Artifacts and Releases

### GitHub Actions Artifacts
Each workflow run produces downloadable artifacts:
- `android-apk-debug`: Debug Android APK
- `android-apk-release`: Release Android APK  
- `ios-ipa`: iOS application and archive

### GitHub Releases
When you push a git tag, the workflow creates a GitHub release with:
- Release Android APK
- iOS IPA (if build successful)
- Release notes with installation instructions

### Creating a Release
```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

## 🐛 Troubleshooting

### Common Issues

1. **Android Build Fails**
   - Check Android SDK version compatibility
   - Verify Gradle wrapper version
   - Run diagnostics workflow for details

2. **iOS Build Fails**
   - Ensure Xcode version compatibility
   - Check framework linking in Xcode project
   - Verify code signing settings

3. **Network Issues**
   - GitHub Actions should have proper internet access
   - Check dependency resolution in diagnostics workflow

### Debug Steps
1. Run `./test-ci-setup.sh` to validate structure
2. Check the diagnostics workflow results
3. Review GitHub Actions logs for detailed error messages
4. Test locally with `./build-local.sh` (requires network access)

## 📝 Customization

### Adding Code Signing
To add proper code signing for release builds:

1. **Android:** Add signing configuration to `androidApp/build.gradle.kts`
2. **iOS:** Configure development team and provisioning profiles

### Modifying Build Configuration
- **Android:** Edit `androidApp/build.gradle.kts`
- **iOS:** Modify `iosApp/MountainSpotter.xcodeproj`
- **Shared:** Update `shared/build.gradle.kts`

### Custom Build Steps
Add additional steps to `.github/workflows/build-and-publish.yml`:
- Code quality checks
- Testing
- Security scanning
- Additional deployment targets

## 📋 Requirements

### GitHub Actions Environment
- Ubuntu runner (for Android builds)
- macOS runner (for iOS builds)
- JDK 17
- Android SDK
- Xcode (latest stable)

### Local Development
- JDK 17+
- Android SDK
- Gradle 8.4+
- Xcode (for iOS builds, macOS only)

This CI/CD setup provides a complete solution for building and distributing both Android and iOS versions of the Mountain Spotter app.