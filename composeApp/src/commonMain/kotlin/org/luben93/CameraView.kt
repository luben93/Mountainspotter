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

@Composable
expect fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    onSwitchCamera: () -> Unit
)

@Composable
fun CameraView(
    visiblePeaks: List<VisiblePeak>,
    currentLocation: Location?,
    compassData: CompassData?,
    onBack: () -> Unit
) {
    var isFrontCamera by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            isFrontCamera = isFrontCamera,
            onSwitchCamera = { isFrontCamera = !isFrontCamera }
        )

        // Horizon Overlay - Add this component to display peaks
        HorizonOverlay(
            visiblePeaks = visiblePeaks,
            compassData = compassData,
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls - Moved to less intrusive position
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp), // Moved down from top
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                modifier = Modifier.size(width = 80.dp, height = 36.dp) // Smaller buttons
            ) {
                Text("←", fontSize = 18.sp) // Use symbol instead of text
            }

            // Camera switch button
            Button(
                onClick = { isFrontCamera = !isFrontCamera },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                modifier = Modifier.size(width = 80.dp, height = 36.dp) // Smaller buttons
            ) {
                Text("⟲", fontSize = 16.sp) // Use symbol instead of text
            }
        }

        // Debug information overlay
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

            // Visible peaks count
            Text(
                "Visible Peaks: ${visiblePeaks.size}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
