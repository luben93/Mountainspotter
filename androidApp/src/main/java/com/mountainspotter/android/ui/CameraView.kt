package com.mountainspotter.android.ui

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mountainspotter.shared.model.CompassData
import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.VisiblePeak
import kotlin.math.*

@Composable
fun CameraView(
    visiblePeaks: List<VisiblePeak>,
    currentLocation: Location?,
    compassData: CompassData?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var selectedLens by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    
    // Initialize camera
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = cameraProviderFuture.get()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { previewView ->
            cameraProvider?.let { provider ->
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(selectedLens)
                    .build()
                
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    // Handle camera binding error
                }
            }
        }
        
        // Horizon Overlay
        HorizonOverlay(
            visiblePeaks = visiblePeaks,
            compassData = compassData,
            modifier = Modifier.fillMaxSize()
        )
        
        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Back Button
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text("← Back", color = Color.White)
            }
            
            // Lens Selector
            LensSelector(
                selectedLens = selectedLens,
                onLensSelected = { lens ->
                    selectedLens = lens
                }
            )
        }
        
        // Bottom Info Panel
        CompassInfo(
            compassData = compassData,
            currentLocation = currentLocation,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun HorizonOverlay(
    visiblePeaks: List<VisiblePeak>,
    compassData: CompassData?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        
        compassData?.let { compass ->
            // Draw topographical horizon profile instead of flat line
            drawTopographicalHorizon(visiblePeaks, compass, centerX, centerY, canvasWidth, canvasHeight)
            
            // Draw peak markers on top of the profile
            visiblePeaks.filter { it.isVisible }.forEach { peak ->
                drawPeakMarker(peak, compass, centerX, centerY, canvasWidth, canvasHeight)
            }
            
            // Draw compass direction indicator
            drawCompassIndicator(compass, centerX, centerY)
        }
    }
}

private fun DrawScope.drawTopographicalHorizon(
    visiblePeaks: List<VisiblePeak>,
    compass: CompassData,
    centerX: Float,
    centerY: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    // Calculate base horizon position based on pitch
    val baseHorizonY = centerY + (compass.pitch * 10f)
    
    // Field of view (roughly ±60 degrees)
    val fieldOfView = 60.0
    
    // Filter peaks within field of view and sort by bearing
    val peaksInView = visiblePeaks.filter { peak ->
        val bearingDiff = peak.bearing - compass.azimuth
        val normalizedBearing = when {
            bearingDiff > 180 -> bearingDiff - 360
            bearingDiff < -180 -> bearingDiff + 360
            else -> bearingDiff
        }
        abs(normalizedBearing) <= fieldOfView && peak.isVisible
    }.sortedBy { peak ->
        val bearingDiff = peak.bearing - compass.azimuth
        when {
            bearingDiff > 180 -> bearingDiff - 360
            bearingDiff < -180 -> bearingDiff + 360
            else -> bearingDiff
        }
    }
    
    if (peaksInView.isEmpty()) {
        // Draw flat horizon line if no peaks visible
        drawLine(
            color = Color.White,
            start = Offset(0f, baseHorizonY),
            end = Offset(canvasWidth, baseHorizonY),
            strokeWidth = 2.dp.toPx(),
            alpha = 0.6f
        )
        return
    }
    
    // Create horizon profile points
    val profilePoints = mutableListOf<Offset>()
    
    // Add left edge point at base horizon
    profilePoints.add(Offset(0f, baseHorizonY))
    
    // Add peak points
    peaksInView.forEach { peak ->
        val bearingDiff = peak.bearing - compass.azimuth
        val normalizedBearing = when {
            bearingDiff > 180 -> bearingDiff - 360
            bearingDiff < -180 -> bearingDiff + 360
            else -> bearingDiff
        }
        
        val screenX = centerX + (normalizedBearing * canvasWidth / (fieldOfView * 2)).toFloat()
        val elevationOffset = (peak.elevationAngle * 15f).toFloat() // Scale elevation for visibility
        val screenY = baseHorizonY - elevationOffset
        
        // Ensure point is within screen bounds
        val clampedX = screenX.coerceIn(0f, canvasWidth)
        val clampedY = screenY.coerceIn(0f, canvasHeight)
        
        profilePoints.add(Offset(clampedX, clampedY))
    }
    
    // Add right edge point at base horizon
    profilePoints.add(Offset(canvasWidth, baseHorizonY))
    
    // Interpolate points for smoother profile
    val smoothedPoints = interpolateHorizonProfile(profilePoints, canvasWidth)
    
    // Draw the topographical horizon profile as a filled shape
    if (smoothedPoints.size >= 2) {
        // Create path for filled horizon silhouette
        val path = androidx.compose.ui.graphics.Path()
        
        // Start from bottom left
        path.moveTo(0f, canvasHeight)
        
        // Draw to first horizon point
        path.lineTo(smoothedPoints.first().x, smoothedPoints.first().y)
        
        // Draw the horizon profile
        smoothedPoints.windowed(2).forEach { (current, next) ->
            path.lineTo(next.x, next.y)
        }
        
        // Close the path at bottom right
        path.lineTo(canvasWidth, canvasHeight)
        path.lineTo(0f, canvasHeight)
        
        // Fill the horizon silhouette
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.3f)
        )
        
        // Draw the horizon profile line
        smoothedPoints.windowed(2).forEach { (current, next) ->
            drawLine(
                color = Color.White,
                start = current,
                end = next,
                strokeWidth = 3.dp.toPx(),
                alpha = 0.9f
            )
        }
    }
    
    // Draw horizon label
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().asFrameworkPaint().apply {
            color = Color.White.copy(alpha = 0.9f).hashCode()
            textSize = 12.sp.toPx()
            isAntiAlias = true
        }
        drawText("TOPOGRAPHICAL HORIZON", 10f, baseHorizonY - 10f, paint)
    }
}

private fun interpolateHorizonProfile(points: List<Offset>, canvasWidth: Float): List<Offset> {
    if (points.size < 2) return points
    
    val interpolatedPoints = mutableListOf<Offset>()
    val sortedPoints = points.sortedBy { it.x }
    
    // Sample points at regular intervals for smooth profile
    val sampleInterval = canvasWidth / 100f // Sample every 1% of screen width
    
    for (x in 0..100) {
        val targetX = x * sampleInterval
        
        // Find the two points to interpolate between
        val leftPoint = sortedPoints.lastOrNull { it.x <= targetX } ?: sortedPoints.first()
        val rightPoint = sortedPoints.firstOrNull { it.x >= targetX } ?: sortedPoints.last()
        
        val interpolatedY = if (leftPoint == rightPoint) {
            leftPoint.y
        } else {
            val ratio = (targetX - leftPoint.x) / (rightPoint.x - leftPoint.x)
            leftPoint.y + ratio * (rightPoint.y - leftPoint.y)
        }
        
        interpolatedPoints.add(Offset(targetX, interpolatedY))
    }
    
    return interpolatedPoints
}

// This function now draws peak markers on top of the topographical horizon profile
// The actual horizon profile is drawn by drawTopographicalHorizon() using VisiblePeak data
private fun DrawScope.drawPeakMarker(
    peak: VisiblePeak,
    compass: CompassData,
    centerX: Float,
    centerY: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    // Calculate peak position on screen
    val bearingDiff = peak.bearing - compass.azimuth
    val normalizedBearing = when {
        bearingDiff > 180 -> bearingDiff - 360
        bearingDiff < -180 -> bearingDiff + 360
        else -> bearingDiff
    }
    
    // Only show peaks within field of view (roughly ±60 degrees)
    val fieldOfView = 60.0
    if (abs(normalizedBearing) > fieldOfView) return
    
    // Calculate screen position
    val screenX = centerX + (normalizedBearing * canvasWidth / (fieldOfView * 2)).toFloat()
    val elevationOffset = (peak.elevationAngle * 15f).toFloat() // Scale elevation for visibility
    val baseHorizonY = centerY + compass.pitch * 10f
    val screenY = baseHorizonY - elevationOffset
    
    // Ensure marker is within screen bounds
    if (screenX < 0 || screenX > canvasWidth || screenY < 0 || screenY > canvasHeight) return
    
    // Draw peak marker as a small triangle pointing to the peak on the horizon
    val markerSize = 6.dp.toPx()
    val trianglePath = Path().apply {
        moveTo(screenX, screenY - markerSize)
        lineTo(screenX - markerSize / 2, screenY)
        lineTo(screenX + markerSize / 2, screenY)
        close()
    }
    
    drawPath(
        path = trianglePath,
        color = Color.Red,
        style = Stroke(width = 2.dp.toPx())
    )
    
    // Draw peak info above the marker
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().asFrameworkPaint().apply {
            color = Color.White.hashCode()
            textSize = 10.sp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        val text = "${peak.peak.name}\n${peak.peak.elevation.toInt()}m\n${String.format("%.1f", peak.distance)}km"
        val lines = text.split("\n")
        
        lines.forEachIndexed { index, line ->
            drawText(
                line,
                screenX,
                screenY - markerSize - 5.dp.toPx() - (index.toFloat() * 12.sp.toPx()),
                paint
            )
        }
    }
}

private fun DrawScope.drawCompassIndicator(
    compass: CompassData,
    centerX: Float,
    centerY: Float
) {
    // Draw compass rose at top center
    val compassRadius = 40.dp.toPx()
    val compassCenterX = centerX
    val compassCenterY = 60.dp.toPx()
    
    // Draw compass circle
    drawCircle(
        color = Color.White,
        radius = compassRadius,
        center = Offset(compassCenterX, compassCenterY),
        style = Stroke(width = 2.dp.toPx()),
        alpha = 0.8f
    )
    
    // Draw north indicator
    val northAngle = -compass.azimuth * PI / 180.0
    val northX = compassCenterX + (compassRadius * 0.8f * sin(northAngle)).toFloat()
    val northY = compassCenterY - (compassRadius * 0.8f * cos(northAngle)).toFloat()
    
    drawLine(
        color = Color.Red,
        start = Offset(compassCenterX, compassCenterY),
        end = Offset(northX, northY),
        strokeWidth = 3.dp.toPx()
    )
    
    // Draw "N" label
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().asFrameworkPaint().apply {
            color = Color.Red.hashCode()
            textSize = 14.sp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawText("N", northX, northY - 10f, paint)
    }
}

@Composable
fun LensSelector(
    selectedLens: Int,
    onLensSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Text(
                text = if (selectedLens == CameraSelector.LENS_FACING_BACK) "Wide" else "Front",
                color = Color.White
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Wide Lens") },
                onClick = {
                    onLensSelected(CameraSelector.LENS_FACING_BACK)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Front Camera") },
                onClick = {
                    onLensSelected(CameraSelector.LENS_FACING_FRONT)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun CompassInfo(
    compassData: CompassData?,
    currentLocation: Location?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            compassData?.let { compass ->
                Text(
                    text = "Azimuth: ${compass.azimuth.toInt()}°",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pitch: ${compass.pitch.toInt()}°",
                    color = Color.White,
                    fontSize = 12.sp
                )
            } ?: Text(
                text = "Compass: Not available",
                color = Color.White,
                fontSize = 12.sp
            )
            
            currentLocation?.let { location ->
                Text(
                    text = "Alt: ${location.altitude?.toInt() ?: 0}m",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}