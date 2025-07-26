@file:OptIn(ExperimentalForeignApi::class)

package com.mountainspotter.shared.platform

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.CompassData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

// Unified sensor manager that handles both location and compass
object UnifiedSensorManager {
    private val locationManager = CLLocationManager()
    private val motionManager = CMMotionManager()
    private var delegate: UnifiedSensorDelegate? = null

    private val _locationFlow = MutableSharedFlow<Location?>(replay = 1)
    val locationFlow = _locationFlow.asSharedFlow()

    private val _compassFlow = MutableSharedFlow<CompassData>(replay = 1)
    val compassFlow = _compassFlow.asSharedFlow()

    private var isStarted = false

    fun startSensors() {
        if (isStarted) return

        println("UnifiedSensorManager: Starting sensors")

        delegate = UnifiedSensorDelegate(
            onLocationUpdate = { location ->
                println("UnifiedSensorManager: Location update: $location")
                _locationFlow.tryEmit(location)
            },
            onCompassUpdate = { compassData ->
                println("UnifiedSensorManager: Compass update: $compassData")
                _compassFlow.tryEmit(compassData)
            }
        )

        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 1.0
        locationManager.headingFilter = 1.0

        val authStatus = CLLocationManager.authorizationStatus()
        println("UnifiedSensorManager: Auth status: $authStatus")

        when (authStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                println("UnifiedSensorManager: Permission granted, starting updates")
                locationManager.startUpdatingLocation()
                if (CLLocationManager.headingAvailable()) {
                    locationManager.startUpdatingHeading()
                }
                startMotionUpdates()
            }
            kCLAuthorizationStatusNotDetermined -> {
                println("UnifiedSensorManager: Requesting permission")
                locationManager.requestWhenInUseAuthorization()
            }
            else -> {
                println("UnifiedSensorManager: Permission denied")
                _locationFlow.tryEmit(null)
            }
        }
        
        isStarted = true
    }

    private fun startMotionUpdates() {
        if (motionManager.deviceMotionAvailable) {
            motionManager.deviceMotionUpdateInterval = 0.1
            motionManager.startDeviceMotionUpdatesToQueue(mainQueue) { motion, error ->
                if (error != null) {
                    println("UnifiedSensorManager: Motion error: ${error.localizedDescription}")
                    return@startDeviceMotionUpdatesToQueue
                }

                motion?.let { deviceMotion ->
                    val attitude = deviceMotion.attitude
                    val pitch = (attitude.pitch * 180.0 / PI).toFloat()
                    val roll = (attitude.roll * 180.0 / PI).toFloat()

                    delegate?.updateMotionData(pitch, roll)
                }
            }
        }
    }
    
    fun stopSensors() {
        if (!isStarted) return

        println("UnifiedSensorManager: Stopping sensors")
        locationManager.stopUpdatingLocation()
        locationManager.stopUpdatingHeading()
        motionManager.stopDeviceMotionUpdates()
        locationManager.delegate = null
        delegate = null
        isStarted = false
    }
    
    fun isLocationPermissionGranted(): Boolean {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> true
            else -> false
        }
    }
}

private class UnifiedSensorDelegate(
    private val onLocationUpdate: (Location?) -> Unit,
    private val onCompassUpdate: (CompassData) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    private var lastPitch: Float = 0f
    private var lastRoll: Float = 0f

    fun updateMotionData(pitch: Float, roll: Float) {
        lastPitch = pitch
        lastRoll = roll
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        println("UnifiedSensorDelegate: didUpdateLocations called")
        val locations = didUpdateLocations as List<CLLocation>
        locations.lastOrNull()?.let { clLocation ->
            val location = Location(
                latitude = clLocation.coordinate.useContents { latitude },
                longitude = clLocation.coordinate.useContents { longitude },
                altitude = if (clLocation.altitude > -1000.0) clLocation.altitude else null
            )
            println("UnifiedSensorDelegate: Location: lat=${location.latitude}, lon=${location.longitude}, alt=${location.altitude}")
            onLocationUpdate(location)
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
        val azimuth = didUpdateHeading.trueHeading.toFloat()
        println("UnifiedSensorDelegate: Heading: $azimuth degrees")

        if (azimuth >= 0) {
            val compassData = CompassData(
                azimuth = azimuth,
                pitch = lastPitch,
                roll = lastRoll
            )
            println("UnifiedSensorDelegate: Compass: azimuth=$azimuth, pitch=$lastPitch, roll=$lastRoll")
            onCompassUpdate(compassData)
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        println("UnifiedSensorDelegate: Error: ${didFailWithError.localizedDescription}")
        onLocationUpdate(null)
    }

    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        println("UnifiedSensorDelegate: Auth status changed to: $didChangeAuthorizationStatus")
        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                println("UnifiedSensorDelegate: Permission granted, starting all updates")
                manager.startUpdatingLocation()
                if (CLLocationManager.headingAvailable()) {
                    manager.startUpdatingHeading()
                }
                // Call motion updates through the manager instance
                if (CMMotionManager().deviceMotionAvailable) {
                    println("UnifiedSensorDelegate: Starting motion updates after permission grant")
                }
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                println("UnifiedSensorDelegate: Permission denied")
                onLocationUpdate(null)
            }
            else -> {
                println("UnifiedSensorDelegate: Auth status: $didChangeAuthorizationStatus")
            }
        }
    }
}

actual class LocationService {
    actual fun startLocationUpdates(): Flow<Location?> = callbackFlow {
        println("LocationService: Starting location updates")

        // Start the unified sensor manager
        UnifiedSensorManager.startSensors()

        // Use launchIn instead of collect to avoid blocking
        val job = UnifiedSensorManager.locationFlow
            .onEach { location ->
                println("LocationService: Emitting location: $location")
                trySend(location)
            }
            .launchIn(this)

        awaitClose {
            println("LocationService: Stopping location updates")
            job.cancel()
        }
    }

    actual fun stopLocationUpdates() {
        println("LocationService: Stop called")
    }

    actual suspend fun getCurrentLocation(): Location? {
        return null
    }

    actual fun isLocationPermissionGranted(): Boolean {
        return UnifiedSensorManager.isLocationPermissionGranted()
    }

    actual suspend fun requestLocationPermission(): Boolean {
        println("LocationService: Requesting permission")
        UnifiedSensorManager.startSensors()
        return isLocationPermissionGranted()
    }
}

actual class CompassService {
    actual fun startCompassUpdates(): Flow<CompassData> = callbackFlow {
        println("CompassService: Starting compass updates")

        if (!isCompassAvailable()) {
            println("CompassService: Compass not available")
            close()
            return@callbackFlow
        }
        
        // Start the unified sensor manager
        UnifiedSensorManager.startSensors()

        // Use launchIn instead of collect to avoid blocking
        val job = UnifiedSensorManager.compassFlow
            .onEach { compassData ->
                println("CompassService: Emitting compass data: $compassData")
                trySend(compassData)
            }
            .launchIn(this)

        awaitClose {
            println("CompassService: Stopping compass updates")
            job.cancel()
        }
    }
    
    actual fun stopCompassUpdates() {
        println("CompassService: Stop called")
    }
    
    actual fun isCompassAvailable(): Boolean {
        val available = CLLocationManager.headingAvailable() &&
                       CMMotionManager().deviceMotionAvailable
        println("CompassService: Available: $available")
        return available
    }
}

actual class PermissionManager {
    actual suspend fun requestLocationPermission(): Boolean {
        println("PermissionManager: Requesting permission")
        UnifiedSensorManager.startSensors()
        return isLocationPermissionGranted()
    }
    
    actual fun isLocationPermissionGranted(): Boolean {
        return UnifiedSensorManager.isLocationPermissionGranted()
    }
    
    actual fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        settingsUrl?.let { url ->
            UIApplication.sharedApplication.openURL(url)
        }
    }
}
