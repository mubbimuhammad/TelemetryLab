package com.example.telemetrylab.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(
    viewModel: TelemetryViewModel
) {
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val computeLoad by viewModel.computeLoad.collectAsStateWithLifecycle()
    val currentLatency by viewModel.currentLatency.collectAsStateWithLifecycle()
    val averageLatency by viewModel.averageLatency.collectAsStateWithLifecycle()
    val jankPercentage by viewModel.jankPercentage.collectAsStateWithLifecycle()
    val jankFrameCount by viewModel.jankFrameCount.collectAsStateWithLifecycle()
    val isPowerSaveMode by viewModel.isPowerSaveMode.collectAsStateWithLifecycle()
    val counter by viewModel.counter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Telemetry Lab",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (isPowerSaveMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Power-save mode active (10 Hz, Load -1)",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRunning) "Processing Active" else "Processing Stopped",
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                    Switch(
                        checked = isRunning,
                        onCheckedChange = { viewModel.toggleRunning() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Compute Load",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = computeLoad.toInt().toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = computeLoad,
                        onValueChange = { viewModel.updateComputeLoad(it) },
                        valueRange = 1f..5f,
                        steps = 3,
                        enabled = !isPowerSaveMode || !isRunning,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📊 Performance Metrics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    StatsRow(
                        label = "Current Frame",
                        value = "$currentLatency ms",
                        color = when {
                            currentLatency > 50 -> MaterialTheme.colorScheme.error
                            currentLatency > 16 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    StatsRow(
                        label = "Avg Latency",
                        value = "$averageLatency ms",
                        color = MaterialTheme.colorScheme.primary
                    )

                    StatsRow(
                        label = "Jank % (30s)",
                        value = String.format("%.1f%%", jankPercentage),
                        color = when {
                            jankPercentage > 5f -> MaterialTheme.colorScheme.error
                            jankPercentage > 2f -> MaterialTheme.colorScheme.tertiary
                            else -> Color(0xFF4CAF50)
                        }
                    )

                    StatsRow(
                        label = "Jank Frames",
                        value = jankFrameCount.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🔄 Activity Monitor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isRunning) {

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = rememberLazyListState(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items((0..counter).toList().takeLast(50)) { index ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index % 2 == 0)
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            else
                                                Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Frame #$index",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Processing...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Telemetry Stopped",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}