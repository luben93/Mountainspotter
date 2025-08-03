package com.mountainspotter.shared.ai

/**
 * iOS-specific AI model factory implementation
 */
actual object AIModelFactory {
    
    actual fun createHorizonDetectionModel(): HorizonDetectionModel {
        return IOSHorizonDetectionModel()
    }
    
    actual fun createPeakDetectionModel(): PeakDetectionModel {
        return IOSPeakDetectionModel()
    }
    
    actual fun createImagePreprocessor(): ImagePreprocessor {
        return IOSImagePreprocessor()
    }
}