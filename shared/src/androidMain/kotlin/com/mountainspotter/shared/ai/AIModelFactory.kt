package com.mountainspotter.shared.ai

import android.content.Context

/**
 * Android-specific AI model factory implementation
 */
actual object AIModelFactory {
    
    private lateinit var context: Context
    
    /**
     * Initialize the factory with Android context
     */
    fun initialize(context: Context) {
        this.context = context
    }
    
    actual fun createHorizonDetectionModel(): HorizonDetectionModel {
        return AndroidHorizonDetectionModel(context)
    }
    
    actual fun createPeakDetectionModel(): PeakDetectionModel {
        return AndroidPeakDetectionModel(context)
    }
    
    actual fun createImagePreprocessor(): ImagePreprocessor {
        return AndroidImagePreprocessor()
    }
}