package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BeaconSignal
import com.example.model.SignalType
import com.example.ui.components.RadarView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.SignalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BeaconViewModel
import com.example.ui.viewmodel.SignalFilterType
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: BeaconViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredSignals by viewModel.filteredSignals.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val simulationMode by viewModel.simulationMode.collectAsState()
    val hardwareStatus by viewModel.hardwareStatus.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Radar & Metric HUD Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radar_hud_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SIGNAL SPECTRUM RADAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = hardwareStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        // Simulation mode pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (simulationMode) ElectricIndigo.copy(alpha = 0.2f) else DarkSurface,
                            border = BorderStroke(1.dp, if (simulationMode) ElectricIndigo else DarkSurfaceBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.toggleSimulation(!simulationMode) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (simulationMode) ElectricIndigo else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (simulationMode) "Simulation ON" else "Hardware Only",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Radar Graphic
                    RadarView(
                        signals = filteredSignals,
                        isScanning = isScanning,
                        onSignalClick = { viewModel.selectSignalForOsint(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metrics Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricItem(
                            label = "TOTAL",
                            value = "${filteredSignals.size}",
                            color = TextPrimary
                        )
                        MetricItem(
                            label = "BLE",
                            value = "${filteredSignals.count { it.type == SignalType.BLE }}",
                            color = CyberCyan
                        )
                        MetricItem(
                            label = "WI-FI",
                            value = "${filteredSignals.count { it.type == SignalType.WIFI }}",
                            color = ElectricIndigo
                        )
                        val topRssi = filteredSignals.maxOfOrNull { it.rssi }
                        MetricItem(
                            label = "TOP SIGNAL",
                            value = if (topRssi != null) "$topRssi dBm" else "--",
                            color = if (topRssi != null && topRssi > -60) SignalGreen else SignalYellow
                        )
                    }
                }
            }
        }

        // Primary Controls Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Big Scan Toggle Button
                val buttonBg by animateColorAsState(
                    targetValue = if (isScanning) SignalRed else CyberCyan,
                    label = "ScanBtnColor"
                )

                Button(
                    onClick = { viewModel.toggleScanning() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("scan_toggle_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "STOP SCAN" else "START SCAN",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Filter toggle
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                        .testTag("filter_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (showFilters) CyberCyan else TextSecondary
                    )
                }

                // Speak Summary
                IconButton(
                    onClick = {
                        val bleCount = filteredSignals.count { it.type == SignalType.BLE }
                        val wifiCount = filteredSignals.count { it.type == SignalType.WIFI }
                        viewModel.ttsManager.announceScanResults(bleCount, wifiCount)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                        .testTag("tts_announce_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Voice Announcement",
                        tint = CyberCyan
                    )
                }

                // Clear List
                IconButton(
                    onClick = { viewModel.clearSignals() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                        .testTag("clear_signals_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Signals",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Foreground Service Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("foreground_service_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Foreground Background Service",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.isForegroundServiceActive) "Running persistent scan with ongoing notification" else "Service idle (stops when app closes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isForegroundServiceActive) SignalGreen else TextMuted
                        )
                    }

                    Switch(
                        checked = uiState.isForegroundServiceActive,
                        onCheckedChange = { viewModel.toggleForegroundService(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("foreground_switch")
                    )
                }
            }
        }

        // Expandable Filter & Search section
        item {
            AnimatedVisibility(visible = showFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Search bar
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field"),
                            placeholder = { Text("Search name, MAC, or vendor...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Type filter chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Type:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            FilterChip(
                                selected = uiState.filterType == SignalFilterType.ALL,
                                onClick = { viewModel.setFilterType(SignalFilterType.ALL) },
                                label = { Text("All") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                            FilterChip(
                                selected = uiState.filterType == SignalFilterType.BLE_ONLY,
                                onClick = { viewModel.setFilterType(SignalFilterType.BLE_ONLY) },
                                label = { Text("BLE") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                            FilterChip(
                                selected = uiState.filterType == SignalFilterType.WIFI_ONLY,
                                onClick = { viewModel.setFilterType(SignalFilterType.WIFI_ONLY) },
                                label = { Text("Wi-Fi") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // RSSI threshold slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Minimum Signal Filter", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text("${uiState.minRssi} dBm", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                        }
                        Slider(
                            value = uiState.minRssi.toFloat(),
                            onValueChange = { viewModel.setMinRssi(it.toInt()) },
                            valueRange = -100f..-40f,
                            steps = 11,
                            modifier = Modifier.fillMaxWidth().testTag("rssi_slider")
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETECTED BROADCAST IDENTIFIERS (${filteredSignals.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Quick Sort dropdown/toggle
                Text(
                    text = when (uiState.sortOption) {
                        SortOption.RSSI_DESCENDING -> "Sort: RSSI"
                        SortOption.NAME_ASCENDING -> "Sort: Name"
                        SortOption.RECENT -> "Sort: Recent"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    modifier = Modifier.clickable {
                        val next = when (uiState.sortOption) {
                            SortOption.RSSI_DESCENDING -> SortOption.NAME_ASCENDING
                            SortOption.NAME_ASCENDING -> SortOption.RECENT
                            SortOption.RECENT -> SortOption.RSSI_DESCENDING
                        }
                        viewModel.setSortOption(next)
                    }
                )
            }
        }

        if (filteredSignals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isScanning) "Listening for broadcast radio frames..." else "Scanner is paused",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Start Scan' or enable Simulation mode to discover signals",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredSignals, key = { it.id }) { signal ->
                SignalCard(
                    signal = signal,
                    onInspectOsint = { viewModel.selectSignalForOsint(signal) },
                    onSpeak = {
                        val distanceStr = if (signal.estimatedDistanceMeters > 0) {
                            String.format("%.1f meters", signal.estimatedDistanceMeters)
                        } else "unknown distance"
                        viewModel.ttsManager.speak("Device ${signal.name}, ${signal.type.name} broadcast at ${signal.rssi} d-B-m, estimated $distanceStr")
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SignalCard(
    signal: BeaconSignal,
    onInspectOsint: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("signal_card_${signal.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type Badge + Name + RSSI Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Type Badge Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (signal.type == SignalType.BLE) CyberCyan.copy(alpha = 0.15f) else ElectricIndigo.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (signal.type == SignalType.BLE) Icons.Default.Bluetooth else Icons.Default.Wifi,
                            contentDescription = signal.type.name,
                            tint = if (signal.type == SignalType.BLE) CyberCyan else ElectricIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = signal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = signal.identifier,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // RSSI badge
                val rssiColor = when {
                    signal.rssi >= -60 -> SignalGreen
                    signal.rssi >= -75 -> SignalYellow
                    else -> SignalRed
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rssiColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, rssiColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${signal.rssi} dBm",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = rssiColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signal bar meter & Distance estimate
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { signal.signalPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (signal.rssi >= -65) SignalGreen else CyberCyan,
                    trackColor = DarkSurfaceBorder
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (signal.estimatedDistanceMeters > 0) "~${String.format("%.1f", signal.estimatedDistanceMeters)}m" else "N/A",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vendor & Capabilities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vendor: ${signal.vendor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (signal.frequency > 0) {
                    Text(
                        text = "${signal.frequency} MHz",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: OSINT Inspect & Voice readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInspectOsint,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("inspect_osint_button_${signal.id}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OSINT Inspect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .size(36.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak Telemetry",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}
