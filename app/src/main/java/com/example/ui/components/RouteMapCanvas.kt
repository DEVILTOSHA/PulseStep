package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LatLngPoint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Precision Tactical GPS Radar HUD & Real-Time Pinpoint Tracking Canvas.
 * Renders an active radar sweep, coordinate crosshairs, concentric range rings,
 * compass cardinal directions, and a dynamic glowing route polyline.
 * Designed to never appear blank or empty, even while acquiring satellite fix.
 */
@Composable
fun RouteMapCanvas(
    routePoints: List<LatLngPoint> = emptyList(),
    currentPosition: LatLngPoint? = null,
    isTracking: Boolean = false,
    accuracyMeters: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tactical_hud_anim")

    // Radar scanner sweep angle
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    // Pinpoint beacon ripple pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF080E1E))
            .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(20.dp))
            .testTag("tactical_route_canvas_box")
    ) {
        // Main Tactical Canvas Drawing Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // 1. Tactical Coordinate Grid Lines
            val gridStep = 36.dp.toPx()
            var gx = centerX % gridStep
            while (gx < w) {
                drawLine(
                    color = Color(0xFF111E38),
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = centerY % gridStep
            while (gy < h) {
                drawLine(
                    color = Color(0xFF111E38),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            // 2. Concentric Radar Range Rings
            val maxRadius = minOf(centerX, centerY) * 0.88f
            val rings = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            rings.forEach { ratio ->
                drawCircle(
                    color = Color(0xFF1E3A5F).copy(alpha = 0.6f),
                    radius = maxRadius * ratio,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1f)
                )
            }

            // 3. Coordinate Crosshairs (Center Reticle)
            drawLine(
                color = Color(0xFF2A4D7A),
                start = Offset(centerX - maxRadius, centerY),
                end = Offset(centerX + maxRadius, centerY),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color(0xFF2A4D7A),
                start = Offset(centerX, centerY - maxRadius),
                end = Offset(centerX, centerY + maxRadius),
                strokeWidth = 1.2f
            )

            // Crosshair tick marks
            val tickCount = 6
            for (i in 1..tickCount) {
                val dx = (maxRadius / tickCount) * i
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                    start = Offset(centerX + dx, centerY - 4f),
                    end = Offset(centerX + dx, centerY + 4f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                    start = Offset(centerX - dx, centerY - 4f),
                    end = Offset(centerX - dx, centerY + 4f),
                    strokeWidth = 1.5f
                )
            }

            // 4. Rotating Radar Sweep Beam
            val sweepRad = Math.toRadians(radarAngle.toDouble())
            val sweepEndX = centerX + (maxRadius * cos(sweepRad)).toFloat()
            val sweepEndY = centerY + (maxRadius * sin(sweepRad)).toFloat()

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(EmeraldPrimary.copy(alpha = 0.85f), Color.Transparent),
                    start = Offset(centerX, centerY),
                    end = Offset(sweepEndX, sweepEndY)
                ),
                start = Offset(centerX, centerY),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 2.5f
            )

            // 5. Route Polyline or User Pinpoint Rendering
            if (routePoints.size >= 2) {
                // Compute dynamic bounding box
                val allLats = routePoints.map { it.latitude } + listOfNotNull(currentPosition?.latitude)
                val allLngs = routePoints.map { it.longitude } + listOfNotNull(currentPosition?.longitude)

                val minLat = allLats.minOrNull() ?: 0.0
                val maxLat = allLats.maxOrNull() ?: 0.0
                val minLng = allLngs.minOrNull() ?: 0.0
                val maxLng = allLngs.maxOrNull() ?: 0.0

                val latSpan = (maxLat - minLat).coerceAtLeast(0.0003)
                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0003)

                val padX = 40.dp.toPx()
                val padY = 50.dp.toPx()
                val drawW = w - (padX * 2)
                val drawH = h - (padY * 2)

                fun project(lat: Double, lng: Double): Offset {
                    val px = padX + (((lng - minLng) / lngSpan) * drawW).toFloat()
                    val py = h - padY - (((lat - minLat) / latSpan) * drawH).toFloat()
                    return Offset(px, py)
                }

                val path = Path()
                routePoints.forEachIndexed { i, pt ->
                    val off = project(pt.latitude, pt.longitude)
                    if (i == 0) path.moveTo(off.x, off.y) else path.lineTo(off.x, off.y)
                }

                // Route Glowing Aura
                drawPath(
                    path = path,
                    color = TealAccent.copy(alpha = 0.35f),
                    style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Route Main Line
                drawPath(
                    path = path,
                    color = EmeraldPrimary,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Start Marker (Blue Beacon)
                val startOff = project(routePoints.first().latitude, routePoints.first().longitude)
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = startOff)
                drawCircle(color = Color(0xFF3B82F6), radius = 4.5f.dp.toPx(), center = startOff)

                // Current Moving Position Marker
                val latestPt = currentPosition ?: routePoints.last()
                val latestOff = project(latestPt.latitude, latestPt.longitude)

                // Pulsing Ripple Wave
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = pulseAlpha),
                    radius = 16.dp.toPx() * pulseScale,
                    center = latestOff
                )
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = latestOff)
                drawCircle(color = EmeraldPrimary, radius = 5.dp.toPx(), center = latestOff)

            } else {
                // Single Point or Pre-Workout Standby Pinpoint
                // Center pinpoint beacon right at the radar crosshairs!
                if (currentPosition != null) {
                    // Pulsing ripple rings around user
                    drawCircle(
                        color = EmeraldPrimary.copy(alpha = pulseAlpha),
                        radius = 24.dp.toPx() * pulseScale,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = TealAccent.copy(alpha = 0.25f),
                        radius = 14.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = 5.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                } else {
                    // Satellites search reticle
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = pulseAlpha),
                        radius = 20.dp.toPx() * pulseScale,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                }
            }

            // 6. Corner Tactical Brackets
            val bracketLen = 14.dp.toPx()
            val m = 12.dp.toPx()
            val bracketColor = Color(0xFF38BDF8).copy(alpha = 0.6f)

            // Top-Left
            drawLine(bracketColor, Offset(m, m), Offset(m + bracketLen, m), 1.5f)
            drawLine(bracketColor, Offset(m, m), Offset(m, m + bracketLen), 1.5f)
            // Top-Right
            drawLine(bracketColor, Offset(w - m, m), Offset(w - m - bracketLen, m), 1.5f)
            drawLine(bracketColor, Offset(w - m, m), Offset(w - m, m + bracketLen), 1.5f)
            // Bottom-Left
            drawLine(bracketColor, Offset(m, h - m), Offset(m + bracketLen, h - m), 1.5f)
            drawLine(bracketColor, Offset(m, h - m), Offset(m, h - m - bracketLen), 1.5f)
            // Bottom-Right
            drawLine(bracketColor, Offset(w - m, h - m), Offset(w - m - bracketLen, h - m), 1.5f)
            drawLine(bracketColor, Offset(w - m, h - m), Offset(w - m, h - m - bracketLen), 1.5f)
        }

        // Overlay: Top Telemetry Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (currentPosition != null) EmeraldPrimary else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentPosition != null) {
                            if (routePoints.size > 1) "TACTICAL HUD • ${routePoints.size} WAYPOINTS" else "TACTICAL HUD • PINPOINT LOCKED"
                        } else {
                            "TACTICAL HUD • SEARCHING SATELLITES"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (currentPosition != null) EmeraldPrimary else Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Overlay: Cardinal Direction Markers (N, S, E, W)
        Text(
            text = "N",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF38BDF8),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
        )
        Text(
            text = "S",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
        Text(
            text = "W",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
        )
        Text(
            text = "E",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )

        // Overlay: Bottom Telemetry Coordinates Readout
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D4A)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (currentPosition != null) {
                    val latStr = "${"%.6f".format(Locale.US, currentPosition.latitude)}° N"
                    val lngStr = "${"%.6f".format(Locale.US, currentPosition.longitude)}° E"
                    Text(
                        text = "POS: $latStr, $lngStr",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val accText = if (accuracyMeters > 0) "±${accuracyMeters.toInt()}m ACCURACY" else "SATELLITE SYNCED"
                    Text(
                        text = "RADAR: $accText • LIVE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )
                } else {
                    Text(
                        text = "SCANNING GPS SIGNALS...",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = "Ensure location permissions & GPS are active",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
