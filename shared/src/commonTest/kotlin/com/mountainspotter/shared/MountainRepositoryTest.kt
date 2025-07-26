package com.mountainspotter.shared

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.MountainPeak
import com.mountainspotter.shared.repository.MountainRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MountainRepositoryTest {
    
    @Test
    fun testGetAllPeaks() = runTest {
        val repository = MountainRepository()
        
        val peaks = repository.getAllPeaks()
        
        assertNotNull(peaks)
        assertTrue(peaks.isNotEmpty())
        assertTrue(peaks.size >= 5) // At least the sample peaks
        
        // Verify each peak has required fields
        peaks.forEach { peak ->
            assertNotNull(peak.id)
            assertNotNull(peak.name)
            assertNotNull(peak.location)
            assertTrue(peak.elevation > 0)
        }
    }
    
    @Test
    fun testSearchPeaks() = runTest {
        val repository = MountainRepository()
        
        // Search for Mont Blanc
        val results = repository.searchPeaks("Mont Blanc")
        
        assertNotNull(results)
        assertTrue(results.any { it.name.contains("Mont Blanc") })
    }
    
    @Test
    fun testCacheClearing() {
        val repository = MountainRepository()
        
        // Should not throw exception
        repository.clearCache()
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun testMountainPeakDataIntegrity() = runTest {
        val repository = MountainRepository()
        val peaks = repository.getAllPeaks()
        
        // Verify the sample data has expected properties
        val montBlanc = peaks.find { it.name == "Mont Blanc" }
        assertNotNull(montBlanc)
        assertTrue(montBlanc.elevation > 4800)
        assertNotNull(montBlanc.imageUrl)
        
        val matterhorn = peaks.find { it.name == "Matterhorn" }
        assertNotNull(matterhorn)
        assertTrue(matterhorn.elevation > 4400)
        assertNotNull(matterhorn.imageUrl)
    }
}