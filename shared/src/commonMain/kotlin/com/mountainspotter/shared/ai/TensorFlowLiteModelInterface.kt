package com.mountainspotter.shared.ai

/**
 * Interface for TensorFlow Lite model operations across platforms
 */
interface TensorFlowLiteModelInterface {
    
    /**
     * Load a TensorFlow Lite model from assets
     */
    suspend fun loadModel(modelPath: String): Boolean
    
    /**
     * Run inference on input data
     */
    suspend fun runInference(inputData: FloatArray): FloatArray?
    
    /**
     * Get model input shape
     */
    fun getInputShape(): IntArray
    
    /**
     * Get model output shape  
     */
    fun getOutputShape(): IntArray
    
    /**
     * Clean up model resources
     */
    fun close()
}

/**
 * Horizon detection model wrapper
 */
interface HorizonDetectionModel : TensorFlowLiteModelInterface {
    
    /**
     * Detect horizon line in camera frame
     * @param frameData Raw camera frame data (RGB)
     * @param width Frame width
     * @param height Frame height
     * @return Horizon Y position (0.0-1.0 normalized) and confidence
     */
    suspend fun detectHorizon(frameData: ByteArray, width: Int, height: Int): Pair<Float, Float>
}

/**
 * Peak detection model wrapper
 */
interface PeakDetectionModel : TensorFlowLiteModelInterface {
    
    /**
     * Detect mountain peaks in camera frame
     * @param frameData Raw camera frame data (RGB)
     * @param width Frame width
     * @param height Frame height
     * @return List of detected peak positions and confidences
     */
    suspend fun detectPeaks(frameData: ByteArray, width: Int, height: Int): List<PeakDetection>
}

/**
 * Peak detection result
 */
data class PeakDetection(
    val x: Float,           // Normalized X position (0.0-1.0)
    val y: Float,           // Normalized Y position (0.0-1.0)
    val confidence: Float,  // Detection confidence (0.0-1.0)
    val height: Float       // Estimated peak height in pixels
)

/**
 * Computer vision preprocessing utilities
 */
interface ImagePreprocessor {
    
    /**
     * Convert RGBA to RGB
     */
    fun rgbaToRgb(rgba: ByteArray): ByteArray
    
    /**
     * Resize image to model input size
     */
    fun resizeImage(data: ByteArray, fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int): ByteArray
    
    /**
     * Apply Gaussian blur for noise reduction
     */
    fun applyGaussianBlur(data: ByteArray, width: Int, height: Int, radius: Float): ByteArray
    
    /**
     * Apply Canny edge detection
     */
    fun applyCannyEdgeDetection(data: ByteArray, width: Int, height: Int, lowThreshold: Float, highThreshold: Float): ByteArray
    
    /**
     * Convert image to grayscale
     */
    fun toGrayscale(rgbData: ByteArray): ByteArray
    
    /**
     * Normalize pixel values to 0.0-1.0 range
     */
    fun normalizePixels(data: ByteArray): FloatArray
}