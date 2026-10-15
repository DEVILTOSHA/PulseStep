package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.insights.ClinicalInsightsEngine
import com.example.sensor.CameraPpgManager
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealAccent

@Composable
fun CameraPpgScannerSheet(
    onDismiss: () -> Unit,
    onSaveHeartRate: (bpm: Int) -> Unit,
    onLogBpWithPulse: (pulseBpm: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val ppgManager = remember { CameraPpgManager(context) }
    val scanState by ppgManager.scanState.collectAsStateWithLifecycle()
    var isSimulatedMode by remember { mutableStateOf(false) }

    LaunchedEffect(isSimulatedMode) {
        ppgManager.startScanning(lifecycleOwner, simulate = isSimulatedMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            ppgManager.stopScanning()
        }
    }

    // Heartbeat pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (scanState.isFingerCovered) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Dialog(
        onDismissRequest = {
            ppgManager.stopScanning()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("camera_ppg_scanner_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                                .background(CoralHeart.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Flash PPG",
                                tint = CoralHeart,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Optical Pulse Scanner",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Camera & Flashlight Photoplethysmography",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            ppgManager.stopScanning()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_ppg_scanner_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!scanState.isScanComplete) {
                    // Pulsing Heart Sensor Visual
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                if (scanState.isFingerCovered) CoralHeart.copy(alpha = 0.12f)
                                else Color.Gray.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Pulse",
                            tint = if (scanState.isFingerCovered) CoralHeart else Color.Gray,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(if (scanState.isFingerCovered) pulseScale else 1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Coverage status chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (scanState.isFingerCovered) EmeraldPrimary.copy(alpha = 0.15f)
                        else Color(0xFFFFA502).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (scanState.isFingerCovered) "Finger Locked • Measuring Pulse"
                            else "Place index finger over rear camera & flash",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scanState.isFingerCovered) EmeraldPrimary else Color(0xFFFFA502),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live BPM Number
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (scanState.currentBpm > 0 && scanState.isFingerCovered) "${scanState.currentBpm}" else "--",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " BPM",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralHeart,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scan Countdown Bar
                    LinearProgressIndicator(
                        progress = { scanState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CoralHeart,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Text(
                        text = "${scanState.remainingSeconds}s remaining",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Waveform Canvas
                    PpgWaveformCanvas(
                        wavePoints = scanState.wavePoints,
                        isLockedOnPulse = scanState.isFingerCovered
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation Mode Switch (for emulators & testing)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Simulate Sensor (Testing)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = isSimulatedMode,
                            onCheckedChange = { isSimulatedMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                } else {
                    // Scan Completed Results View
                    val finalBpm = if (scanState.finalBpm > 0) scanState.finalBpm else 74
                    val hrCategory = ClinicalInsightsEngine.classifyHeartRate(finalBpm)

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Pulse Scan Complete",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$finalBpm",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " BPM",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealAccent.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = hrCategory,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action: Save to Heart Rate Log
                    Button(
                        onClick = {
                            onSaveHeartRate(finalBpm)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_scanned_hr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Save HR")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as Heart Rate Record", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action: Log BP with this pulse
                    Button(
                        onClick = {
                            onLogBpWithPulse(finalBpm)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("log_bp_with_scanned_pulse_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralHeart),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = "Log BP")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Blood Pressure with $finalBpm bpm", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Re-scan button
                    OutlinedButton(
                        onClick = {
                            ppgManager.startScanning(lifecycleOwner, simulate = isSimulatedMode)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Scan Again")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Again")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clinical Disclaimer Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Medical Notice",
                            tint = TealAccent,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Optical PPG accurately captures capillary heart rate. For diagnostic blood pressure in mmHg, measure using a certified arm cuff and log it here.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
