#!/bin/bash

# Local build script for Mountain Spotter
# This script builds both Android APK and iOS frameworks locally

set -e  # Exit on any error

echo "🏔️  Mountain Spotter Local Build Script"
echo "======================================="

# Create build output directory
mkdir -p build-outputs/android
mkdir -p build-outputs/ios

echo ""
echo "📱 Building Android APK..."
echo "--------------------------"

# Build Android APK
./gradlew :androidApp:assembleDebug --stacktrace
./gradlew :androidApp:assembleRelease --stacktrace

# Copy APK files
if [ -f "androidApp/build/outputs/apk/debug/androidApp-debug.apk" ]; then
    cp androidApp/build/outputs/apk/debug/androidApp-debug.apk build-outputs/android/
    echo "✅ Debug APK created: build-outputs/android/androidApp-debug.apk"
fi

if [ -f "androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk" ]; then
    cp androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk build-outputs/android/
    echo "✅ Release APK created: build-outputs/android/androidApp-release-unsigned.apk"
fi

echo ""
echo "🍎 Building iOS Framework..."
echo "----------------------------"

# Build iOS frameworks
./gradlew :shared:linkDebugFrameworkIosX64 --stacktrace
./gradlew :shared:linkReleaseFrameworkIosX64 --stacktrace
./gradlew :shared:linkDebugFrameworkIosArm64 --stacktrace
./gradlew :shared:linkReleaseFrameworkIosArm64 --stacktrace

# Copy framework files
if [ -d "shared/build/bin/iosX64/debugFramework/shared.framework" ]; then
    cp -r shared/build/bin/iosX64/debugFramework/shared.framework build-outputs/ios/shared-debug-x64.framework
    echo "✅ iOS x64 Debug Framework created"
fi

if [ -d "shared/build/bin/iosArm64/releaseFramework/shared.framework" ]; then
    cp -r shared/build/bin/iosArm64/releaseFramework/shared.framework build-outputs/ios/shared-release-arm64.framework
    echo "✅ iOS ARM64 Release Framework created"
fi

echo ""
echo "📱 Building iOS App (if Xcode is available)..."
echo "----------------------------------------------"

# Check if we're on macOS and have Xcode
if [[ "$OSTYPE" == "darwin"* ]] && command -v xcodebuild &> /dev/null; then
    cd iosApp
    
    # Build iOS app
    xcodebuild -project MountainSpotter.xcodeproj -scheme MountainSpotter -configuration Release -destination 'generic/platform=iOS' -archivePath MountainSpotter.xcarchive archive DEVELOPMENT_TEAM="" CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO
    
    # Create IPA (if successful)
    if [ -d "MountainSpotter.xcarchive" ]; then
        xcodebuild -exportArchive -archivePath MountainSpotter.xcarchive -exportPath ../build-outputs/ios/ -exportOptionsPlist ExportOptions.plist
        echo "✅ iOS IPA created in build-outputs/ios/"
    fi
    
    cd ..
else
    echo "⚠️  iOS app build skipped (Xcode not available or not on macOS)"
    echo "   Frameworks are built and can be used in Xcode manually"
fi

echo ""
echo "🎉 Build Complete!"
echo "=================="
echo "Output files are in the build-outputs/ directory:"
ls -la build-outputs/android/ 2>/dev/null || echo "No Android outputs"
ls -la build-outputs/ios/ 2>/dev/null || echo "No iOS outputs"

echo ""
echo "📝 Next Steps:"
echo "- For Android: Install the APK on your device"
echo "- For iOS: Open iosApp/MountainSpotter.xcodeproj in Xcode and build"