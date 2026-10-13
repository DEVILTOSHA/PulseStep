package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BloodPressureEntry
import com.example.data.local.HeartRateEntry
import com.example.ui.components.BloodPressureChart
import com.example.ui.components.HeartRateTrendChart
import com.example.ui.theme.BpCrisis
import com.example.ui.theme.BpElevated
import com.example.ui.theme.BpNormal
import com.example.ui.theme.BpStage1
import com.example.ui.theme.BpStage2
import com.example.ui.theme.CoralHeart
import com.example.ui.theme.TealAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VitalsScreen(
    latestBp: BloodPressureEntry?,
    allBpLogs: List<BloodPressureEntry>,
    latestHr: HeartRateEntry?,
    allHrLogs: List<HeartRateEntry>,
    onOpenLogBpDialog: () -> Unit,
    onDeleteBpEntry: (Long) -> Unit,
    onOpenLogHrDialog: () -> Unit,
    onDeleteHrEntry: (Long) -> Unit,
    onOpenPpgScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Blood Pressure", "Heart Rate")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (selectedTab == 0) {
            // Blood Pressure Tab
            BloodPressureTabContent(
                latestBp = latestBp,
                allBpLogs = allBpLogs,
                onOpenLogDialog = onOpenLogBpDialog,
                onOpenPpgScanner = onOpenPpgScanner,
                onDeleteEntry = onDeleteBpEntry
            )
        } else {
            // Heart Rate Tab
            HeartRateTabContent(
                latestHr = latestHr,
                allHrLogs = allHrLogs,
                onOpenLogDialog = onOpenLogHrDialog,
                onOpenPpgScanner = onOpenPpgScanner,
                onDeleteEntry = onDeleteHrEntry
            )
        }
    }
}

@Composable
private fun BloodPressureTabContent(
    latestBp: BloodPressureEntry?,
    allBpLogs: List<BloodPressureEntry>,
    onOpenLogDialog: () -> Unit,
    onOpenPpgScanner: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Latest Reading
        item {
            val bpColor = when (latestBp?.category) {
                "Normal" -> BpNormal
                "Elevated" -> BpElevated
                "Stage 1", "Hypertension Stage 1" -> BpStage1
                "Stage 2", "Hypertension Stage 2" -> BpStage2
                else -> BpCrisis
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("latest_bp_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonitorHeart,
                                contentDescription = "BP",
                                tint = CoralHeart
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Reading",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bpColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = latestBp?.category ?: "Normal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = bpColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (latestBp != null) "${latestBp.systolic}/${latestBp.diastolic}" else "--/--",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " mmHg",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        if (latestBp != null && latestBp.pulse > 0) {
                            Text(
                                text = "• Pulse ${latestBp.pulse} bpm",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = CoralHeart,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    if (latestBp?.note?.isNotEmpty() == true) {
                        Text(
                            text = "Note: ${latestBp.note}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons: Log Reading & Camera PPG Pulse Scan
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenLogDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("log_bp_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralHeart),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Cuff Reading (BP + Pulse)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenPpgScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("bp_scan_pulse_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "PPG Scan", tint = CoralHeart)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Pulse First (Camera PPG)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Blood Pressure Chart
        item {
            BloodPressureChart(entries = allBpLogs)
        }

        // AHA Guidelines Reference Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = TealAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "AHA Blood Pressure Stages", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AhaRow("Normal", "< 120 and < 80 mmHg", BpNormal)
                    AhaRow("Elevated", "120–129 and < 80 mmHg", BpElevated)
                    AhaRow("Stage 1", "130–139 or 80–89 mmHg", BpStage1)
                    AhaRow("Stage 2", "≥ 140 or ≥ 90 mmHg", BpStage2)
                }
            }
        }

        // History Entries
        item {
            Text(
                text = "Log History (${allBpLogs.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (allBpLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Blood Pressure Readings Logged",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Log Cuff Reading' or 'Scan Pulse First' above to log your first cardiovascular measurement.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(allBpLogs) { entry ->
            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(entry.timestamp))
            val badgeColor = when (entry.category) {
                "Normal" -> BpNormal
                "Elevated" -> BpElevated
                "Stage 1", "Hypertension Stage 1" -> BpStage1
                "Stage 2", "Hypertension Stage 2" -> BpStage2
                else -> BpCrisis
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${entry.systolic}/${entry.diastolic} mmHg",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = entry.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$dateStr • Pulse: ${entry.pulse} bpm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entry.note.isNotEmpty()) {
                            Text(
                                text = entry.note,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDeleteEntry(entry.id) },
                        modifier = Modifier.testTag("delete_bp_${entry.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun HeartRateTabContent(
    latestHr: HeartRateEntry?,
    allHrLogs: List<HeartRateEntry>,
    onOpenLogDialog: () -> Unit,
    onOpenPpgScanner: () -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("latest_hr_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "HR", tint = CoralHeart)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Latest Heart Rate", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = latestHr?.contextTag ?: "Resting",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (latestHr != null) "${latestHr.bpm}" else "--",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " bpm",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons: Camera PPG Scan & Manual Entry
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenPpgScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("scan_pulse_camera_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralHeart),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Camera Flash PPG", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Pulse with Camera (PPG)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onOpenLogDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("log_hr_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual Heart Rate Entry", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Heart Rate Trend Chart
        item {
            HeartRateTrendChart(entries = allHrLogs)
        }

        // History
        item {
            Text(
                text = "Log History (${allHrLogs.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (allHrLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Heart Rate Readings Logged",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Scan Pulse (Camera PPG)' or 'Log Heart Rate' above to record your first pulse check.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(allHrLogs) { entry ->
            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(entry.timestamp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${entry.bpm} bpm",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TealAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = entry.contextTag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(text = dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = { onDeleteEntry(entry.id) },
                        modifier = Modifier.testTag("delete_hr_${entry.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AhaRow(label: String, range: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
        Text(text = range, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
