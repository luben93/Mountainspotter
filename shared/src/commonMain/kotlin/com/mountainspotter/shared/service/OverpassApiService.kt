package com.mountainspotter.shared.service

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.MountainPeak
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.PI

class OverpassApiService(private val httpClient: HttpClient) {
    
    private val baseUrl = "https://overpass-api.de/api/interpreter"
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Fetch mountain peaks within a radius of the given location using Overpass API
     */
    suspend fun getPeaksNearLocation(
        location: Location,
        radiusKm: Double = 50.0
    ): List<MountainPeak> {
        try {
            val bbox = calculateBoundingBox(location, radiusKm)
            val query = buildOverpassQuery(bbox)
            
            val response = httpClient.post(baseUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("data=$query")
            }
            
            val responseText = response.bodyAsText()
            val overpassResponse = json.decodeFromString<OverpassResponse>(responseText)
            
            return overpassResponse.elements.mapNotNull { element ->
                convertToMountainPeak(element)
            }
        } catch (e: Exception) {
            // Log error in real implementation
            println("Error fetching peaks: ${e.message}")
            return emptyList()
        }
    }
    
    /**
     * Calculate bounding box for Overpass query based on location and radius
     */
    private fun calculateBoundingBox(location: Location, radiusKm: Double): BoundingBox {
        val lat = location.latitude
        val lon = location.longitude
        
        // Convert radius from km to degrees (approximate)
        val latDelta = radiusKm / 111.0 // 1 degree lat ≈ 111 km
        val lonDelta = radiusKm / (111.0 * cos(lat * PI / 180.0)) // Adjust for longitude at this latitude
        
        return BoundingBox(
            south = lat - latDelta,
            west = lon - lonDelta,
            north = lat + latDelta,
            east = lon + lonDelta
        )
    }
    
    /**
     * Build Overpass API query for mountain peaks within bounding box
     */
    private fun buildOverpassQuery(bbox: BoundingBox): String {
        return """
            [out:json][timeout:25];
            (
              node["natural"="peak"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
              way["natural"="peak"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
              relation["natural"="peak"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});
            );
            out geom;
        """.trimIndent()
    }
    
    /**
     * Convert Overpass API element to MountainPeak
     */
    private fun convertToMountainPeak(element: OverpassElement): MountainPeak? {
        val tags = element.tags ?: return null
        val name = tags["name"] ?: return null
        
        // Get coordinates - handle different element types
        val (lat, lon) = when {
            element.lat != null && element.lon != null -> Pair(element.lat, element.lon)
            element.center != null -> Pair(element.center.lat, element.center.lon)
            element.geometry?.isNotEmpty() == true -> {
                val first = element.geometry.first()
                Pair(first.lat, first.lon)
            }
            else -> return null
        }
        
        // Parse elevation
        val elevation = tags["ele"]?.toDoubleOrNull() ?: 0.0
        
        // Generate deterministic ID from coordinates
        val id = "peak_${lat.toString().replace(".", "_")}_${lon.toString().replace(".", "_")}"
        
        return MountainPeak(
            id = id,
            name = name,
            location = Location(lat, lon, elevation),
            elevation = elevation,
            prominence = tags["prominence"]?.toDoubleOrNull(),
            country = tags["addr:country"] ?: tags["country"],
            region = tags["addr:state"] ?: tags["state"] ?: tags["region"],
            imageUrl = null // Will be populated by ImageCacheService
        )
    }
}

// Data classes for Overpass API response
@Serializable
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@Serializable
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val tags: Map<String, String>? = null,
    val center: OverpassCenter? = null,
    val geometry: List<OverpassGeometry>? = null
)

@Serializable
data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

@Serializable
data class OverpassGeometry(
    val lat: Double,
    val lon: Double
)

data class BoundingBox(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
)