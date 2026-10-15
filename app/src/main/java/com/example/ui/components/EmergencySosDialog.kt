package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LatLngPoint
import com.example.sensor.EmergencySignalController
import java.util.Locale

/**
 * Emergency SOS Dialog customized for India (ERSS - 112, Police - 100,
 * Ambulance - 108/102, Women Helpline - 1091, Highway Emergency - 1033).
 * Features high-pitch piercing audio siren (3,200 - 3,600 Hz) and synchronized camera flashlight strobe.
 */
@Composable
fun EmergencySosDialog(
    currentGpsPoint: LatLngPoint?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("pulse_step_sos_prefs", Context.MODE_PRIVATE) }

    var contactName by remember {
        mutableStateOf(prefs.getString("sos_contact_name", "ICE Contact") ?: "ICE Contact")
    }
    var contactPhone by remember {
        mutableStateOf(prefs.getString("sos_contact_phone", "112") ?: "112")
    }
    var isEditingContact by remember { mutableStateOf(false) }

    // High-pitch acoustic siren and physical camera flashlight strobe manager
    val signalController = remember { EmergencySignalController(context) }
    var isSignalActive by remember { mutableStateOf(false) }
    var signalMode by remember { mutableStateOf("PIERCING_SIREN") } // "PIERCING_SIREN" or "MORSE_SOS"

    DisposableEffect(isSignalActive, signalMode) {
        if (isSignalActive) {
            signalController.startSignal(coroutineScope, mode = signalMode)
        } else {
            signalController.stopSignal()
        }

        onDispose {
            signalController.stopSignal()
        }
    }

    // Determine current location: workout route point or last known system location, fallback to India center/capital
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
    val lastKnownLocation = remember {
        try {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            null
        }
    }

    val lat = currentGpsPoint?.latitude ?: lastKnownLocation?.latitude
    val lng = currentGpsPoint?.longitude ?: lastKnownLocation?.longitude
    val isLiveGpsAcquired = lat != null && lng != null
    val mapsUrl = if (isLiveGpsAcquired) "https://maps.google.com/?q=$lat,$lng" else "https://maps.google.com"
    val distressMessage = if (isLiveGpsAcquired) {
        "EMERGENCY SOS: Urgent help needed! My real-time pinpoint location on Google Maps: $mapsUrl (Coordinates: ${"%.6f".format(Locale.US, lat)}, ${"%.6f".format(Locale.US, lng)})."
    } else {
        "EMERGENCY SOS: Urgent help needed! My GPS position is currently being tracked via PulseStep."
    }

    // Pulsing Strobe Background Animation
    val infiniteTransition = rememberInfiniteTransition(label = "sos_strobe")
    val strobeColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF2A2A),
        targetValue = Color(0xFFB71515),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_anim"
    )

    AlertDialog(
        onDismissRequest = {
            isSignalActive = false
            signalController.stopSignal()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(strobeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS Warning",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EMERGENCY SOS (INDIA)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFFFF3838)
                        )
                        Text(
                            text = "High-Pitch Siren • Camera Flash Strobe • 112 ERSS",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    isSignalActive = false
                    signalController.stopSignal()
                    onDismiss()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // High-Pitch Audio & Camera Flash Strobe Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSignalActive) Color(0xFFFF3838).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isSignalActive) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF3838)) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "High-Pitch Siren & Flash Strobe",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isSignalActive) Color(0xFFFF3838) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Piercing sound audible far away + Camera Torch",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSignalActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFF3838))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "BLASTING",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Mode Selector: Piercing Siren vs Morse Code SOS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = signalMode == "PIERCING_SIREN",
                                onClick = { signalMode = "PIERCING_SIREN" },
                                label = { Text("3.5 kHz Siren", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = signalMode == "MORSE_SOS",
                                onClick = { signalMode = "MORSE_SOS" },
                                label = { Text("Morse SOS Flash", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Big Toggle Button for High-Pitch Siren + Flash Strobe
                        Button(
                            onClick = { isSignalActive = !isSignalActive },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("sos_siren_toggle_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignalActive) Color(0xFFFF3838) else Color(0xFFD63031)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSignalActive) Icons.Default.NotificationsActive else Icons.Default.FlashOn,
                                contentDescription = "Siren & Flash",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSignalActive) "STOP HIGH-PITCH SIREN & FLASH" else "ACTIVATE HIGH-PITCH SIREN & FLASH",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        // Feature Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isSignalActive) Color(0xFFFF3838) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("3,500 Hz Max Pitch", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = if (isSignalActive) Color(0xFFFF3838) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hardware Torch Strobe", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Live GPS Coordinates & Google Maps Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "GPS",
                                    tint = if (isLiveGpsAcquired) Color(0xFF00C896) else Color(0xFFFFA502),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLiveGpsAcquired) "Live GPS Coordinates (India)" else "Acquiring GPS Fix (India)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = if (isLiveGpsAcquired) "LOCKED" else "SEARCHING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiveGpsAcquired) Color(0xFF00C896) else Color(0xFFFFA502)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (lat != null && lng != null) {
                                    Text(
                                        text = "LAT: ${"%.5f".format(Locale.US, lat)}° N",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "LNG: ${"%.5f".format(Locale.US, lng)}° E",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Text(
                                        text = "ACQUIRING SATELLITE FIX...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFA502)
                                    )
                                    Text(
                                        text = "Searching for GPS satellites and network coordinates",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Open Pin in Google Maps App
                        OutlinedButton(
                            onClick = {
                                val gmmIntentUri = if (lat != null && lng != null) {
                                    Uri.parse("geo:$lat,$lng?q=$lat,$lng(User Pinpoint Location)&z=18")
                                } else {
                                    Uri.parse("geo:0,0?q=my+location")
                                }
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                    context.startActivity(browserIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sos_open_google_maps_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = "Google Maps", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Pinpoint in Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1-Tap Share Distress Message & Live Google Maps Link via SMS / WhatsApp / Apps
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, distressMessage)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Send Emergency SOS Broadcast"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sos_share_location_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3838)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share Location")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Live Google Maps Distress Link",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Primary 112 Direct Call Action (India National Emergency Helpline)
                Button(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:112")
                        }
                        context.startActivity(dialIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sos_call_112_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call 112")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Call 112 (National Emergency Helpline)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Pan-India All-in-One: Police • Fire • Ambulance",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Quick Indian Emergency Dialers Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "India Specialized Emergency Numbers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 108/102 Ambulance & 100 Police Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EmergencyServiceButton(
                                title = "108 Ambulance",
                                subtitle = "Medical",
                                phoneNumber = "108",
                                icon = Icons.Default.LocalHospital,
                                testTag = "sos_call_ambulance_108_button",
                                modifier = Modifier.weight(1f)
                            )
                            EmergencyServiceButton(
                                title = "100 Police",
                                subtitle = "Control Room",
                                phoneNumber = "100",
                                icon = Icons.Default.LocalPolice,
                                testTag = "sos_call_police_100_button",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 1091 Women Safety & 1033 Highway Helpline Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EmergencyServiceButton(
                                title = "1091 Women",
                                subtitle = "Helpline in Distress",
                                phoneNumber = "1091",
                                icon = Icons.Default.Security,
                                testTag = "sos_call_women_1091_button",
                                modifier = Modifier.weight(1f)
                            )
                            EmergencyServiceButton(
                                title = "1033 Highway",
                                subtitle = "Accident Rescue",
                                phoneNumber = "1033",
                                icon = Icons.Default.Warning,
                                testTag = "sos_call_highway_1033_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Configured Personal Emergency Contact (ICE in India)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Personal Emergency Contact (ICE)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            IconButton(onClick = { isEditingContact = !isEditingContact }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Contact",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = !isEditingContact) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = contactName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = contactPhone,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$contactPhone")
                                        }
                                        context.startActivity(dialIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call Contact", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 12.sp)
                                }
                            }
                        }

                        AnimatedVisibility(visible = isEditingContact) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = contactName,
                                    onValueChange = { contactName = it },
                                    label = { Text("Contact Name (e.g. Family / Doctor)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = contactPhone,
                                    onValueChange = { contactPhone = it },
                                    label = { Text("Phone Number (+91 Mobile or Landline)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        prefs.edit()
                                            .putString("sos_contact_name", contactName)
                                            .putString("sos_contact_phone", contactPhone)
                                            .apply()
                                        isEditingContact = false
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save Contact")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    isSignalActive = false
                    signalController.stopSignal()
                    onDismiss()
                },
                modifier = Modifier.testTag("dismiss_sos_dialog")
            ) {
                Text("Close SOS")
            }
        }
    )
}

@Composable
private fun EmergencyServiceButton(
    title: String,
    subtitle: String,
    phoneNumber: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(dialIntent)
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
