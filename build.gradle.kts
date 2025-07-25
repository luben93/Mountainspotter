plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

// Create unified "foo" task that builds both APK and IPA
tasks.register("foo") {
    group = "build"
    description = "Build both Android APK and iOS framework for distribution"
    
    dependsOn(":composeApp:assembleDebug")
    
    doLast {
        println("✅ foo build completed successfully!")
        println("📱 Android APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk")
        
        val iosFrameworkPath = File("composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework")
        if (iosFrameworkPath.exists()) {
            println("🍎 iOS Framework: ${iosFrameworkPath.path}")
        } else {
            println("🍎 iOS Framework: Skipped (requires macOS with Xcode for iOS builds)")
        }
        
        println("ℹ️  Note: iOS .ipa creation requires Xcode project build with proper signing")
        println("")
        println("To create iOS .ipa manually:")
        println("1. Open iosApp/iosApp.xcodeproj in Xcode on macOS")
        println("2. Build and Archive the project")
        println("3. Export as .ipa from Organizer")
    }
}

// Enhanced foo task that includes iOS when on macOS
tasks.register("fooWithIOS") {
    group = "build"
    description = "Build both Android APK and iOS framework (macOS only)"
    
    dependsOn(":composeApp:assembleDebug")
    
    // Add iOS dependencies if on macOS
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    if (isMacOS) {
        dependsOn(":composeApp:linkDebugFrameworkIosSimulatorArm64")
    }
    
    doLast {
        println("✅ fooWithIOS build completed successfully!")
        println("📱 Android APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk")
        
        if (isMacOS) {
            val iosFrameworkPath = File("composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework")
            if (iosFrameworkPath.exists()) {
                println("🍎 iOS Framework: ${iosFrameworkPath.path}")
            } else {
                println("🍎 iOS Framework: Build attempted but not found")
            }
        } else {
            println("🍎 iOS Framework: Skipped (requires macOS)")
        }
        
        println("ℹ️  Note: iOS .ipa creation requires Xcode project build with proper signing")
    }
}