package com.mountainspotter.shared.service

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.MountainPeak
import com.mountainspotter.shared.model.VisiblePeak
import kotlin.math.*

class MountainCalculationService {
    
    /**
     * Calculate distance between two locations using Haversine formula
     */
    fun calculateDistance(from: Location, to: Location): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val lat1Rad = toRadians(from.latitude)
        val lat2Rad = toRadians(to.latitude)
        val deltaLatRad = toRadians(to.latitude - from.latitude)
        val deltaLonRad = toRadians(to.longitude - from.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Calculate bearing from one location to another
     */
    fun calculateBearing(from: Location, to: Location): Double {
        val lat1Rad = toRadians(from.latitude)
        val lat2Rad = toRadians(to.latitude)
        val deltaLonRad = toRadians(to.longitude - from.longitude)
        
        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        
        val bearingRad = atan2(y, x)
        return (toDegrees(bearingRad) + 360) % 360
    }
    
    /**
     * Calculate elevation angle to a mountain peak
     */
    fun calculateElevationAngle(
        userLocation: Location,
        peak: MountainPeak,
        distance: Double
    ): Double {
        val userAltitude = userLocation.altitude ?: 0.0
        val heightDifference = peak.elevation - userAltitude
        
        // Account for Earth's curvature
        val earthRadius = 6371000.0 // meters
        val distanceMeters = distance * 1000
        val curvatureCorrection = (distanceMeters * distanceMeters) / (2 * earthRadius)
        val adjustedHeightDifference = heightDifference - curvatureCorrection
        
        return toDegrees(atan(adjustedHeightDifference / (distanceMeters)))
    }
    
    /**
     * Determine if a peak is visible based on line of sight
     */
    fun isPeakVisible(
        userLocation: Location,
        peak: MountainPeak,
        elevationAngle: Double,
        maxDistance: Double = 200.0 // km
    ): Boolean {
        val distance = calculateDistance(userLocation, peak.location)
        
        // Too far away
        if (distance > maxDistance) return false
        
        // Below horizon (accounting for refraction ~0.1 degrees)
        if (elevationAngle < -0.1) return false
        
        // Basic visibility check - in a real app you'd want terrain analysis
        return true
    }
    
    /**
     * Calculate all visible peaks from user location
     */
    fun calculateVisiblePeaks(
        userLocation: Location,
        allPeaks: List<MountainPeak>,
        maxDistance: Double = 100.0
    ): List<VisiblePeak> {
        return allPeaks.mapNotNull { peak ->
            val distance = calculateDistance(userLocation, peak.location)
            
            if (distance <= maxDistance) {
                val bearing = calculateBearing(userLocation, peak.location)
                val elevationAngle = calculateElevationAngle(userLocation, peak, distance)
                val isVisible = isPeakVisible(userLocation, peak, elevationAngle, maxDistance)
                
                VisiblePeak(
                    peak = peak,
                    distance = distance,
                    bearing = bearing,
                    elevationAngle = elevationAngle,
                    isVisible = isVisible
                )
            } else null
        }.sortedBy { it.distance }
    }
    
    private fun toRadians(degrees: Double): Double {
        return degrees * PI / 180.0
    }

    private fun toDegrees(radians: Double): Double {
        return radians * 180.0 / PI
    }
}
