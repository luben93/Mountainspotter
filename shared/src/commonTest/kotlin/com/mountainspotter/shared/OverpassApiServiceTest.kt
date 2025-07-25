package com.mountainspotter.shared

import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.service.OverpassApiService
import com.mountainspotter.shared.service.NetworkModule
import kotlin.test.Test
import kotlin.test.assertNotNull

class OverpassApiServiceTest {
    
    @Test
    fun testServiceCreation() {
        val httpClient = NetworkModule.createHttpClient()
        val service = OverpassApiService(httpClient)
        
        // Test that service can be created without errors
        assertNotNull(service)
        
        // Clean up
        httpClient.close()
    }
    
    @Test
    fun testNetworkModuleHttpClient() {
        val httpClient = NetworkModule.createHttpClient()
        
        assertNotNull(httpClient)
        
        // Clean up
        httpClient.close()
    }
}