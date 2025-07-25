package com.mountainspotter.shared.service

import com.mountainspotter.shared.model.MountainPeak
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageCacheService(private val httpClient: HttpClient) {
    
    private val imageUrlMap = mutableMapOf<String, String>()
    
    /**
     * Fetch and cache images for the given mountain peaks
     * This implementation uses a simple Wikipedia/Wikimedia image search approach
     */
    suspend fun fetchAndCacheImages(peaks: List<MountainPeak>): List<MountainPeak> {
        return peaks.map { peak ->
            val imageUrl = getImageUrl(peak)
            peak.copy(imageUrl = imageUrl)
        }
    }
    
    /**
     * Get image URL for a mountain peak
     * In a real implementation, this would:
     * 1. Check local cache first
     * 2. Query image APIs (Wikimedia, Flickr, etc.)
     * 3. Download and cache the image locally
     * 4. Return local file path or cached URL
     */
    private suspend fun getImageUrl(peak: MountainPeak): String? {
        // Check cache first
        val cachedUrl = imageUrlMap[peak.id]
        if (cachedUrl != null) {
            return cachedUrl
        }
        
        try {
            // Simple approach: try to find image from Wikimedia Commons
            val imageUrl = searchWikimediaImage(peak.name)
            if (imageUrl != null) {
                imageUrlMap[peak.id] = imageUrl
                return imageUrl
            }
            
            // Fallback: generate a placeholder image URL
            val placeholderUrl = generatePlaceholderImageUrl(peak)
            imageUrlMap[peak.id] = placeholderUrl
            return placeholderUrl
            
        } catch (e: Exception) {
            println("Error fetching image for ${peak.name}: ${e.message}")
            return null
        }
    }
    
    /**
     * Search for mountain image on Wikimedia Commons
     * This is a simplified implementation - a real app would use proper APIs
     */
    private suspend fun searchWikimediaImage(peakName: String): String? = withContext(Dispatchers.Default) {
        try {
            // Use Wikimedia Commons API to search for images
            val searchUrl = "https://commons.wikimedia.org/w/api.php"
            val query = peakName.replace(" ", "_")
            
            val response = httpClient.get(searchUrl) {
                parameter("action", "query")
                parameter("format", "json")
                parameter("list", "search")
                parameter("srsearch", "File:$query")
                parameter("srnamespace", "6") // File namespace
                parameter("srlimit", "1")
            }
            
            val responseText = response.bodyAsText()
            
            // Simple parsing - in a real app, use proper JSON parsing
            if (responseText.contains("\"title\":\"File:")) {
                val titleStart = responseText.indexOf("\"title\":\"File:") + 8
                val titleEnd = responseText.indexOf("\"", titleStart)
                if (titleEnd > titleStart) {
                    val fileName = responseText.substring(titleStart, titleEnd)
                    return@withContext "https://commons.wikimedia.org/wiki/$fileName"
                }
            }
            
            null
        } catch (e: Exception) {
            println("Wikimedia search error: ${e.message}")
            null
        }
    }
    
    /**
     * Generate a placeholder image URL for a mountain peak
     * Uses a service like picsum.photos or similar placeholder service
     */
    private fun generatePlaceholderImageUrl(peak: MountainPeak): String {
        // Use elevation and coordinates to generate a deterministic placeholder
        val seed = (peak.location.latitude + peak.location.longitude + peak.elevation).toInt().toString()
        return "https://picsum.photos/seed/$seed/400/300"
    }
    
    /**
     * Clear image cache
     */
    fun clearCache() {
        imageUrlMap.clear()
    }
    
    /**
     * Get cache size
     */
    fun getCacheSize(): Int {
        return imageUrlMap.size
    }
}