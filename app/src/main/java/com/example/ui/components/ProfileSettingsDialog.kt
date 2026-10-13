package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TealAccent

@Composable
fun ProfileSettingsDialog(
    currentWeightKg: Int = 70,
    currentStrideLengthCm: Int = 76,
    onDismiss: () -> Unit,
    onSaveProfile: (weightKg: Int, strideLengthCm: Int) -> Unit,
    onResetCleanSlate: (() -> Unit)? = null
) {
    var weightText by remember { mutableStateOf(currentWeightKg.toString()) }
    var strideText by remember { mutableStateOf(currentStrideLengthCm.toString()) }
    var showConfirmResetDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profile & Biometrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure your body parameters to ensure accurate calorie expenditure and distance estimates.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { if (it.length <= 3) weightText = it.filter { c -> c.isDigit() } },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weight_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = strideText,
                        onValueChange = { if (it.length <= 3) strideText = it.filter { c -> c.isDigit() } },
                        label = { Text("Stride (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stride_input"),
                        singleLine = true
                    )
                }

                // Reset All Data / Clean Slate Button
                if (onResetCleanSlate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Data Management",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Wipe all current steps, blood pressure logs, heart rate records, and workouts to start as fresh as new.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showConfirmResetDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_clean_slate_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Reset All Data to Clean Slate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showConfirmResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmResetDialog = false },
                        title = { Text("Reset to Clean Slate?", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to clear all step records, cardiovascular logs, and workouts? The app will become as good as new.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmResetDialog = false
                                    onResetCleanSlate?.invoke()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("confirm_clean_slate_button")
                            ) {
                                Text("Yes, Reset Everything")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showConfirmResetDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Privacy & Medical Notice (Play Store policy compliance)
                var showFullPolicyDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Privacy",
                                tint = TealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Health Privacy & Play Compliance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your steps, heart rate, and blood pressure logs are stored locally in on-device SQLite database and synced to Google Health Connect only with your permission. Biometric data is never sold or transferred to 3rd parties.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showFullPolicyDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_full_privacy_policy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Privacy & Medical Disclaimer", fontSize = 11.sp)
                        }
                    }
                }

                if (showFullPolicyDialog) {
                    AlertDialog(
                        onDismissRequest = { showFullPolicyDialog = false },
                        title = {
                            Text(
                                text = "PulseStep Privacy & Health Compliance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        text = {
                            val policyScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(policyScrollState),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "1. Google Health Connect Integration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "PulseStep requests READ and WRITE permissions for Steps, Heart Rate, and Blood Pressure exclusively to aggregate and synchronize your daily activity within Google Health Connect. You can revoke permissions at any time through Android Settings > Privacy > Health Connect.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Text(
                                    text = "2. Optical Camera PPG Sensor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "When you use the Camera Pulse Scanner, the camera and LED torch are active solely to calculate optical capillary color variations in device RAM. No photos, videos, or facial imagery are ever saved, recorded, or transmitted.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Text(
                                    text = "3. Medical Device & Clinical Disclaimer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "PulseStep is a fitness and wellness companion. It is NOT an FDA, CE, or clinically approved medical diagnostic device. Blood pressure readings must be taken using certified clinical oscillometric cuffs. Always seek the advice of your physician regarding any medical conditions.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Text(
                                    text = "4. Zero Third-Party Tracking",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "PulseStep contains zero third-party behavioral advertising trackers. Your health reports can be exported or shared with your doctor strictly by your own explicit action.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showFullPolicyDialog = false },
                                modifier = Modifier.testTag("dismiss_policy_dialog")
                            ) {
                                Text("I Understand")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightText.toIntOrNull() ?: currentWeightKg
                    val s = strideText.toIntOrNull() ?: currentStrideLengthCm
                    onSaveProfile(w.coerceIn(30, 250), s.coerceIn(40, 150))
                },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_profile_button")
            ) {
                Text("Close")
            }
        }
    )
}
