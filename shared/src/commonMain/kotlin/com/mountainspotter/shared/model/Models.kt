package com.mountainspotter.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null
)

@Serializable
data class MountainPeak(
    val id: String,
    val name: String,
    val location: Location,
    val elevation: Double,
    val prominence: Double? = null,
    val country: String? = null,
    val region: String? = null,
    val imageUrl: String? = null
)

data class CompassData(
    val azimuth: Float, // Degrees from North (0-360)
    val pitch: Float,   // Tilt up/down in degrees
    val roll: Float     // Side-to-side tilt in degrees
)

data class VisiblePeak(
    val peak: MountainPeak,
    val distance: Double,      // Distance in kilometers
    val bearing: Double,       // Bearing from user location in degrees
    val elevationAngle: Double, // Angle above horizon in degrees
    val isVisible: Boolean     // Whether peak is above horizon and not blocked
)
