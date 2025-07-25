@file:OptIn(ExperimentalForeignApi::class)

package com.mountainspotter.shared.platform

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.CompassData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.CoreMotion.*
import kotlinx.cinterop.*
import platform.Foundation.NSOperationQueue.Companion.mainQueue
import platform.Foundation.NSURL
import platform.darwin.NSObject
import kotlin.math.*
import platform.CoreLocation.CLLocationCoordinate2D // Ensure this is present

actual class LocationService {
    private val locationManager = CLLocationManager()
    private var locationDelegate: LocationDelegate? = null
    
    actual fun startLocationUpdates(): Flow<Location?> = callbackFlow {
        locationDelegate = LocationDelegate { location ->
            trySend(location)
        }
        
        locationManager.delegate = locationDelegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10.0 // 10 meters
        
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.startUpdatingLocation()
            }
            else -> {
                trySend(null)
            }
        }
        
        awaitClose {
            stopLocationUpdates()
        }
    }
    
    actual fun stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
        locationDelegate = null
    }
    
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): Location? {
        return locationManager.location?.let { clLocation ->
            Location(
                latitude = clLocation.coordinate.useContents { latitude },
                longitude = clLocation.coordinate.useContents { longitude },
                altitude = if (clLocation.altitude != -1.0) clLocation.altitude else null
            )
        }
    }
    
    actual fun isLocationPermissionGranted(): Boolean {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> true
            else -> false
        }
    }
    
    actual suspend fun requestLocationPermission(): Boolean {
        locationManager.requestWhenInUseAuthorization()
        return isLocationPermissionGranted()
    }
    
    private class LocationDelegate(
        private val onLocationUpdate: (Location?) -> Unit
    ) : NSObject(), CLLocationManagerDelegateProtocol {
        
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val locations = didUpdateLocations as List<CLLocation>
            locations.lastOrNull()?.let { location ->
                onLocationUpdate(
                    Location(
                        latitude = location.coordinate.useContents { latitude },
                        longitude = location.coordinate.useContents { longitude },
                        altitude = if (location.altitude != -1.0) location.altitude else null
                    )
                )
            }
        }
        
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            onLocationUpdate(null)
        }
    }
}

actual class CompassService {
    private val motionManager = CMMotionManager()
    
    actual fun startCompassUpdates(): Flow<CompassData> = callbackFlow {
        if (!isCompassAvailable()) {
            return@callbackFlow
        }
        
        motionManager.deviceMotionUpdateInterval = 0.1 // 10 Hz
        
        motionManager.startDeviceMotionUpdatesToQueue(
            queue = mainQueue // Use main queue
        ) { motion, error ->
            motion?.let { deviceMotion ->
                val attitude = deviceMotion.attitude

                val azimuth = (-attitude.yaw * 180.0 / PI).toFloat()
                val pitch = (attitude.pitch * 180.0 / PI).toFloat()
                val roll = (attitude.roll * 180.0 / PI).toFloat()

                trySend(
                    CompassData(
                        azimuth = if (azimuth < 0) azimuth + 360f else azimuth,
                        pitch = pitch,
                        roll = roll
                    )
                )

                // Convert from radians to degrees
//                val azimuth = Math.toDegrees(-attitude.yaw).toFloat()
//                val pitch = Math.toDegrees(attitude.pitch).toFloat()
//                val roll = Math.toDegrees(attitude.roll).toFloat()
//
//                trySend(
//                    CompassData(
//                        azimuth = if (azimuth < 0) azimuth + 360f else azimuth,
//                        pitch = pitch,
//                        roll = roll
//                    )
//                )
            }
        }
        
        awaitClose {
            stopCompassUpdates()
        }
    }
    
    actual fun stopCompassUpdates() {
        motionManager.stopDeviceMotionUpdates()
    }
    
    actual fun isCompassAvailable(): Boolean {
        return motionManager.deviceMotionAvailable
    }
}

actual class PermissionManager {
    actual suspend fun requestLocationPermission(): Boolean {
        val locationManager = CLLocationManager()
        locationManager.requestWhenInUseAuthorization()
        return isLocationPermissionGranted()
    }
    
    actual fun isLocationPermissionGranted(): Boolean {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> true
            else -> false
        }
    }
    
    actual fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        settingsUrl?.let { url ->
            UIApplication.sharedApplication.openURL(url)
        }
    }
}
