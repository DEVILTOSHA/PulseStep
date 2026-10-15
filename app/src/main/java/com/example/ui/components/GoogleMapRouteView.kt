package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.location.LatLngPoint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent
import java.util.Locale

/**
 * Interactive Google Maps & Real-Time Pinpoint Tracking Component.
 * Seamlessly tracks and follows the user's pinpoint location while clocking workouts,
 * displaying live street/satellite maps, high-precision tactical HUD, and 1-tap Google Maps integration.
 * Never defaults or hardcodes artificial locations.
 */
@Composable
fun GoogleMapRouteView(
    routePoints: List<LatLngPoint>,
    currentPosition: LatLngPoint?,
    isWorkoutRunning: Boolean = false,
    accuracyMeters: Float = 0f,
    onRefreshLocation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSatelliteMode by remember { mutableStateOf(false) }
    var useTacticalView by remember { mutableStateOf(false) }
    var autoFollowPinpoint by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val effectiveLat = currentPosition?.latitude ?: routePoints.lastOrNull()?.latitude
    val effectiveLng = currentPosition?.longitude ?: routePoints.lastOrNull()?.longitude
    val hasPinpoint = effectiveLat != null && effectiveLng != null

    val googleMapsWebUrl = if (hasPinpoint) {
        "https://maps.google.com/?q=$effectiveLat,$effectiveLng&z=18"
    } else {
        "https://maps.google.com"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_pinpoint")
    val beaconPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // User Pinpoint Telemetry Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasPinpoint) EmeraldPrimary.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasPinpoint) Icons.Default.LocationOn else Icons.Default.GpsNotFixed,
                            contentDescription = "User Pinpoint",
                            tint = if (hasPinpoint) EmeraldPrimary else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (hasPinpoint) "USER PINPOINT LOCATION" else "ACQUIRING GPS PINPOINT...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasPinpoint) {
                            Text(
                                text = "${"%.6f".format(Locale.US, effectiveLat)}° N, ${"%.6f".format(Locale.US, effectiveLng)}° E",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        } else {
                            Text(
                                text = "Searching satellites (High Accuracy)",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isWorkoutRunning) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TRACKING LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    } else if (hasPinpoint) {
                        Text(
                            text = if (accuracyMeters > 0) "±${accuracyMeters.toInt()}m" else "GPS Locked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Force Locate Me / Refresh Button
                    IconButton(
                        onClick = onRefreshLocation,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("refresh_gps_pinpoint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh GPS Location",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Map View Selector & Open in Google Maps Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !useTacticalView,
                    onClick = { useTacticalView = false },
                    label = { Text("Google Map", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.testTag("select_interactive_map_chip")
                )
                FilterChip(
                    selected = useTacticalView,
                    onClick = { useTacticalView = true },
                    label = { Text("Tactical HUD", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.testTag("select_tactical_hud_chip")
                )
            }

            // Quick launch direct to Google Maps App
            Button(
                onClick = {
                    if (hasPinpoint) {
                        openInGoogleMaps(context, effectiveLat!!, effectiveLng!!)
                    } else {
                        onRefreshLocation()
                        val gmmIntentUri = Uri.parse("geo:0,0?q=my+location")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("open_in_google_maps_app_button")
            ) {
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open Google Maps", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Google Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Map Container Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (useTacticalView) {
                    RouteMapCanvas(
                        routePoints = routePoints,
                        currentPosition = currentPosition,
                        isTracking = isWorkoutRunning,
                        accuracyMeters = accuracyMeters,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (hasPinpoint) {
                        InteractiveMapWebView(
                            lat = effectiveLat!!,
                            lng = effectiveLng!!,
                            routePoints = routePoints,
                            isSatellite = isSatelliteMode,
                            autoFollow = autoFollowPinpoint || isWorkoutRunning,
                            accuracy = accuracyMeters,
                            onWebViewReady = { webViewRef = it }
                        )

                        // Map Overlay Badges and Quick Control Floaters
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.88f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isWorkoutRunning) "TRACKING • ${routePoints.size} waypoints" else "GPS PINPOINT LOCKED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        // Floating Map Controls (Auto-Follow, Satellite Switch & Center Pinpoint)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Satellite / Road Layer Toggle
                            Surface(
                                onClick = {
                                    isSatelliteMode = !isSatelliteMode
                                    webViewRef?.evaluateJavascript(
                                        "if (typeof setMapLayer === 'function') setMapLayer('${if (isSatelliteMode) "satellite" else "street"}');",
                                        null
                                    )
                                },
                                shape = CircleShape,
                                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Switch Map Layer",
                                        tint = if (isSatelliteMode) EmeraldPrimary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Auto-Follow Pinpoint Lock Toggle
                            Surface(
                                onClick = {
                                    autoFollowPinpoint = !autoFollowPinpoint
                                    webViewRef?.evaluateJavascript(
                                        "if (typeof centerOnLocation === 'function') centerOnLocation($effectiveLat, $effectiveLng);",
                                        null
                                    )
                                },
                                shape = CircleShape,
                                color = if (autoFollowPinpoint || isWorkoutRunning) EmeraldPrimary.copy(alpha = 0.9f) else Color(0xFF0F172A).copy(alpha = 0.9f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Auto-Follow Pinpoint",
                                        tint = if (autoFollowPinpoint || isWorkoutRunning) Color.White else EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Acquiring Satellite Signal View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF080E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "ACQUIRING YOUR GPS PINPOINT...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Connecting with Google Location Services & GPS satellites to pinpoint your exact coordinates.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    lineHeight = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = onRefreshLocation,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Find My Location", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct Google Maps Actions Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (hasPinpoint) {
                        openInGoogleMaps(context, effectiveLat!!, effectiveLng!!)
                    } else {
                        onRefreshLocation()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("open_gmaps_app_card_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open in Maps App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    if (hasPinpoint) {
                        val navUri = Uri.parse("google.navigation:q=$effectiveLat,$effectiveLng&mode=w")
                        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(navIntent)
                        } catch (_: Exception) {
                            openInGoogleMaps(context, effectiveLat!!, effectiveLng!!)
                        }
                    } else {
                        onRefreshLocation()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("google_maps_walk_nav_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Walk Directions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = {
                    val textToShare = if (hasPinpoint) {
                        "Check out my live pinpoint location on Google Maps: $googleMapsWebUrl (Coordinates: ${"%.6f".format(Locale.US, effectiveLat)}, ${"%.6f".format(Locale.US, effectiveLng)})"
                    } else {
                        "Tracking workout live with PulseStep GPS!"
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textToShare)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Pinpoint Location"))
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InteractiveMapWebView(
    lat: Double,
    lng: Double,
    routePoints: List<LatLngPoint>,
    isSatellite: Boolean,
    autoFollow: Boolean,
    accuracy: Float,
    onWebViewReady: (WebView) -> Unit
) {
    val polylineJson = remember(routePoints) {
        if (routePoints.isEmpty()) {
            "[[${"%.6f".format(Locale.US, lat)}, ${"%.6f".format(Locale.US, lng)}]]"
        } else {
            routePoints.joinToString(prefix = "[", postfix = "]") {
                "[${"%.6f".format(Locale.US, it.latitude)}, ${"%.6f".format(Locale.US, it.longitude)}]"
            }
        }
    }

    val htmlContent = remember(isSatellite) {
        generateMapHtml(lat, lng, isSatellite)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val safeAcc = if (accuracy > 0) accuracy else 12f
                        view?.evaluateJavascript("if (typeof updateRoutePath === 'function') updateRoutePath($polylineJson, $lat, $lng, $autoFollow, $safeAcc);", null)
                    }
                }
                loadDataWithBaseURL("https://maps.google.com/", htmlContent, "text/html", "UTF-8", null)
                onWebViewReady(this)
            }
        },
        update = { webView ->
            val safeAcc = if (accuracy > 0) accuracy else 12f
            webView.evaluateJavascript("if (typeof updateRoutePath === 'function') updateRoutePath($polylineJson, $lat, $lng, $autoFollow, $safeAcc);", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun openInGoogleMaps(context: Context, lat: Double, lng: Double) {
    val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(My Pinpoint Location)&z=18")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng&z=18"))
        context.startActivity(browserIntent)
    }
}

private fun generateMapHtml(initialLat: Double, initialLng: Double, isSatellite: Boolean): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: #080e1e;
                }
                .user-pinpoint-marker {
                    width: 22px;
                    height: 22px;
                    background-color: #00c896;
                    border: 3px solid #ffffff;
                    border-radius: 50%;
                    box-shadow: 0 0 16px #00c896;
                    animation: pinpoint-radar 1.5s infinite ease-out;
                }
                @keyframes pinpoint-radar {
                    0% { box-shadow: 0 0 0 0 rgba(0, 200, 150, 0.8); }
                    65% { box-shadow: 0 0 0 18px rgba(0, 200, 150, 0); }
                    100% { box-shadow: 0 0 0 0 rgba(0, 200, 150, 0); }
                }
                .start-pin {
                    width: 14px;
                    height: 14px;
                    background-color: #3b82f6;
                    border: 2px solid #ffffff;
                    border-radius: 50%;
                }
                .leaflet-control-attribution {
                    font-size: 8px !important;
                    background: rgba(8, 14, 30, 0.75) !important;
                    color: #94a3b8 !important;
                }
                .leaflet-control-attribution a {
                    color: #00c896 !important;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = null;
                var streetLayer = null;
                var satelliteLayer = null;
                var polyline = null;
                var polylineGlow = null;
                var userMarker = null;
                var accuracyCircle = null;
                var startMarker = null;

                try {
                    map = L.map('map', {
                        zoomControl: false,
                        attributionControl: true
                    }).setView([$initialLat, $initialLng], 17);

                    streetLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap, © CARTO'
                    });

                    satelliteLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                        maxZoom: 19,
                        attribution: '© Esri, Maxar'
                    });

                    var currentActiveLayer = ${if (isSatellite) "satelliteLayer" else "streetLayer"};
                    currentActiveLayer.addTo(map);

                    polylineGlow = L.polyline([], {
                        color: '#2dd4bf',
                        weight: 8,
                        opacity: 0.35,
                        lineCap: 'round',
                        lineJoin: 'round'
                    }).addTo(map);

                    polyline = L.polyline([], {
                        color: '#00c896',
                        weight: 4,
                        opacity: 1.0,
                        lineCap: 'round',
                        lineJoin: 'round'
                    }).addTo(map);

                    var pinpointIcon = L.divIcon({
                        className: 'user-pinpoint-marker',
                        iconSize: [22, 22],
                        iconAnchor: [11, 11]
                    });

                    userMarker = L.marker([$initialLat, $initialLng], { icon: pinpointIcon }).addTo(map);
                    accuracyCircle = L.circle([$initialLat, $initialLng], {
                        radius: 15,
                        color: '#00c896',
                        weight: 1,
                        fillColor: '#00c896',
                        fillOpacity: 0.12
                    }).addTo(map);

                } catch (e) {
                    console.error('Leaflet init error', e);
                }

                function setMapLayer(layerType) {
                    if (!map) return;
                    map.removeLayer(streetLayer);
                    map.removeLayer(satelliteLayer);
                    if (layerType === 'satellite') {
                        satelliteLayer.addTo(map);
                    } else {
                        streetLayer.addTo(map);
                    }
                }

                function centerOnLocation(lat, lng) {
                    if (!map) return;
                    map.panTo([lat, lng], { animate: true, duration: 0.5 });
                }

                function updateRoutePath(pts, curLat, curLng, autoFollow, acc) {
                    if (!map) return;
                    try {
                        if (pts && pts.length > 0) {
                            polyline.setLatLngs(pts);
                            polylineGlow.setLatLngs(pts);

                            if (!startMarker && pts.length > 1) {
                                var startIcon = L.divIcon({
                                    className: 'start-pin',
                                    iconSize: [14, 14],
                                    iconAnchor: [7, 7]
                                });
                                startMarker = L.marker(pts[0], { icon: startIcon }).addTo(map);
                            }
                        }

                        userMarker.setLatLng([curLat, curLng]);
                        if (accuracyCircle) {
                            accuracyCircle.setLatLng([curLat, curLng]);
                            if (acc && acc > 0) {
                                accuracyCircle.setRadius(acc);
                            }
                        }

                        if (autoFollow) {
                            map.panTo([curLat, curLng], { animate: true, duration: 0.35 });
                        }
                    } catch (err) {
                        console.error('Update path error', err);
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
