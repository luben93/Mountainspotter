package com.mountainspotter.shared.repository

import com.mountainspotter.shared.model.MountainPeak
import com.mountainspotter.shared.model.Location
import kotlinx.coroutines.delay

/**
 * Repository for mountain peak data
 * In a real app, this would fetch from APIs like OpenStreetMap, PeakWare, or local databases
 */
class MountainRepository {
    
    // Sample data - simulate network delay for realistic behavior
    private val samplePeaks = listOf(
        MountainPeak(
            id = "mont_blanc",
            name = "Mont Blanc",
            location = Location(45.8326, 6.8652, 4809.0),
            elevation = 4809.0,
            prominence = 4696.0,
            country = "France/Italy",
            region = "Alps"
        ),
        MountainPeak(
            id = "matterhorn",
            name = "Matterhorn",
            location = Location(45.9763, 7.6586, 4478.0),
            elevation = 4478.0,
            prominence = 1042.0,
            country = "Switzerland/Italy",
            region = "Alps"
        ),
        MountainPeak(
            id = "mount_whitney",
            name = "Mount Whitney",
            location = Location(36.5786, -118.2920, 4421.0),
            elevation = 4421.0,
            prominence = 3072.0,
            country = "USA",
            region = "Sierra Nevada"
        ),
        MountainPeak(
            id = "denali",
            name = "Denali",
            location = Location(63.0692, -151.0070, 6190.0),
            elevation = 6190.0,
            prominence = 6144.0,
            country = "USA",
            region = "Alaska Range"
        ),
        MountainPeak(
            id = "mount_rainier",
            name = "Mount Rainier",
            location = Location(46.8523, -121.7603, 4392.0),
            elevation = 4392.0,
            prominence = 4026.0,
            country = "USA",
            region = "Cascade Range"
        )
    )
    
    /**
     * Get peaks within a certain radius of a location
     */
    suspend fun getPeaksNearLocation(
        location: Location,
        radiusKm: Double = 200.0
    ): List<MountainPeak> {
        // Simulate network delay to test background threading
        delay(1500)

        println("MountainRepository: Fetching peaks near ${location.latitude}, ${location.longitude}")

        // In a real implementation, this would:
        // 1. Query a spatial database
        // 2. Call external APIs (OpenStreetMap Overpass, PeakBagger, etc.)
        // 3. Filter based on geographical bounds
        
        // For now, return sample data with some location-based filtering
        return samplePeaks.filter { peak ->
            val distance = calculateDistance(location, peak.location)
            distance <= radiusKm
        }
    }
    
    /**
     * Get all known peaks (for demo purposes)
     */
    suspend fun getAllPeaks(): List<MountainPeak> {
        delay(1000) // Simulate network delay
        println("MountainRepository: Fetching all peaks")
        return samplePeaks
    }
    
    /**
     * Search peaks by name
     */
    suspend fun searchPeaks(query: String): List<MountainPeak> {
        delay(800) // Simulate network delay
        println("MountainRepository: Searching peaks with query: $query")
        return samplePeaks.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.region?.contains(query, ignoreCase = true) == true ||
            it.country?.contains(query, ignoreCase = true) == true
        }
    }

    /**
     * Calculate distance between two locations in km
     */
    private fun calculateDistance(loc1: Location, loc2: Location): Double {
        val earthRadius = 6371.0 // Earth's radius in km

        val lat1Rad = Math.toRadians(loc1.latitude)
        val lat2Rad = Math.toRadians(loc2.latitude)
        val deltaLatRad = Math.toRadians(loc2.latitude - loc1.latitude)
        val deltaLonRad = Math.toRadians(loc2.longitude - loc1.longitude)

        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLonRad / 2) * kotlin.math.sin(deltaLonRad / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return earthRadius * c
    }
}
