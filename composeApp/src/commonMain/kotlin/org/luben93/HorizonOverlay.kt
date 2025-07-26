package org.luben93

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mountainspotter.shared.model.CompassData
import com.mountainspotter.shared.model.VisiblePeak
import kotlin.math.*

@Composable
fun HorizonOverlay(
    visiblePeaks: List<VisiblePeak>,
    compassData: CompassData?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        compassData?.let { compass ->
            val azimuth = compass.azimuth
            val halfScreenWidth = size.width / 2

            // Draw horizon line
            drawLine(
                color = Color.Green,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx()
            )

            // Draw visible peaks
            visiblePeaks.forEach { peak ->
                val peakBearing = peak.bearing

                // Calculate relative bearing to peak from current compass direction
                val relativeBearing = (peakBearing - azimuth + 360) % 360

                // Convert bearing to screen position (0 = center, -180/+180 = edges)
                // We map 360 degrees to screen width
                val peakX = (halfScreenWidth + halfScreenWidth * (relativeBearing - 180f) / 90f).toFloat()
                    .coerceIn(0f, size.width)

                // Draw peak indicator
                if (peakX >= 0 && peakX <= size.width) {
                    val elevationAngle = peak.elevationAngle.toFloat()
                    // Convert elevation angle to y position
                    // Negative elevation angle means the peak is below horizon
                    val peakY = size.height / 2 - (elevationAngle * 10f).dp.toPx()

                    // Draw peak marker
                    drawCircle(
                        color = Color.Red,
                        radius = 5.dp.toPx(),
                        center = Offset(peakX, peakY)
                    )

                    // Draw line from horizon to peak
                    drawLine(
                        color = Color.Red,
                        start = Offset(peakX, size.height / 2),
                        end = Offset(peakX, peakY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Instead of using platform-specific text drawing,
                    // just draw simple shapes to indicate peaks
                    drawCircle(
                        color = Color.Yellow,
                        radius = 3.dp.toPx(),
                        center = Offset(peakX, peakY - 15.dp.toPx())
                    )
                }
            }

            // Draw compass indicator
            drawCompassIndicator(azimuth)
        }
    }
}

private fun DrawScope.drawCompassIndicator(azimuth: Float) {
    // Draw a simple compass indicator at the top
    val centerX = size.width / 2
    val centerY = 50.dp.toPx()
    val radius = 40.dp.toPx()

    // Draw circle
    drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Draw cardinal points with lines instead of text
    val directions = listOf(0f, 90f, 180f, 270f) // N, E, S, W

    directions.forEach { direction ->
        val relativeDirection = (direction - azimuth + 360) % 360
        val angleRadians = relativeDirection * PI.toFloat() / 180f
        val x = centerX + cos(angleRadians) * radius * 0.7f
        val y = centerY - sin(angleRadians) * radius * 0.7f

        // Draw direction indicator line
        drawLine(
            color = Color.White,
            start = Offset(x, y),
            end = Offset(x + 10f * cos(angleRadians), y - 10f * sin(angleRadians)),
            strokeWidth = 2.dp.toPx()
        )
    }

    // Draw pointer
    val pointerPath = Path().apply {
        moveTo(centerX, centerY - radius * 0.9f)
        lineTo(centerX - 5.dp.toPx(), centerY)
        lineTo(centerX + 5.dp.toPx(), centerY)
        close()
    }

    drawPath(
        path = pointerPath,
        color = Color.Red,
        style = Stroke(width = 2.dp.toPx())
    )
}
