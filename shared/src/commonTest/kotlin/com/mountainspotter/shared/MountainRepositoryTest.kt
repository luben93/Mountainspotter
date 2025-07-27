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
        assertTrue(peaks.size >= 8) // At least the original 5 + 3 new peaks
        
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
        
        // Search for Norwegian/Swedish peaks
        val scandinavianResults = repository.searchPeaks("Åreskutan")
        assertNotNull(scandinavianResults)
        assertTrue(scandinavianResults.any { it.name == "Åreskutan" })
        
        // Search by region
        val scandinaviaResults = repository.searchPeaks("Scandinavia")
        assertNotNull(scandinaviaResults)
        assertTrue(scandinaviaResults.size >= 3) // Should find the 3 new peaks
        assertTrue(scandinaviaResults.any { it.name == "Inste Åbittinden" })
        assertTrue(scandinaviaResults.any { it.name == "Åreskutan" })
        assertTrue(scandinaviaResults.any { it.name == "Gråhøgda" })
        
        // Search by country
        val norwayResults = repository.searchPeaks("Norway")
        assertNotNull(norwayResults)
        assertTrue(norwayResults.size >= 2) // Should find the 2 Norwegian peaks
        assertTrue(norwayResults.any { it.name == "Inste Åbittinden" })
        assertTrue(norwayResults.any { it.name == "Gråhøgda" })
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
        
        // Verify the original sample data has expected properties
        val montBlanc = peaks.find { it.name == "Mont Blanc" }
        assertNotNull(montBlanc)
        assertTrue(montBlanc.elevation > 4800)
        assertNotNull(montBlanc.imageUrl)
        
        val matterhorn = peaks.find { it.name == "Matterhorn" }
        assertNotNull(matterhorn)
        assertTrue(matterhorn.elevation > 4400)
        assertNotNull(matterhorn.imageUrl)
        
        // Verify the new Norwegian/Swedish peaks are present
        val instePeak = peaks.find { it.name == "Inste Åbittinden" }
        assertNotNull(instePeak)
        assertTrue(instePeak.elevation == 1396.0)
        assertTrue(instePeak.location.latitude > 62.0)
        assertTrue(instePeak.country == "Norway")
        
        val areskutan = peaks.find { it.name == "Åreskutan" }
        assertNotNull(areskutan)
        assertTrue(areskutan.elevation == 1420.0)
        assertTrue(areskutan.location.latitude > 63.0)
        assertTrue(areskutan.country == "Sweden")
        
        val grahogda = peaks.find { it.name == "Gråhøgda" }
        assertNotNull(grahogda)
        assertTrue(grahogda.elevation == 1436.0)
        assertTrue(grahogda.location.latitude > 62.0)
        assertTrue(grahogda.country == "Norway")
    }
}