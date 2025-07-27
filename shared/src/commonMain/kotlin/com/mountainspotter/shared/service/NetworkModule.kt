package com.mountainspotter.shared.service

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object NetworkModule {
    
    /**
     * Create and configure HttpClient for API calls with optimized timeouts for mobile
     */
    fun createHttpClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            
            // Configure timeouts for better mobile performance
            engine {
                // Platform-specific timeout configuration will be handled by the engine
            }
        }
    }
}