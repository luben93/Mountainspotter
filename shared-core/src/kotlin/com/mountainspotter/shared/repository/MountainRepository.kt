package com.mountainspotter.shared.repository

import com.mountainspotter.shared.model.MountainPeak
import com.mountainspotter.shared.model.Location

/**
 * Repository for mountain peak data
 * In a real app, this would fetch from APIs like OpenStreetMap, PeakWare, or local databases
 */
class MountainRepository {
    
    // todo Sample data - in reality this would come from external sources
use overpass query with gps based limit
`
[out:json][timeout:25];
// gather results
nwr["natural"="peak"]({{bbox}});
// print results
out geom; `
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
        // In a real implementation, this would:
        // 1. Query a spatial database
        // 2. Call external APIs (OpenStreetMap Overpass, PeakBagger, etc.)
        // 3. Filter based on geographical bounds
        
        // For now, return sample data
        return samplePeaks
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
        return samplePeaks.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.region?.contains(query, ignoreCase = true) == true ||
            it.country?.contains(query, ignoreCase = true) == true
        }
    }
}
