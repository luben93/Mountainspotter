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
            // Draw horizon line
            drawHorizonLine(compass, centerX, centerY, canvasWidth)
            
            // Draw peak markers
            visiblePeaks.filter { it.isVisible }.forEach { peak ->
                drawPeakMarker(peak, compass, centerX, centerY, canvasWidth, canvasHeight)
            }
            
            // Draw compass direction indicator
            drawCompassIndicator(compass, centerX, centerY)
        }
    }
}

private fun DrawScope.drawHorizonLine(
    compass: CompassData,
    centerX: Float,
    centerY: Float,
    canvasWidth: Float
) {
    // Calculate horizon line position based on pitch
    val horizonY = centerY + (compass.pitch * 10f) // Scale pitch for visibility
    
    drawLine(
        color = Color.White,
        start = Offset(0f, horizonY),
        end = Offset(canvasWidth, horizonY),
        strokeWidth = 3.dp.toPx(),
        alpha = 0.8f
    )
    
    // Draw horizon line label
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().asFrameworkPaint().apply {
            color = Color.White.copy(alpha = 0.9f).hashCode()
            textSize = 14.sp.toPx()
            isAntiAlias = true
        }
        drawText("HORIZON", 10f, horizonY - 10f, paint)
    }
}

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
    val screenY = centerY + compass.pitch * 10f - elevationOffset
    
    // Ensure marker is within screen bounds
    if (screenX < 0 || screenX > canvasWidth || screenY < 0 || screenY > canvasHeight) return
    
    // Draw peak marker
    drawCircle(
        color = Color.Red,
        radius = 8.dp.toPx(),
        center = Offset(screenX, screenY),
        style = Stroke(width = 2.dp.toPx())
    )
    
    // Draw peak info
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().asFrameworkPaint().apply {
            color = Color.White.hashCode()
            textSize = 12.sp.toPx()
            isAntiAlias = true
        }
        
        val text = "${peak.peak.name}\n${peak.peak.elevation.toInt()}m"
        val lines = text.split("\n")
        
        lines.forEachIndexed { index, line ->
            drawText(
                line,
                screenX + 12.dp.toPx(),
                screenY + (index.toFloat() * 16.sp.toPx()) - 4.dp.toPx(),
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