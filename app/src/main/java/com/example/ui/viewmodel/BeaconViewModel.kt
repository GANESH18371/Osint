package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BeaconRepository
import com.example.model.BackendStatus
import com.example.model.BeaconSignal
import com.example.model.ImageMatchResult
import com.example.model.OsintReport
import com.example.model.SignalType
import com.example.model.TtsSettings
import com.example.service.BeaconScannerService
import com.example.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    SCANNER,
    OSINT,
    IMAGE_MATCH,
    BLUEPRINT,
    SETTINGS
}

enum class SignalFilterType {
    ALL,
    BLE_ONLY,
    WIFI_ONLY
}

enum class SortOption {
    RSSI_DESCENDING,
    NAME_ASCENDING,
    RECENT
}

data class ScannerUiState(
    val currentScreen: AppScreen = AppScreen.SCANNER,
    val filterType: SignalFilterType = SignalFilterType.ALL,
    val searchQuery: String = "",
    val minRssi: Int = -95,
    val sortOption: SortOption = SortOption.RSSI_DESCENDING,
    val isForegroundServiceActive: Boolean = false,
    val selectedSignal: BeaconSignal? = null,
    val isOsintLoading: Boolean = false,
    val activeReport: OsintReport? = null,
    val capturedImage: Bitmap? = null,
    val isImageAnalyzing: Boolean = false,
    val imageMatchResult: ImageMatchResult? = null,
    val ttsSettings: TtsSettings = TtsSettings(),
    val backendUrl: String = "http://10.0.2.2:8000",
    val snackbarMessage: String? = null
)

class BeaconViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BeaconRepository(application)
    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val rawSignals = repository.scannerManager.signals
    val isScanning = repository.scannerManager.isScanning
    val simulationMode = repository.scannerManager.simulationMode
    val hardwareStatus = repository.scannerManager.hardwareStatus
    val backendStatus: StateFlow<BackendStatus> = repository.backendStatus
    val recentReports = repository.recentReports

    // Filtered & sorted signals stream
    val filteredSignals: StateFlow<List<BeaconSignal>> = combine(
        rawSignals,
        _uiState
    ) { signals, state ->
        signals.asSequence()
            .filter { signal ->
                when (state.filterType) {
                    SignalFilterType.ALL -> true
                    SignalFilterType.BLE_ONLY -> signal.type == SignalType.BLE
                    SignalFilterType.WIFI_ONLY -> signal.type == SignalType.WIFI
                }
            }
            .filter { signal ->
                signal.rssi >= state.minRssi
            }
            .filter { signal ->
                if (state.searchQuery.isBlank()) true
                else {
                    signal.name.contains(state.searchQuery, ignoreCase = true) ||
                            signal.identifier.contains(state.searchQuery, ignoreCase = true) ||
                            signal.vendor.contains(state.searchQuery, ignoreCase = true)
                }
            }
            .sortedWith(
                when (state.sortOption) {
                    SortOption.RSSI_DESCENDING -> compareByDescending { it.rssi }
                    SortOption.NAME_ASCENDING -> compareBy { it.name.lowercase() }
                    SortOption.RECENT -> compareByDescending { it.timestamp }
                }
            )
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial backend health ping
        testBackendConnection(_uiState.value.backendUrl)
    }

    fun setScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setFilterType(type: SignalFilterType) {
        _uiState.update { it.copy(filterType = type) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setMinRssi(rssi: Int) {
        _uiState.update { it.copy(minRssi = rssi) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun toggleScanning() {
        val currentlyScanning = isScanning.value
        if (currentlyScanning) {
            repository.scannerManager.stopScan()
            stopForegroundService()
            ttsManager.speak("Scanning halted.")
        } else {
            repository.scannerManager.startScan { signal ->
                if (signal.rssi > _uiState.value.ttsSettings.proximityAlertThreshold) {
                    ttsManager.announceProximityAlert(signal.name, signal.rssi)
                }
            }
            ttsManager.announceScanStarted()
        }
    }

    fun toggleForegroundService(enable: Boolean) {
        _uiState.update { it.copy(isForegroundServiceActive = enable) }
        val context = getApplication<Application>()
        val intent = Intent(context, BeaconScannerService::class.java).apply {
            action = if (enable) BeaconScannerService.ACTION_START else BeaconScannerService.ACTION_STOP
        }

        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            showSnackbar("Foreground monitoring service started")
            ttsManager.speak("Foreground monitoring service engaged.")
        } else {
            context.stopService(intent)
            showSnackbar("Foreground service stopped")
        }
    }

    private fun stopForegroundService() {
        if (_uiState.value.isForegroundServiceActive) {
            toggleForegroundService(false)
        }
    }

    fun toggleSimulation(enable: Boolean) {
        repository.scannerManager.toggleSimulation(enable)
        showSnackbar(if (enable) "Radio simulation enabled" else "Simulation disabled (Hardware only)")
    }

    fun clearSignals() {
        repository.scannerManager.clearSignals()
        showSnackbar("Signals list cleared")
    }

    fun selectSignalForOsint(signal: BeaconSignal) {
        _uiState.update { it.copy(selectedSignal = signal, currentScreen = AppScreen.OSINT) }
        runOsintInvestigation(signal.name, if (signal.type == SignalType.BLE) "BLE Device Name" else "Wi-Fi SSID", signal)
    }

    fun runOsintInvestigation(query: String, targetType: String, signal: BeaconSignal? = null) {
        if (query.isBlank()) return
        _uiState.update { it.copy(isOsintLoading = true) }

        viewModelScope.launch {
            ttsManager.speak("Initiating Open Source Intelligence correlation for $query")
            val report = repository.runOsintQuery(query, targetType, signal)
            _uiState.update {
                it.copy(
                    isOsintLoading = false,
                    activeReport = report
                )
            }
            ttsManager.announceOsintCompleted(query, report.matches.size)
        }
    }

    fun setImageForAnalysis(bitmap: Bitmap) {
        _uiState.update { it.copy(capturedImage = bitmap, imageMatchResult = null) }
    }

    fun analyzeCapturedImage(notes: String = "") {
        val bitmap = _uiState.value.capturedImage ?: return
        _uiState.update { it.copy(isImageAnalyzing = true) }

        viewModelScope.launch {
            ttsManager.speak("Uploading image and querying public image indices.")
            val result = repository.analyzeImage(bitmap, notes)
            _uiState.update {
                it.copy(
                    isImageAnalyzing = false,
                    imageMatchResult = result
                )
            }
            ttsManager.announceImageMatchCompleted(result.publicProfileMatches.size)
        }
    }

    fun clearImage() {
        _uiState.update { it.copy(capturedImage = null, imageMatchResult = null) }
    }

    fun updateTtsSettings(newSettings: TtsSettings) {
        _uiState.update { it.copy(ttsSettings = newSettings) }
        ttsManager.updateSettings(newSettings)
    }

    fun testTtsVoice() {
        ttsManager.speak("BeaconLens voice feedback is active and functioning at optimal speech synthesis parameters.")
    }

    fun updateBackendUrl(url: String) {
        _uiState.update { it.copy(backendUrl = url) }
        testBackendConnection(url)
    }

    fun testBackendConnection(url: String) {
        viewModelScope.launch {
            val status = repository.checkBackendConnection(url)
            if (status.isOnline) {
                showSnackbar("Connected to ${status.serverName} (${status.latencyMs}ms)")
                ttsManager.speak("Connected to Python backend daemon.")
            } else {
                showSnackbar("Backend unavailable. Local intelligence fallback active.")
            }
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.scannerManager.stopScan()
        ttsManager.shutdown()
    }
}
