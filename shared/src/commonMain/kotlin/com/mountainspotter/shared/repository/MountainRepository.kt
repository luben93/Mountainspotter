package com.mountainspotter.shared.repository

import com.mountainspotter.shared.model.MountainPeak
import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.service.OverpassApiService
import com.mountainspotter.shared.service.OverpassResponse
import com.mountainspotter.shared.service.OverpassElement
import com.mountainspotter.shared.service.ImageCacheService
import com.mountainspotter.shared.service.NetworkModule
import kotlinx.serialization.json.Json

/**
 * Load a resource file as string - platform-specific implementation
 */
expect fun loadResource(resourcePath: String): String

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
    
    // JSON parser for fallback data
    private val json = Json { ignoreUnknownKeys = true }
    
    // Lazy-loaded fallback peaks from JSON file
    private val fallbackPeaks: List<MountainPeak> by lazy {
        loadFallbackPeaksFromJson()
    }
    
    /**
     * Get peaks within a certain radius of a location
     * Now uses Overpass API to fetch real mountain peak data
     */
    suspend fun getPeaksNearLocation(
        location: Location,
        radiusKm: Double = 50.0
    ): List<MountainPeak> {
        // Check if we can use cached data
        if (shouldUseCachedData(location, radiusKm)) {
            return cachedPeaks
        }

        // First, check if we have coverage in our fallback data
        val localPeaks = getPeaksFromFallbackData(location, radiusKm)
        if (localPeaks.isNotEmpty()) {
            // If local peaks are found, cache and return them
            cachedPeaks = localPeaks
            lastLocation = location
            lastRadius = radiusKm
            return localPeaks
        }

        // If no local peaks, try fetching from the API
        try {
            val apiPeaks = overpassApiService.getPeaksNearLocation(location, radiusKm)

            val peaksToCache = if (apiPeaks.isNotEmpty()) {
                // If API returns peaks, fetch images and prepare for caching
                imageCacheService.fetchAndCacheImages(apiPeaks)
            } else {
                // If API is empty, use all fallback peaks as a last resort
                fallbackPeaks
            }

            // Update cache with the result (either from API or all fallback)
            cachedPeaks = peaksToCache
            lastLocation = location
            lastRadius = radiusKm
            
            return peaksToCache

        } catch (e: Exception) {
            println("Error fetching peaks from API, using all fallback data: ${e.message}")
            // In case of API error, use all fallback peaks
            return fallbackPeaks
        }
    }
    
    /**
     * Get all known peaks (for demo purposes)
     */
    suspend fun getAllPeaks(): List<MountainPeak> {
        return fallbackPeaks
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
        
        // Fallback to fallback data search
        return fallbackPeaks.filter { 
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
    
    /**
     * Close resources (for testing and cleanup)
     */
    fun close() {
        httpClient.close()
    }
    
    /**
     * Load fallback peaks from the embedded JSON file
     */
    private fun loadFallbackPeaksFromJson(): List<MountainPeak> {
        return try {
            val jsonContent = loadResourceAsString("fallback_peaks.json")
            val overpassResponse = json.decodeFromString<OverpassResponse>(jsonContent)
            
            overpassResponse.elements.mapNotNull { element ->
                convertOverpassElementToMountainPeak(element)
            }
        } catch (e: Exception) {
            println("Error loading fallback peaks from JSON: ${e.message}")
            e.printStackTrace()
            // Return hardcoded fallback data if JSON loading fails
            getHardcodedFallbackPeaks()
        }
    }
    
    /**
     * Hardcoded fallback data for when JSON resource loading fails
     */
    private fun getHardcodedFallbackPeaks(): List<MountainPeak> {
        return listOf(
            MountainPeak(
                id = "peak_1",
                name = "Mont Blanc",
                location = Location(45.8326, 6.8652, 4809.0),
                elevation = 4809.0,
                prominence = 4696.0,
                country = "France/Italy",
                region = "Alps",
                imageUrl = "https://picsum.photos/seed/mont_blanc/400/300"
            ),
            MountainPeak(
                id = "peak_2",
                name = "Matterhorn",
                location = Location(45.9763, 7.6586, 4478.0),
                elevation = 4478.0,
                prominence = 1042.0,
                country = "Switzerland/Italy",
                region = "Alps",
                imageUrl = "https://picsum.photos/seed/matterhorn/400/300"
            ),
            MountainPeak(
                id = "peak_3",
                name = "Mount Whitney",
                location = Location(36.5786, -118.2920, 4421.0),
                elevation = 4421.0,
                prominence = 3072.0,
                country = "USA",
                region = "Sierra Nevada",
                imageUrl = "https://picsum.photos/seed/mount_whitney/400/300"
            ),
            MountainPeak(
                id = "peak_4",
                name = "Denali",
                location = Location(63.0692, -151.0070, 6190.0),
                elevation = 6190.0,
                prominence = 6144.0,
                country = "USA",
                region = "Alaska Range",
                imageUrl = "https://picsum.photos/seed/denali/400/300"
            ),
            MountainPeak(
                id = "peak_5",
                name = "Mount Rainier",
                location = Location(46.8523, -121.7603, 4392.0),
                elevation = 4392.0,
                prominence = 4026.0,
                country = "USA",
                region = "Cascade Range",
                imageUrl = "https://picsum.photos/seed/mount_rainier/400/300"
            ),
            MountainPeak(
                id = "peak_26862449",
                name = "Inste Åbittinden",
                location = Location(62.7211109, 8.2269910, 1396.0),
                elevation = 1396.0,
                prominence = null,
                country = "Norway",
                region = "Scandinavia",
                imageUrl = "https://picsum.photos/seed/inste_abittinden/400/300"
            ),
            MountainPeak(
                id = "peak_26862501",
                name = "Åreskutan",
                location = Location(63.4313938, 13.0932779, 1420.0),
                elevation = 1420.0,
                prominence = null,
                country = "Sweden",
                region = "Scandinavia",
                imageUrl = "https://picsum.photos/seed/areskutan/400/300"
            ),
            MountainPeak(
                id = "peak_26862513",
                name = "Gråhøgda",
                location = Location(62.1133333, 11.2963889, 1436.0),
                elevation = 1436.0,
                prominence = null,
                country = "Norway",
                region = "Scandinavia",
                imageUrl = "https://picsum.photos/seed/grahogda/400/300"
            )
        )
    }
    
    /**
     * Convert Overpass API element to MountainPeak with consistent image URLs
     */
    private fun convertOverpassElementToMountainPeak(element: OverpassElement): MountainPeak? {
        val tags = element.tags ?: return null
        val name = tags["name"] ?: return null
        val lat = element.lat ?: return null
        val lon = element.lon ?: return null
        val elevation = tags["ele"]?.toDoubleOrNull() ?: 0.0
        
        // Generate deterministic image URL based on name
        val imageUrl = "https://picsum.photos/seed/${name.lowercase().replace(" ", "_").replace("å", "a").replace("ö", "o").replace("ä", "a")}/400/300"
        
        return MountainPeak(
            id = "peak_${element.id}",
            name = name,
            location = Location(lat, lon, elevation),
            elevation = elevation,
            prominence = tags["prominence"]?.toDoubleOrNull(),
            country = tags["addr:country"] ?: tags["country"],
            region = tags["region"] ?: tags["addr:state"] ?: tags["state"],
            imageUrl = imageUrl
        )
    }
    
    /**
     * Check if we have coverage for a location in our fallback data
     */
    private fun getPeaksFromFallbackData(location: Location, radiusKm: Double): List<MountainPeak> {
        return fallbackPeaks.filter { peak ->
            val distance = calculateSimpleDistance(location, peak.location)
            distance <= radiusKm
        }
    }
    
    /**
     * Load a resource file as string - expect implementation
     */
    private fun loadResourceAsString(resourcePath: String): String {
        return loadResource(resourcePath)
    }
}
