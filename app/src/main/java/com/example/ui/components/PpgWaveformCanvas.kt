package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldPrimary

@Composable
fun PpgWaveformCanvas(
    wavePoints: List<Float>,
    isLockedOnPulse: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090E17))
            .testTag("ppg_waveform_canvas_box")
    ) {
        if (wavePoints.isEmpty() || !isLockedOnPulse) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!isLockedOnPulse) "Cover rear camera & flashlight with your fingertip" else "Acquiring pulse waveform...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Background subtle medical oscilloscope grid
                val gridStep = 24.dp.toPx()
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridStep
                }
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = Color(0xFF1E293B).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridStep
                }

                // Draw central baseline
                val midY = height / 2f
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.dp.toPx()
                )

                // Plot wave points
                val stepX = width / (wavePoints.size - 1).coerceAtLeast(1)
                val path = Path()

                wavePoints.forEachIndexed { i, value ->
                    val px = i * stepX
                    // Map value in [-1f, 1f] to height
                    val py = midY - (value * (height * 0.42f))

                    if (i == 0) {
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }

                val strokeColor = if (isLockedOnPulse) CoralHeart else Color(0xFF94A3B8)

                // Glow stroke
                drawPath(
                    path = path,
                    color = strokeColor.copy(alpha = 0.35f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Sharp line stroke
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Leading glowing pulse point at the latest point
                if (wavePoints.isNotEmpty()) {
                    val lastX = (wavePoints.size - 1) * stepX
                    val lastY = midY - (wavePoints.last() * (height * 0.42f))
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = 4.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}
