rootProject.name = "Mountainspotter"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        // Add JetBrains Compose repository for better dependency resolution
        maven("https://maven.pkg.jetbrains.space/public/packages/compose/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Add JetBrains Compose repository
        maven("https://maven.pkg.jetbrains.space/public/packages/compose/p/compose/dev")
    }
}

include(":shared")
include(":shared-core")
include(":androidApp")
include(":iosApp")
