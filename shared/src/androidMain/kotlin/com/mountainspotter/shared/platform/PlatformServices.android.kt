package com.mountainspotter.shared.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.CompassData
import android.annotation.SuppressLint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.sqrt

actual class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var locationCallback: LocationCallback? = null
    
    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(): Flow<Location?> = callbackFlow {
        if (!isLocationPermissionGranted()) {
            trySend(null)
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 5 seconds
        ).build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = if (location.hasAltitude()) location.altitude else null
                        )
                    )
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                null
            )
        } catch (e: SecurityException) {
            trySend(null)
        }
        
        awaitClose {
            stopLocationUpdates()
        }
    }
    
    actual fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }
    
    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!isLocationPermissionGranted()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = if (location.hasAltitude()) location.altitude else null
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }
    
    actual fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    actual suspend fun requestLocationPermission(): Boolean {
        // This will be handled by the activity/compose permission handling
        return isLocationPermissionGranted()
    }
}

actual class CompassService(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    private var sensorEventListener: SensorEventListener? = null
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    
    // Compass smoothing variables
    private var lastAzimuth: Float = 0f
    private var lastUpdateTime: Long = 0
    private val smoothingFactor = 0.1f // Lower values = more smoothing
    private val minUpdateInterval = 100L // Minimum 100ms between updates
    
    actual fun startCompassUpdates(): Flow<CompassData> = callbackFlow {
        if (!isCompassAvailable()) {
            return@callbackFlow
        }
        
        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        // Low-pass filter
                        val alpha = 0.8f
                        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        geomagnetic[0] = event.values[0]
                        geomagnetic[1] = event.values[1]
                        geomagnetic[2] = event.values[2]
                    }
                }
                
                val R = FloatArray(9)
                val I = FloatArray(9)
                
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    
                    val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    
                    val currentTime = System.currentTimeMillis()
                    
                    // Apply time-based throttling and smoothing
                    if (currentTime - lastUpdateTime >= minUpdateInterval) {
                        val normalizedAzimuth = if (rawAzimuth < 0) rawAzimuth + 360f else rawAzimuth
                        
                        // Apply exponential smoothing to reduce jitter
                        val smoothedAzimuth = if (lastUpdateTime == 0L) {
                            normalizedAzimuth
                        } else {
                            smoothCompassValue(lastAzimuth, normalizedAzimuth, smoothingFactor)
                        }
                        
                        lastAzimuth = smoothedAzimuth
                        lastUpdateTime = currentTime
                        
                        trySend(
                            CompassData(
                                azimuth = smoothedAzimuth,
                                pitch = pitch,
                                roll = roll
                            )
                        )
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        accelerometer?.let { 
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let { 
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        
        awaitClose {
            stopCompassUpdates()
        }
    }
    
    actual fun stopCompassUpdates() {
        sensorEventListener?.let { listener ->
            sensorManager.unregisterListener(listener)
            sensorEventListener = null
        }
    }
    
    actual fun isCompassAvailable(): Boolean {
        return accelerometer != null && magnetometer != null
    }
    
    /**
     * Smooth compass values to reduce jitter, handling the circular nature of compass readings
     */
    private fun smoothCompassValue(lastValue: Float, newValue: Float, factor: Float): Float {
        var diff = newValue - lastValue
        
        // Handle wrapping around 0/360 degrees
        if (diff > 180f) {
            diff -= 360f
        } else if (diff < -180f) {
            diff += 360f
        }
        
        val smoothed = lastValue + factor * diff
        return when {
            smoothed < 0f -> smoothed + 360f
            smoothed >= 360f -> smoothed - 360f
            else -> smoothed
        }
    }
}

actual class PermissionManager(private val context: Context) {
    actual suspend fun requestLocationPermission(): Boolean {
        return isLocationPermissionGranted()
    }
    
    actual fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    actual fun openAppSettings() {
        // This would typically open the app settings
        // Implementation would depend on the activity context
    }
}
