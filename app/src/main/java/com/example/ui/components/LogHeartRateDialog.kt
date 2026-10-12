package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.insights.ClinicalInsightsEngine

@Composable
fun LogHeartRateDialog(
    onDismiss: () -> Unit,
    onConfirm: (bpm: Int, context: String) -> Unit
) {
    var bpmText by remember { mutableStateOf("72") }
    var selectedContext by remember { mutableStateOf("Resting") }
    val contexts = listOf("Resting", "Walking", "Active", "Post-Workout")

    val bpm = bpmText.toIntOrNull() ?: 72
    val hrClass = ClinicalInsightsEngine.classifyHeartRate(bpm)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Heart Rate",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { if (it.length <= 3) bpmText = it.filter { c -> c.isDigit() } },
                    label = { Text("Heart Rate (bpm)") },
                    placeholder = { Text("72") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hr_bpm_input"),
                    singleLine = true
                )

                Text(
                    text = "Status: $hrClass",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text(
                    text = "Measurement Context:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    contexts.forEach { tag ->
                        FilterChip(
                            selected = selectedContext == tag,
                            onClick = { selectedContext = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = bpmText.toIntOrNull() ?: 72
                    onConfirm(rate, selectedContext)
                },
                modifier = Modifier.testTag("save_hr_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_hr_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
