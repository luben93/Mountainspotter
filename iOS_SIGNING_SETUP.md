# iOS Code Signing Setup

This document explains how to configure code signing for the Mountain Spotter iOS app to enable proper IPA creation and distribution.

## Current Configuration

The project is currently configured for **unsigned builds** suitable for:
- Development and testing
- CI/CD without certificates
- Simulator builds
- Archive creation without distribution

## Enabling Code Signing for Distribution

### 1. Apple Developer Account Setup

1. Enroll in the Apple Developer Program
2. Create an App ID in the Apple Developer Console:
   - Bundle ID: `com.mountainspotter.ios` (or your preferred identifier)
   - Enable required capabilities: Location Services

### 2. Xcode Project Configuration

Update the following in `iosApp/MountainSpotterApp.xcodeproj/project.pbxproj`:

```
DEVELOPMENT_TEAM = "YOUR_TEAM_ID"; // Replace with your Apple Developer Team ID
CODE_SIGNING_REQUIRED = YES;       // Enable signing requirement
```

### 3. Provisioning Profiles

Create provisioning profiles in Apple Developer Console:
- **Development**: For testing on devices
- **Ad Hoc**: For limited distribution testing  
- **App Store**: For App Store distribution

### 4. GitHub Actions Secrets (for CI/CD)

Add these secrets to your GitHub repository:

```
APPLE_CERTIFICATE_P12: Base64-encoded P12 certificate
APPLE_CERTIFICATE_PASSWORD: Password for P12 certificate
APPLE_PROVISIONING_PROFILE: Base64-encoded provisioning profile
APPLE_TEAM_ID: Your Apple Developer Team ID
```

### 5. Update ExportOptions.plist

For different distribution methods:

**App Store Distribution:**
```xml
<key>method</key>
<string>app-store</string>
<key>teamID</key>
<string>YOUR_TEAM_ID</string>
```

**Ad Hoc Distribution:**
```xml
<key>method</key>
<string>ad-hoc</string>
<key>teamID</key>
<string>YOUR_TEAM_ID</string>
```

## Testing the Setup

1. **Local Development**: Open the Xcode project and build for device
2. **CI/CD**: Push changes to trigger the iOS workflow
3. **Archive Verification**: Check that `.xcarchive` and `.ipa` files are created

## Troubleshooting

### Common Issues

1. **"No profiles for team matching..."**
   - Ensure provisioning profiles are installed
   - Check Team ID matches

2. **"Code signing is required for product type 'Application'"**
   - Verify DEVELOPMENT_TEAM is set
   - Check certificate is valid

3. **Framework not found errors**
   - The build script automatically handles framework building
   - Ensure Gradle tasks complete successfully

### Debug Commands

```bash
# Check available signing identities
security find-identity -v -p codesigning

# Verify provisioning profiles
ls ~/Library/MobileDevice/Provisioning\ Profiles/

# Test framework building
./gradlew :shared:linkReleaseFrameworkIosArm64
```

## Current Workflow Capabilities

Without signing certificates, the workflow will:
- ✅ Build shared Kotlin frameworks
- ✅ Compile iOS app
- ✅ Create .xcarchive
- ❌ Export signed .ipa (requires certificates)

With proper signing setup:
- ✅ All of the above
- ✅ Export signed .ipa
- ✅ Ready for distribution/App Store upload