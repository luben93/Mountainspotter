package org.luben93

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountainspotter.shared.model.CompassData
import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.VisiblePeak
import com.mountainspotter.shared.model.CameraParameters

@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    zoomLevel: Float = 1f,
    onSwitchCamera: () -> Unit
)

@Composable
fun CameraView(
    visiblePeaks: List<VisiblePeak>,
    currentLocation: Location?,
    compassData: CompassData?,
    cameraParameters: CameraParameters = CameraParameters(),
    onBack: () -> Unit,
    onCalibrateCamera: (Int, Int) -> Unit = { _, _ -> },
    onZoomGesture: (Float) -> Unit = {},
    onPanGesture: (Float, Float) -> Unit = { _, _ -> },
    onResetCalibration: () -> Unit = {}
) {
    var isFrontCamera by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            isFrontCamera = isFrontCamera,
            zoomLevel = cameraParameters.zoomLevel,
            onSwitchCamera = { isFrontCamera = !isFrontCamera }
        )

        // Horizon Overlay - Add this component to display peaks with AI calibration
        HorizonOverlay(
            visiblePeaks = visiblePeaks,
            compassData = compassData,
            cameraParameters = cameraParameters,
            onZoomGesture = onZoomGesture,
            onPanGesture = onPanGesture,
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls - Enhanced with calibration controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                modifier = Modifier.size(width = 80.dp, height = 36.dp)
            ) {
                Text("←", fontSize = 18.sp)
            }

            // Control buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // AI Calibration button
                Button(
                    onClick = { onCalibrateCamera(1920, 1080) }, // Default camera resolution
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cameraParameters.isCalibrated) 
                            Color.Green.copy(alpha = 0.7f) else Color.Blue.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.size(width = 80.dp, height = 36.dp)
                ) {
                    Text("🤖", fontSize = 16.sp)
                }
                
                // Reset calibration button
                if (cameraParameters.isCalibrated) {
                    Button(
                        onClick = onResetCalibration,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF8C00).copy(alpha = 0.7f) // Orange color
                        ),
                        modifier = Modifier.size(width = 80.dp, height = 36.dp)
                    ) {
                        Text("↺", fontSize = 16.sp)
                    }
                }

                // Camera switch button
                Button(
                    onClick = { isFrontCamera = !isFrontCamera },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.size(width = 80.dp, height = 36.dp)
                ) {
                    Text("⟲", fontSize = 16.sp)
                }
            }
        }

        // Enhanced debug information overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
                .widthIn(max = 300.dp)
        ) {
            // Compass display
            Text(
                "Compass: ${compassData?.azimuth?.formatDecimal(1) ?: "N/A"}°",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            // Compass correction display
            if (cameraParameters.compassCorrection != 0f) {
                Text(
                    "Correction: ${cameraParameters.compassCorrection.formatDecimal(1)}°",
                    color = Color.Yellow,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Location display
            currentLocation?.let {
                Text(
                    "Lat: ${it.latitude.formatDecimal(5)}°",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "Long: ${it.longitude.formatDecimal(5)}°",
                    color = Color.White,
                    fontSize = 14.sp
                )
                it.altitude?.let { alt ->
                    Text(
                        "Alt: ${alt.formatDecimal(1)} m",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera parameters display
            Text(
                "Zoom: ${cameraParameters.zoomLevel.formatDecimal(1)}x",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Text(
                "FOV: ${cameraParameters.fieldOfView.formatDecimal(0)}°",
                color = Color.White,
                fontSize = 14.sp
            )
            
            if (cameraParameters.translationX != 0f || cameraParameters.translationY != 0f) {
                Text(
                    "Offset: ${cameraParameters.translationX.formatDecimal(0)}, ${cameraParameters.translationY.formatDecimal(0)}",
                    color = Color.Cyan,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visible peaks count and calibration status
            Text(
                "Visible Peaks: ${visiblePeaks.size}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Text(
                if (cameraParameters.isCalibrated) "AI Calibrated ✓" else "Manual Mode",
                color = if (cameraParameters.isCalibrated) Color.Green else Color.Yellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
