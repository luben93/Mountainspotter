package org.luben93

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountainspotter.shared.model.CompassData
import com.mountainspotter.shared.model.VisiblePeak
import kotlin.math.*

@Composable
fun HorizonOverlay(
    visiblePeaks: List<VisiblePeak>,
    compassData: CompassData?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
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

                // Convert relativeBearing (0-360) to signed angle (-180 to +180)
                val signedAngle = if (relativeBearing > 180) relativeBearing - 360 else relativeBearing

                // Map signed angle to screen position for 180-degree field of view (90 degrees each side)
                // signedAngle -90° → left edge (x = 0)
                // signedAngle 0° → center (x = halfScreenWidth)  
                // signedAngle +90° → right edge (x = width)
                val peakX = (halfScreenWidth + (signedAngle / 90f) * halfScreenWidth).toFloat()
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

                    // Draw peak name and elevation as text
                    val peakText = "${peak.peak.name}\n${peak.peak.elevation.formatDecimal(0)}m"
                    val textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        background = Color.Black.copy(alpha = 0.7f)
                    )
                    
                    val textLayoutResult = textMeasurer.measure(peakText, textStyle)
                    val textWidth = textLayoutResult.size.width
                    val textHeight = textLayoutResult.size.height
                    
                    // Position text above the peak marker, centered horizontally
                    val textX = (peakX - textWidth / 2f).coerceIn(0f, size.width - textWidth)
                    val textY = (peakY - 20.dp.toPx() - textHeight).coerceAtLeast(0f)
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, textY)
                    )
                }
            }

            // Draw compass indicator
            drawCompassIndicator(azimuth, textMeasurer)
        }
    }
}

private fun DrawScope.drawCompassIndicator(azimuth: Float, textMeasurer: TextMeasurer) {
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

    // Draw cardinal points with text labels
    val directions = listOf(
        Pair(0f, "N"),
        Pair(90f, "E"), 
        Pair(180f, "S"),
        Pair(270f, "W")
    )

    directions.forEach { (direction, label) ->
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
        
        // Draw cardinal direction label
        val textStyle = TextStyle(
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        val textLayoutResult = textMeasurer.measure(label, textStyle)
        val textWidth = textLayoutResult.size.width
        val textHeight = textLayoutResult.size.height
        
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(x - textWidth / 2f, y - textHeight / 2f)
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
