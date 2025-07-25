package com.mountainspotter.shared.repository

import com.mountainspotter.shared.model.MountainPeak
import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.service.OverpassApiService
import com.mountainspotter.shared.service.ImageCacheService
import com.mountainspotter.shared.service.NetworkModule

/**
 * Repository for mountain peak data
 * Uses Overpass API to fetch real mountain peak data from OpenStreetMap
 */
class MountainRepository {
    
    private val httpClient = NetworkModule.createHttpClient()
    private val overpassApiService = OverpassApiService(httpClient)
    private val imageCacheService = ImageCacheService(httpClient)
    
    // Cache for peaks to avoid frequent API calls
    private var cachedPeaks: List<MountainPeak> = emptyList()
    private var lastLocation: Location? = null
    private var lastRadius: Double = 0.0
    
    // Fallback sample data for offline or error scenarios
    private val samplePeaks = listOf(
        MountainPeak(
            id = "mont_blanc",
            name = "Mont Blanc",
            location = Location(45.8326, 6.8652, 4809.0),
            elevation = 4809.0,
            prominence = 4696.0,
            country = "France/Italy",
            region = "Alps",
            imageUrl = "https://picsum.photos/seed/mont_blanc/400/300"
        ),
        MountainPeak(
            id = "matterhorn",
            name = "Matterhorn",
            location = Location(45.9763, 7.6586, 4478.0),
            elevation = 4478.0,
            prominence = 1042.0,
            country = "Switzerland/Italy",
            region = "Alps",
            imageUrl = "https://picsum.photos/seed/matterhorn/400/300"
        ),
        MountainPeak(
            id = "mount_whitney",
            name = "Mount Whitney",
            location = Location(36.5786, -118.2920, 4421.0),
            elevation = 4421.0,
            prominence = 3072.0,
            country = "USA",
            region = "Sierra Nevada",
            imageUrl = "https://picsum.photos/seed/mount_whitney/400/300"
        ),
        MountainPeak(
            id = "denali",
            name = "Denali",
            location = Location(63.0692, -151.0070, 6190.0),
            elevation = 6190.0,
            prominence = 6144.0,
            country = "USA",
            region = "Alaska Range",
            imageUrl = "https://picsum.photos/seed/denali/400/300"
        ),
        MountainPeak(
            id = "mount_rainier",
            name = "Mount Rainier",
            location = Location(46.8523, -121.7603, 4392.0),
            elevation = 4392.0,
            prominence = 4026.0,
            country = "USA",
            region = "Cascade Range",
            imageUrl = "https://picsum.photos/seed/mount_rainier/400/300"
        )
    )
    
    /**
     * Get peaks within a certain radius of a location
     * Now uses Overpass API to fetch real mountain peak data
     */
    suspend fun getPeaksNearLocation(
        location: Location,
        radiusKm: Double = 50.0
    ): List<MountainPeak> {
        try {
            // Check if we can use cached data
            if (shouldUseCachedData(location, radiusKm)) {
                return cachedPeaks
            }
            
            // Fetch peaks from Overpass API
            val peaks = overpassApiService.getPeaksNearLocation(location, radiusKm)
            
            // If we got peaks from the API, fetch and cache images
            val peaksWithImages = if (peaks.isNotEmpty()) {
                imageCacheService.fetchAndCacheImages(peaks)
            } else {
                // Fallback to sample data if API fails or returns no results
                samplePeaks
            }
            
            // Update cache
            cachedPeaks = peaksWithImages
            lastLocation = location
            lastRadius = radiusKm
            
            return peaksWithImages
            
        } catch (e: Exception) {
            println("Error fetching peaks from API, using fallback data: ${e.message}")
            // Return sample data as fallback
            return samplePeaks
        }
    }
    
    /**
     * Get all known peaks (for demo purposes)
     */
    suspend fun getAllPeaks(): List<MountainPeak> {
        return samplePeaks
    }
    
    /**
     * Search peaks by name
     */
    suspend fun searchPeaks(query: String): List<MountainPeak> {
        // First try to search in cached data
        val cachedResults = cachedPeaks.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.region?.contains(query, ignoreCase = true) == true ||
            it.country?.contains(query, ignoreCase = true) == true
        }
        
        if (cachedResults.isNotEmpty()) {
            return cachedResults
        }
        
        // Fallback to sample data search
        return samplePeaks.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.region?.contains(query, ignoreCase = true) == true ||
            it.country?.contains(query, ignoreCase = true) == true
        }
    }
    
    /**
     * Check if we should use cached data to avoid unnecessary API calls
     */
    private fun shouldUseCachedData(location: Location, radiusKm: Double): Boolean {
        val lastLoc = lastLocation ?: return false
        
        // Check if location hasn't changed much (within 5km) and radius is similar
        val distance = calculateSimpleDistance(lastLoc, location)
        return distance < 5.0 && kotlin.math.abs(lastRadius - radiusKm) < 10.0 && cachedPeaks.isNotEmpty()
    }
    
    /**
     * Simple distance calculation for cache checking
     */
    private fun calculateSimpleDistance(loc1: Location, loc2: Location): Double {
        val earthRadius = 6371.0 // km
        val dLat = kotlin.math.PI / 180 * (loc2.latitude - loc1.latitude)
        val dLon = kotlin.math.PI / 180 * (loc2.longitude - loc1.longitude)
        val a = kotlin.math.sin(dLat/2) * kotlin.math.sin(dLat/2) +
                kotlin.math.cos(kotlin.math.PI / 180 * loc1.latitude) * kotlin.math.cos(kotlin.math.PI / 180 * loc2.latitude) *
                kotlin.math.sin(dLon/2) * kotlin.math.sin(dLon/2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1-a))
        return earthRadius * c
    }
    
    /**
     * Clear cache and force refresh on next call
     */
    fun clearCache() {
        cachedPeaks = emptyList()
        lastLocation = null
        lastRadius = 0.0
        imageCacheService.clearCache()
    }
}
