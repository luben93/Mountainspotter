package com.mountainspotter.shared.ai

/**
 * Factory for creating platform-specific AI model implementations
 */
expect object AIModelFactory {
    
    /**
     * Create a horizon detection model for the current platform
     */
    fun createHorizonDetectionModel(): HorizonDetectionModel
    
    /**
     * Create a peak detection model for the current platform
     */
    fun createPeakDetectionModel(): PeakDetectionModel
    
    /**
     * Create an image preprocessor for the current platform
     */
    fun createImagePreprocessor(): ImagePreprocessor
}

/**
 * AI model manager for handling model lifecycle
 */
class AIModelManager {
    
    private var horizonModel: HorizonDetectionModel? = null
    private var peakModel: PeakDetectionModel? = null
    private var isInitialized = false
    
    /**
     * Initialize AI models asynchronously
     */
    suspend fun initialize(): Boolean {
        return try {
            horizonModel = AIModelFactory.createHorizonDetectionModel()
            peakModel = AIModelFactory.createPeakDetectionModel()
            
            val horizonLoaded = horizonModel?.loadModel("horizon_detection_model.tflite") ?: false
            val peakLoaded = peakModel?.loadModel("peak_detection_model.tflite") ?: false
            
            isInitialized = horizonLoaded || peakLoaded // At least one model should load
            isInitialized
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Detect horizon line using AI model
     */
    suspend fun detectHorizon(frameData: ByteArray, width: Int, height: Int): Pair<Float, Float>? {
        return if (isInitialized) {
            horizonModel?.detectHorizon(frameData, width, height)
        } else null
    }
    
    /**
     * Detect mountain peaks using AI model
     */
    suspend fun detectPeaks(frameData: ByteArray, width: Int, height: Int): List<PeakDetection> {
        return if (isInitialized) {
            peakModel?.detectPeaks(frameData, width, height) ?: emptyList()
        } else emptyList()
    }
    
    /**
     * Check if AI models are ready
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Clean up AI models
     */
    fun cleanup() {
        horizonModel?.close()
        peakModel?.close()
        horizonModel = null
        peakModel = null
        isInitialized = false
    }
}