package com.mountainspotter.shared.service

import com.mountainspotter.shared.model.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * AI-based camera calibration service for automatic parameter estimation
 * This simulates a local neural network for camera parameter detection
 */
class CameraCalibrationService {
    
    /**
     * Analyzes camera frame and estimates optimal parameters using simulated AI
     * In a real implementation, this would use a TensorFlow Lite or similar local ML model
     */
    suspend fun calibrateCamera(
        visiblePeaks: List<VisiblePeak>,
        compassData: CompassData?,
        frameWidth: Int,
        frameHeight: Int
    ): CalibrationResult {
        // Simulate AI processing time
        delay(1000)
        
        val detectedFeatures = mutableListOf<DetectedFeature>()
        
        // Simulate horizon detection
        val horizonY = frameHeight * 0.5f + Random.nextFloat() * 20f - 10f
        detectedFeatures.add(
            DetectedFeature(
                type = FeatureType.HORIZON_LINE,
                screenX = frameWidth * 0.5f,
                screenY = horizonY,
                confidence = 0.85f + Random.nextFloat() * 0.1f
            )
        )
        
        // Simulate peak detection for visible peaks
        visiblePeaks.take(3).forEach { peak ->
            val peakX = frameWidth * (0.3f + Random.nextFloat() * 0.4f)
            val peakY = horizonY - (peak.elevationAngle * 5).toFloat()
            
            detectedFeatures.add(
                DetectedFeature(
                    type = FeatureType.MOUNTAIN_PEAK,
                    screenX = peakX,
                    screenY = peakY,
                    confidence = 0.7f + Random.nextFloat() * 0.2f
                )
            )
        }
        
        // Estimate camera parameters based on detected features
        val estimatedParameters = estimateParameters(
            detectedFeatures, 
            visiblePeaks, 
            compassData,
            frameWidth,
            frameHeight
        )
        
        val overallConfidence = detectedFeatures.map { it.confidence }.average().toFloat()
        
        return CalibrationResult(
            estimatedParameters = estimatedParameters,
            confidence = overallConfidence,
            detectedFeatures = detectedFeatures
        )
    }
    
    private fun estimateParameters(
        features: List<DetectedFeature>,
        peaks: List<VisiblePeak>,
        compassData: CompassData?,
        frameWidth: Int,
        frameHeight: Int
    ): CameraParameters {
        // Estimate field of view based on peak distribution
        val estimatedFOV = estimateFieldOfView(peaks, frameWidth)
        
        // Estimate compass correction based on peak positions vs expected bearings
        val compassCorrection = estimateCompassCorrection(peaks, compassData)
        
        // Estimate translation based on horizon line detection
        val horizonFeature = features.find { it.type == FeatureType.HORIZON_LINE }
        val translationY = horizonFeature?.let { 
            (frameHeight * 0.5f - it.screenY) * 0.5f 
        } ?: 0f
        
        return CameraParameters(
            fieldOfView = estimatedFOV,
            zoomLevel = 1f, // Default zoom level
            translationX = 0f,
            translationY = translationY,
            compassCorrection = compassCorrection,
            isCalibrated = true
        )
    }
    
    private fun estimateFieldOfView(peaks: List<VisiblePeak>, frameWidth: Int): Float {
        if (peaks.size < 2) return 90f // Default FOV
        
        // Calculate angular spread of visible peaks
        val bearings = peaks.map { it.bearing }
        val minBearing = bearings.minOrNull() ?: 0.0
        val maxBearing = bearings.maxOrNull() ?: 0.0
        
        var angularSpread = maxBearing - minBearing
        
        // Handle bearing wrap-around (e.g., 350° to 10°)
        if (angularSpread > 180) {
            angularSpread = 360 - angularSpread
        }
        
        // Estimate FOV assuming peaks span about 60% of the frame
        val estimatedFOV = (angularSpread / 0.6).toFloat()
        
        // Clamp to reasonable FOV range (30° - 120°)
        return estimatedFOV.coerceIn(30f, 120f)
    }
    
    private fun estimateCompassCorrection(peaks: List<VisiblePeak>, compassData: CompassData?): Float {
        val currentAzimuth = compassData?.azimuth ?: return 0f
        
        if (peaks.isEmpty()) return 0f
        
        // For simplification, estimate a small random correction
        // In real implementation, this would compare expected vs actual peak positions
        return (Random.nextFloat() - 0.5f) * 10f // ±5° correction
    }
    
    /**
     * Updates camera parameters based on user input (e.g., zoom gestures)
     */
    fun updateZoomLevel(currentParams: CameraParameters, zoomFactor: Float): CameraParameters {
        val newZoomLevel = (currentParams.zoomLevel * zoomFactor).coerceIn(1f, 5f)
        return currentParams.copy(
            zoomLevel = newZoomLevel,
            fieldOfView = currentParams.fieldOfView / newZoomLevel
        )
    }
    
    /**
     * Applies manual translation adjustments
     */
    fun updateTranslation(
        currentParams: CameraParameters, 
        deltaX: Float, 
        deltaY: Float
    ): CameraParameters {
        return currentParams.copy(
            translationX = (currentParams.translationX + deltaX).coerceIn(-200f, 200f),
            translationY = (currentParams.translationY + deltaY).coerceIn(-200f, 200f)
        )
    }
}