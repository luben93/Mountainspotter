plugins {
    kotlin("multiplatform") version "1.9.10" apply false
    kotlin("android") version "1.9.10" apply false
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
    id("org.jetbrains.compose") version "1.5.4" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
