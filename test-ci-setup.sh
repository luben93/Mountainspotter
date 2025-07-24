#!/bin/bash

# Test script to verify CI setup works locally

echo "Testing Mountain Spotter CI Setup"
echo "=================================="

# Check if Gradle wrapper exists
if [ -f "./gradlew" ]; then
    echo "✓ Gradle wrapper found"
else
    echo "✗ Gradle wrapper not found"
    exit 1
fi

# Check Android app structure
if [ -d "./androidApp" ]; then
    echo "✓ Android app directory found"
    if [ -f "./androidApp/build.gradle.kts" ]; then
        echo "✓ Android build.gradle.kts found"
    else
        echo "✗ Android build.gradle.kts not found"
    fi
else
    echo "✗ Android app directory not found"
fi

# Check iOS app structure
if [ -d "./iosApp" ]; then
    echo "✓ iOS app directory found"
    if [ -f "./iosApp/MountainSpotter.xcodeproj/project.pbxproj" ]; then
        echo "✓ iOS Xcode project found"
    else
        echo "✗ iOS Xcode project not found"
    fi
    if [ -f "./iosApp/ExportOptions.plist" ]; then
        echo "✓ iOS ExportOptions.plist found"
    else
        echo "✗ iOS ExportOptions.plist not found"
    fi
else
    echo "✗ iOS app directory not found"
fi

# Check shared module
if [ -d "./shared" ]; then
    echo "✓ Shared module found"
    if [ -f "./shared/build.gradle.kts" ]; then
        echo "✓ Shared build.gradle.kts found"
    else
        echo "✗ Shared build.gradle.kts not found"
    fi
else
    echo "✗ Shared module not found"
fi

# Check GitHub Actions workflow
if [ -f "./.github/workflows/build-and-publish.yml" ]; then
    echo "✓ GitHub Actions workflow found"
else
    echo "✗ GitHub Actions workflow not found"
fi

echo ""
echo "Structure validation complete!"
echo ""
echo "Note: Build system requires network access to download dependencies."
echo "In GitHub Actions environment, this should work properly."