package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ArchitectureBlueprintScreen
import com.example.ui.screens.ImageMatchScreen
import com.example.ui.screens.OsintDetailScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BeaconViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BeaconViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Display snackbars
                LaunchedEffect(uiState.snackbarMessage) {
                    uiState.snackbarMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.dismissSnackbar()
                    }
                }

                // Handle system back button: return to SCANNER if in a subscreen
                BackHandler(enabled = uiState.currentScreen != AppScreen.SCANNER) {
                    viewModel.setScreen(AppScreen.SCANNER)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize().background(DarkBg),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Pulsing status dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isScanning) SignalGreen else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "BEACONLENS",
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 17.sp,
                                        color = CyberCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${uiState.currentScreen.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }
                            },
                            actions = {
                                // Fast TTS audio toggle
                                IconButton(
                                    onClick = {
                                        viewModel.updateTtsSettings(
                                            uiState.ttsSettings.copy(enabled = !uiState.ttsSettings.enabled)
                                        )
                                    },
                                    modifier = Modifier.testTag("appbar_tts_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.ttsSettings.enabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = "Toggle TTS",
                                        tint = if (uiState.ttsSettings.enabled) CyberCyan else TextMuted
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = DarkSurface,
                                titleContentColor = TextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.SCANNER,
                                onClick = { viewModel.setScreen(AppScreen.SCANNER) },
                                icon = { Icon(Icons.Default.Radar, contentDescription = "Scanner") },
                                label = { Text("Scanner", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_scanner")
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.OSINT,
                                onClick = { viewModel.setScreen(AppScreen.OSINT) },
                                icon = { Icon(Icons.Default.Security, contentDescription = "OSINT") },
                                label = { Text("OSINT", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_osint")
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.IMAGE_MATCH,
                                onClick = { viewModel.setScreen(AppScreen.IMAGE_MATCH) },
                                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Image Match") },
                                label = { Text("Match", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_image_match")
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.BLUEPRINT,
                                onClick = { viewModel.setScreen(AppScreen.BLUEPRINT) },
                                icon = { Icon(Icons.Default.AccountTree, contentDescription = "Blueprint") },
                                label = { Text("Blueprint", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_blueprint")
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.SETTINGS,
                                onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            AppScreen.SCANNER -> ScannerScreen(viewModel = viewModel)
                            AppScreen.OSINT -> OsintDetailScreen(viewModel = viewModel)
                            AppScreen.IMAGE_MATCH -> ImageMatchScreen(viewModel = viewModel)
                            AppScreen.BLUEPRINT -> ArchitectureBlueprintScreen(viewModel = viewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
