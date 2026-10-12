package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insights.ClinicalInsightsEngine
import com.example.ui.theme.BpCrisis
import com.example.ui.theme.BpElevated
import com.example.ui.theme.BpNormal
import com.example.ui.theme.BpStage1
import com.example.ui.theme.BpStage2

@Composable
fun LogBloodPressureDialog(
    initialPulse: Int = 72,
    onDismiss: () -> Unit,
    onConfirm: (systolic: Int, diastolic: Int, pulse: Int, note: String) -> Unit
) {
    var systolicText by remember { mutableStateOf("120") }
    var diastolicText by remember { mutableStateOf("80") }
    var pulseText by remember { mutableStateOf(initialPulse.toString()) }
    var noteText by remember { mutableStateOf(if (initialPulse != 72) "Pulse measured via Camera PPG" else "") }

    val sys = systolicText.toIntOrNull() ?: 120
    val dia = diastolicText.toIntOrNull() ?: 80
    val classification = ClinicalInsightsEngine.classifyBloodPressure(sys, dia)

    val badgeColor = when (classification.label) {
        "Normal" -> BpNormal
        "Elevated" -> BpElevated
        "Hypertension Stage 1" -> BpStage1
        "Hypertension Stage 2" -> BpStage2
        else -> BpCrisis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Blood Pressure",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Live AHA Staging Preview Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(8.dp)
                                    .background(badgeColor, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = classification.label,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = classification.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = systolicText,
                        onValueChange = { if (it.length <= 3) systolicText = it.filter { c -> c.isDigit() } },
                        label = { Text("Systolic") },
                        placeholder = { Text("120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("systolic_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = diastolicText,
                        onValueChange = { if (it.length <= 3) diastolicText = it.filter { c -> c.isDigit() } },
                        label = { Text("Diastolic") },
                        placeholder = { Text("80") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("diastolic_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pulseText,
                    onValueChange = { if (it.length <= 3) pulseText = it.filter { c -> c.isDigit() } },
                    label = { Text("Pulse (bpm)") },
                    placeholder = { Text("72") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pulse_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. morning resting, after coffee") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bp_note_input"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val systolic = systolicText.toIntOrNull() ?: 120
                    val diastolic = diastolicText.toIntOrNull() ?: 80
                    val pulse = pulseText.toIntOrNull() ?: 70
                    onConfirm(systolic, diastolic, pulse, noteText)
                },
                modifier = Modifier.testTag("save_bp_button")
            ) {
                Text("Save Reading")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_bp_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
