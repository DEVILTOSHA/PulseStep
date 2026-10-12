package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LatLngPoint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent

@Composable
fun RouteMapCanvas(
    routePoints: List<LatLngPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .testTag("route_map_canvas_box")
    ) {
        if (routePoints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GPS Route path will appear here as you move",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw map grid lines for tactical GPS visual appeal
                val gridSpacing = 40.dp.toPx()
                var gx = 0f
                while (gx < width) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(gx, 0f),
                        end = Offset(gx, height),
                        strokeWidth = 1f
                    )
                    gx += gridSpacing
                }
                var gy = 0f
                while (gy < height) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, gy),
                        end = Offset(width, gy),
                        strokeWidth = 1f
                    )
                    gy += gridSpacing
                }

                // Compute bounding box
                val minLat = routePoints.minOf { it.latitude }
                val maxLat = routePoints.maxOf { it.latitude }
                val minLng = routePoints.minOf { it.longitude }
                val maxLng = routePoints.maxOf { it.longitude }

                val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)

                val padding = 36.dp.toPx()
                val drawW = width - (padding * 2)
                val drawH = height - (padding * 2)

                fun project(lat: Double, lng: Double): Offset {
                    val px = padding + (((lng - minLng) / lngSpan) * drawW).toFloat()
                    val py = height - padding - (((lat - minLat) / latSpan) * drawH).toFloat()
                    return Offset(px, py)
                }

                // Draw route path polyline
                val path = Path()
                routePoints.forEachIndexed { i, pt ->
                    val offset = project(pt.latitude, pt.longitude)
                    if (i == 0) {
                        path.moveTo(offset.x, offset.y)
                    } else {
                        path.lineTo(offset.x, offset.y)
                    }
                }

                // Route glow stroke
                drawPath(
                    path = path,
                    color = TealAccent.copy(alpha = 0.3f),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Route main line
                drawPath(
                    path = path,
                    color = EmeraldPrimary,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Start pin (Green)
                val startPt = project(routePoints.first().latitude, routePoints.first().longitude)
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = startPt
                )
                drawCircle(
                    color = Color(0xFF22C55E),
                    radius = 5.dp.toPx(),
                    center = startPt
                )

                // Current moving location pin (Emerald pulse ring)
                if (routePoints.size > 1) {
                    val currentPt = project(routePoints.last().latitude, routePoints.last().longitude)
                    drawCircle(
                        color = EmeraldPrimary.copy(alpha = 0.35f),
                        radius = 12.dp.toPx(),
                        center = currentPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = currentPt
                    )
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = 5.dp.toPx(),
                        center = currentPt
                    )
                }
            }
        }

        // GPS Telemetry Overlay Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "GPS LIVE • ${routePoints.size} waypoints",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
