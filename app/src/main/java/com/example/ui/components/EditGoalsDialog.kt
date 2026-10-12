package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditGoalsDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSaveGoal: (newGoal: Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    val presets = listOf(6000, 8000, 10000, 12000, 15000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Daily Step Goal",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Consistent daily goals help build sustainable walking habits and lower cardiovascular risk.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = goalText,
                    onValueChange = { if (it.length <= 6) goalText = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily Steps Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step_goal_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = goalText == preset.toString(),
                            onClick = { goalText = preset.toString() },
                            label = { Text("%,d".format(Locale.US, preset), fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = goalText.toIntOrNull() ?: currentGoal
                    onSaveGoal(parsed.coerceIn(1000, 50000))
                },
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text("Update Goal")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_goal_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
