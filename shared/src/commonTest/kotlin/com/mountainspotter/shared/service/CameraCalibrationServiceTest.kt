package com.mountainspotter.shared.service

import com.mountainspotter.shared.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraCalibrationServiceTest {
    
    private val calibrationService = CameraCalibrationService()
    
    @Test
    fun testCalibrateCamera_withValidPeaks_returnsCalibrationResult() = runTest {
        // Arrange
        val peaks = listOf(
            VisiblePeak(
                peak = MountainPeak(
                    id = "1",
                    name = "Test Peak",
                    location = Location(46.0, 8.0, 3000.0),
                    elevation = 3000.0
                ),
                distance = 10.0,
                bearing = 90.0,
                elevationAngle = 5.0,
                isVisible = true
            )
        )
        val compassData = CompassData(azimuth = 90f, pitch = 0f, roll = 0f)
        
        // Act
        val result = calibrationService.calibrateCamera(peaks, compassData, 1920, 1080, null)
        
        // Assert
        assertTrue(result.estimatedParameters.isCalibrated)
        assertTrue(result.confidence > 0.0f)
        assertTrue(result.detectedFeatures.isNotEmpty())
        assertTrue(result.estimatedParameters.fieldOfView in 30f..120f)
    }
    
    @Test
    fun testUpdateZoomLevel_increasesZoom() {
        // Arrange
        val initialParams = CameraParameters(zoomLevel = 1f, baseFOV = 90f)
        val zoomFactor = 2f
        
        // Act
        val result = calibrationService.updateZoomLevel(initialParams, zoomFactor)
        
        // Assert
        assertEquals(2f, result.zoomLevel)
        assertEquals(45f, result.fieldOfView) // FOV should be halved when zoom is doubled
    }
    
    @Test
    fun testUpdateZoomLevel_clampsToMaxZoom() {
        // Arrange
        val initialParams = CameraParameters(zoomLevel = 4f, baseFOV = 90f)
        val zoomFactor = 2f
        
        // Act
        val result = calibrationService.updateZoomLevel(initialParams, zoomFactor)
        
        // Assert
        assertEquals(5f, result.zoomLevel) // Should be clamped to max zoom of 5x
    }
    
    @Test
    fun testUpdateTranslation_appliesCorrectOffset() {
        // Arrange
        val initialParams = CameraParameters(translationX = 0f, translationY = 0f)
        val deltaX = 50f
        val deltaY = -30f
        
        // Act
        val result = calibrationService.updateTranslation(initialParams, deltaX, deltaY)
        
        // Assert
        assertEquals(50f, result.translationX)
        assertEquals(-30f, result.translationY)
    }
    
    @Test
    fun testUpdateTranslation_clampsToLimits() {
        // Arrange
        val initialParams = CameraParameters(translationX = 150f, translationY = -150f)
        val deltaX = 100f
        val deltaY = -100f
        
        // Act
        val result = calibrationService.updateTranslation(initialParams, deltaX, deltaY)
        
        // Assert
        assertEquals(200f, result.translationX) // Should be clamped to max 200
        assertEquals(-200f, result.translationY) // Should be clamped to min -200
    }
}