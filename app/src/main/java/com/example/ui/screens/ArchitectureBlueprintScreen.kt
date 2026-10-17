package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BeaconViewModel

@Composable
fun ArchitectureBlueprintScreen(
    viewModel: BeaconViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("System Flow", "Android Layer", "Python API", "CI/CD Pipeline")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYSTEM ARCHITECTURE BLUEPRINT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "End-to-End Data Flow, Component Breakdown & API Schema",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = {
                        val topic = when (selectedTab) {
                            0 -> "System data flow architecture. Android client scans BLE and Wi-Fi, transmits telemetry to Python FastAPI backend, which correlates public OSINT databases."
                            1 -> "Android layer breakdown. Foreground service, BluetoothLeScanner, WifiManager, Jetpack Compose UI, and Google Text to Speech audio synthesis."
                            2 -> "Python backend endpoint design. Health check, OSINT query, multipart image matching, and speech synthesis endpoints."
                            else -> "GitHub Actions automated CI/CD pipeline. Triggers on push, caches Gradle, runs tests, and compiles the Android APK package."
                        }
                        viewModel.ttsManager.speak(topic)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Voice Architecture Walkthrough",
                        tint = CyberCyan
                    )
                }
            }
        }

        // Tab Row
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyberCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) CyberCyan else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // High Level Architecture Diagram
                item {
                    HighLevelFlowSection()
                }
            }
            1 -> {
                // Android Component Breakdown
                item {
                    AndroidComponentSection()
                }
            }
            2 -> {
                // Python Backend Endpoints
                item {
                    PythonBackendSection(context = context, onCopy = { text ->
                        viewModel.showSnackbar("Copied endpoint schema to clipboard")
                    })
                }
            }
            3 -> {
                // CI/CD Pipeline
                item {
                    CiCdSection(context = context, onCopy = {
                        viewModel.showSnackbar("Copied GitHub Actions workflow to clipboard")
                    })
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HighLevelFlowSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DATA FLOW PIPELINE (ARCHITECTURE DIAGRAM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowNode(
                    stepNumber = "1",
                    title = "Radio Emission & Visual Ingestion",
                    subtitle = "Nearby public BLE advertisements (iBeacon/Eddystone) & Wi-Fi Beacons (802.11 frames) + Camera Capture",
                    icon = Icons.Default.PhoneAndroid,
                    color = CyberCyan
                )

                FlowConnector()

                FlowNode(
                    stepNumber = "2",
                    title = "Android Client Layer (BeaconLens)",
                    subtitle = "Foreground Service with BluetoothLeScanner & WifiManager. Path-loss distance estimation. TTS Audio Feedback.",
                    icon = Icons.Default.IntegrationInstructions,
                    color = SignalGreen
                )

                FlowConnector()

                FlowNode(
                    stepNumber = "3",
                    title = "Secure REST Transport (HTTP/2 + TLS)",
                    subtitle = "JSON Payloads for beacon telemetry & Multipart form-data for captured imagery sent to FastAPI daemon",
                    icon = Icons.Default.Http,
                    color = ElectricIndigo
                )

                FlowConnector()

                FlowNode(
                    stepNumber = "4",
                    title = "Python Backend & OSINT Aggregator",
                    subtitle = "OUI IEEE resolution, FCC certification lookup, WiGLE metadata cross-referencing, SHA-256 perceptual hashing",
                    icon = Icons.Default.Storage,
                    color = CyberCyan
                )

                FlowConnector()

                FlowNode(
                    stepNumber = "5",
                    title = "CI/CD Build Automation",
                    subtitle = "GitHub Actions (.github/workflows/build.yml) checks out code, sets up JDK 17, and generates signed APK",
                    icon = Icons.Default.PlayCircle,
                    color = SignalYellow
                )
            }
        }

        // Privacy Guardrails Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXCLUDED & PROTECTED BOUNDARIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SignalGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Zero hidden surveillance: Camera active only upon explicit user trigger.\n• Zero illegal access: Encrypted payloads and private profiles (Instagram, Facebook) are never breached.\n• Strict adherence to open-source public intelligence standards only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun FlowNode(
    stepNumber: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun FlowConnector() {
    Row(
        modifier = Modifier.padding(start = 17.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(DarkSurfaceBorder)
        )
    }
}

@Composable
fun AndroidComponentSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ComponentCard(
            title = "BeaconScannerService",
            type = "Foreground Service (connectedDevice)",
            description = "Runs continuous background radio monitoring with ongoing NotificationCompat notification, preventing Android OS background throttling."
        )

        ComponentCard(
            title = "ScannerManager",
            type = "Radio Hardware Abstraction Layer",
            description = "Interacts with BluetoothLeScanner (ScanCallback) and WifiManager (BroadcastReceiver). Translates RSSI to distance via logarithmic path-loss models."
        )

        ComponentCard(
            title = "TtsManager",
            type = "Google Cloud & Native Speech Engine",
            description = "Initializes com.google.android.tts with natural WaveNet voice packets. Provides hands-free audio announcements on close beacon proximity."
        )

        ComponentCard(
            title = "BackendApiService & BeaconRepository",
            type = "Retrofit 2 + OkHttp 4 Client",
            description = "Multi-part image streaming and JSON REST queries to Python FastAPI backend with local offline intelligence fallback."
        )

        ComponentCard(
            title = "Jetpack Compose UI",
            type = "Material 3 Reactive Layer",
            description = "State-driven HUD radar, dynamic RSSI spectrum meters, zero-permission Photo Picker, and type-safe navigation."
        )
    }
}

@Composable
fun ComponentCard(title: String, type: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, color = CyberCyan, style = MaterialTheme.typography.titleSmall)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricIndigo.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = type,
                        color = ElectricIndigo,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun PythonBackendSection(context: Context, onCopy: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EndpointCard(
            method = "GET",
            route = "/health",
            purpose = "Backend health & daemon status verification",
            payload = """
Response 200:
{
  "status": "healthy",
  "service": "BeaconOSINT Daemon",
  "version": "1.0.0",
  "ethical_mode": true
}
            """.trimIndent(),
            context = context,
            onCopy = onCopy
        )

        EndpointCard(
            method = "POST",
            route = "/api/v1/osint/query",
            purpose = "Correlates public broadcast names/SSIDs against public directories",
            payload = """
Request Body:
{
  "query": "Apple_AirTag_Beacon",
  "target_type": "BLE Device Name",
  "rssi": -48,
  "vendor": "Apple Inc."
}

Response 200:
{
  "query": "Apple_AirTag_Beacon",
  "matches": [
    {
      "platform": "IEEE OUI Registry",
      "handle": "AC:BC:32",
      "confidence": 0.98,
      "status": "Verified Open Directory",
      "details": "Assigned to Apple Inc."
    }
  ],
  "risk_assessment": "High Proximity (<2m)"
}
            """.trimIndent(),
            context = context,
            onCopy = onCopy
        )

        EndpointCard(
            method = "POST",
            route = "/api/v1/osint/image-match",
            purpose = "Receives multipart image file, generates hash, and queries open indices",
            payload = """
Headers:
Content-Type: multipart/form-data

Form Data:
file: <binary image bytes>
metadata: "Conference badge inspection"

Response 200:
{
  "image_hash": "e3b0c44298fc1c149afb...",
  "matched_entities": ["IoT Device Sticker", "Attendee Badge"],
  "similarity_score": 0.88,
  "status_message": "Matched via open source registries"
}
            """.trimIndent(),
            context = context,
            onCopy = onCopy
        )
    }
}

@Composable
fun EndpointCard(
    method: String,
    route: String,
    purpose: String,
    payload: String,
    context: Context,
    onCopy: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (method == "GET") SignalGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = method,
                            fontWeight = FontWeight.Bold,
                            color = if (method == "GET") SignalGreen else CyberCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = route, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = purpose, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = payload,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Endpoint Schema", payload)
                                clipboard.setPrimaryClip(clip)
                                onCopy(payload)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Schema", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun CiCdSection(context: Context, onCopy: () -> Unit) {
    val workflowCode = """
name: Android CI Build

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]

jobs:
  build:
    name: Build Android APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew || true

      - name: Run Unit Tests
        run: gradle :app:testDebugUnitTest

      - name: Build Debug APK
        run: gradle assembleDebug

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: BeaconLens-Debug-APK
          path: app/build/outputs/apk/debug/app-debug.apk
    """.trimIndent()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GITHUB ACTIONS CI/CD (.github/workflows/build.yml)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automated compilation, unit test validation, and APK artifact generation",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("GitHub Actions Workflow", workflowCode)
                        clipboard.setPrimaryClip(clip)
                        onCopy()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Workflow", tint = CyberCyan, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                Text(
                    text = workflowCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
        }
    }
}
