package com.mountainspotter.shared

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegrationTest {
    
    @Test
    fun testImageUrlsInSampleData() = runTest {
        val repository = MountainRepository()
        val allPeaks = repository.getAllPeaks()
        
        // Verify that sample data now has image URLs
        val montBlanc = allPeaks.find { it.name == "Mont Blanc" }
        assertNotNull(montBlanc)
        assertNotNull(montBlanc.imageUrl)
        assertTrue(montBlanc.imageUrl!!.startsWith("https://"))
        
        val matterhorn = allPeaks.find { it.name == "Matterhorn" }
        assertNotNull(matterhorn)
        assertNotNull(matterhorn.imageUrl)
        assertTrue(matterhorn.imageUrl!!.startsWith("https://"))
        
        // Test that all sample peaks have image URLs
        allPeaks.forEach { peak ->
            assertNotNull(peak.imageUrl, "Peak ${peak.name} should have an image URL")
            assertTrue(peak.imageUrl!!.startsWith("https://"), "Peak ${peak.name} image URL should be valid")
        }
        
        repository.close()
    }
    
    @Test
    fun testVisibilityCalculationsWithNewData() = runTest {
        val repository = MountainRepository()
        val calculationService = MountainCalculationService()
        
        // Test location: Near Mont Blanc (Alps)
        val userLocation = Location(45.8326, 6.8652, 1000.0)
        val allPeaks = repository.getAllPeaks()
        
        // Test that visibility calculations work with the enhanced peak data
        allPeaks.forEach { peak ->
            assertNotNull(peak.id)
            assertNotNull(peak.name)
            assertNotNull(peak.location)
            assertTrue(peak.elevation > 0)
            assertNotNull(peak.imageUrl) // This is the new functionality
            
            // Test that visibility calculations work with the peaks
            val distance = calculationService.calculateDistance(userLocation, peak.location)
            val bearing = calculationService.calculateBearing(userLocation, peak.location)
            val elevationAngle = calculationService.calculateElevationAngle(userLocation, peak, distance)
            val isVisible = calculationService.isPeakVisible(userLocation, peak, elevationAngle)
            
            assertTrue(distance >= 0)
            assertTrue(bearing >= 0 && bearing < 360)
        }
        
        repository.close()
    }
    
    @Test
    fun testCacheManagement() = runTest {
        val repository = MountainRepository()
        
        // Clear cache should work without error
        repository.clearCache()
        
        // Verify we can still get sample data after cache clear
        val peaks = repository.getAllPeaks()
        assertNotNull(peaks)
        assertTrue(peaks.isNotEmpty())
        
        // All peaks should have image URLs
        peaks.forEach { peak ->
            assertNotNull(peak.imageUrl)
        }
        
        repository.close()
    }
}