package com.mountainspotter.shared.model

/**
 * Camera parameters for AI-based calibration and peak overlay
 */
data class CameraParameters(
    val fieldOfView: Float = 90f,        // Horizontal field of view in degrees
    val zoomLevel: Float = 1f,           // Current zoom level (1.0 = no zoom)
    val translationX: Float = 0f,        // Horizontal translation offset in pixels
    val translationY: Float = 0f,        // Vertical translation offset in pixels
    val compassCorrection: Float = 0f,   // Compass correction in degrees
    val isCalibrated: Boolean = false    // Whether AI calibration has been performed
)

/**
 * AI calibration result containing estimated camera parameters
 */
data class CalibrationResult(
    val estimatedParameters: CameraParameters,
    val confidence: Float,               // Confidence score (0.0 - 1.0)
    val detectedFeatures: List<DetectedFeature>
)

/**
 * Features detected during AI calibration
 */
data class DetectedFeature(
    val type: FeatureType,
    val screenX: Float,
    val screenY: Float,
    val confidence: Float
)

enum class FeatureType {
    HORIZON_LINE,
    MOUNTAIN_PEAK,
    SKYLINE_POINT,
    COMPASS_REFERENCE
}