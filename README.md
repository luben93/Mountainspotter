# Mountain Spotter

A Kotlin Multiplatform Mobile app that uses GPS, compass, and elevation data to identify visible mountain peaks.

## Features

- **GPS Location**: Uses device GPS to determine your current position and altitude
- **Compass Integration**: Combines device orientation with peak bearing calculations  
- **Peak Visibility**: Calculates which mountain peaks are visible from your location
- **Mountain Database**: Contains information about major mountain peaks worldwide
- **Cross-Platform**: Built with Kotlin Multiplatform for both Android and iOS

## Architecture

- **Shared Module**: Core business logic, calculations, and data models
- **Android App**: Native Android UI with Jetpack Compose
- **iOS App**: Native iOS UI with SwiftUI

## Key Components

### Shared Logic (shared/)
- `MountainCalculationService`: Handles distance, bearing, and visibility calculations
- `MountainRepository`: Manages mountain peak data
- `MountainSpotterViewModel`: Main app state management
- Platform abstractions for GPS and compass sensors

### Platform Implementations
- **Android**: Uses Google Play Services for location, device sensors for compass
- **iOS**: Uses Core Location and Core Motion frameworks

## Setup

### Prerequisites
- Android Studio or Xcode for mobile development
- JDK 11 or higher
- Android SDK (for Android builds)
- Xcode (for iOS builds, Mac only)

### Building
```bash
./gradlew build
```

### Running
- **Android**: Use Android Studio or `./gradlew :androidApp:installDebug`
- **iOS**: Open iosApp in Xcode

## Technical Details

- **Calculations**: Uses Haversine formula for distance, accounts for Earth curvature
- **Sensors**: Integrates GPS altitude with compass bearing for 3D positioning
- **Performance**: Efficient peak filtering based on distance and visibility

The app provides real-time identification of mountain peaks visible from your current location, making it perfect for hiking, mountaineering, and outdoor exploration.