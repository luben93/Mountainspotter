package com.mountainspotter.shared.platform

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.CompassData
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific location services
 */
expect class LocationService {
    fun startLocationUpdates(): Flow<Location?>
    fun stopLocationUpdates()
    suspend fun getCurrentLocation(): Location?
    fun isLocationPermissionGranted(): Boolean
    suspend fun requestLocationPermission(): Boolean
}

/**
 * Platform-specific compass/sensor services
 */
expect class CompassService {
    fun startCompassUpdates(): Flow<CompassData>
    fun stopCompassUpdates()
    fun isCompassAvailable(): Boolean
}

/**
 * Platform-specific permission handling
 */
expect class PermissionManager {
    suspend fun requestLocationPermission(): Boolean
    fun isLocationPermissionGranted(): Boolean
    fun openAppSettings()
}
