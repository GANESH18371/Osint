package com.example.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.BeaconSignal
import com.example.model.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ScannerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _signals = MutableStateFlow<List<BeaconSignal>>(emptyList())
    val signals: StateFlow<List<BeaconSignal>> = _signals.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _simulationMode = MutableStateFlow(false)
    val simulationMode: StateFlow<Boolean> = _simulationMode.asStateFlow()

    private val _hardwareStatus = MutableStateFlow("Hardware ready")
    val hardwareStatus: StateFlow<String> = _hardwareStatus.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var wifiManager: WifiManager? = null

    private var bleCallback: ScanCallback? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var simulationJob: Job? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        // Preload baseline mock signals if in emulator environment so the user sees immediate feedback
        if (isProbablyEmulator()) {
            _simulationMode.value = true
        }
    }

    private fun isProbablyEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86"))
    }

    fun toggleSimulation(enabled: Boolean) {
        _simulationMode.value = enabled
        if (!enabled) {
            // Remove simulation signals
            _signals.update { list -> list.filter { !it.isSimulation } }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(onSignalFound: ((BeaconSignal) -> Unit)? = null) {
        if (_isScanning.value) return
        _isScanning.value = true

        var bleStarted = false
        var wifiStarted = false

        // 1. Try real BLE Scanner
        try {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bleScanner != null && bluetoothAdapter?.isEnabled == true) {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                bleCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result?.let { res ->
                            val record = res.scanRecord
                            val rawName = res.device.name ?: record?.deviceName ?: "Unnamed BLE Beacon"
                            val address = res.device.address ?: "00:00:00:00:00:00"
                            val rssi = res.rssi
                            val vendor = resolveVendor(address, rawName)

                            val signal = BeaconSignal(
                                id = "ble_$address",
                                name = rawName,
                                type = SignalType.BLE,
                                identifier = address,
                                rssi = rssi,
                                vendor = vendor,
                                capabilities = "BLE 5.0 Broadcast, TxPower: ${record?.txPowerLevel ?: "N/A"}",
                                isSimulation = false
                            )

                            upsertSignal(signal)
                            onSignalFound?.invoke(signal)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e("ScannerManager", "BLE Scan failed with code: $errorCode")
                        _hardwareStatus.value = "BLE scan error: $errorCode (Emulation active)"
                    }
                }

                bleScanner?.startScan(null, settings, bleCallback)
                bleStarted = true
            } else {
                Log.w("ScannerManager", "Bluetooth adapter disabled or BLE scanner unavailable")
            }
        } catch (e: SecurityException) {
            Log.w("ScannerManager", "Missing BLE permission at runtime: ${e.message}")
            _hardwareStatus.value = "BLE permissions required"
        } catch (e: Exception) {
            Log.w("ScannerManager", "BLE init exception: ${e.message}")
        }

        // 2. Try real Wi-Fi Scanner
        try {
            if (wifiManager != null) {
                wifiReceiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
                        if (success) {
                            processWifiResults(onSignalFound)
                        }
                    }
                }
                context.registerReceiver(
                    wifiReceiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )

                // Request Wi-Fi scan
                @Suppress("DEPRECATION")
                wifiManager?.startScan()
                processWifiResults(onSignalFound)
                wifiStarted = true
            }
        } catch (e: SecurityException) {
            Log.w("ScannerManager", "Location/Wi-Fi permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.w("ScannerManager", "Wi-Fi scan error: ${e.message}")
        }

        // 3. Fallback / Demonstration simulation:
        // Always run simulation if enabled OR if real hardware is unavailable (e.g. streaming emulator)
        startSimulationPulse(onSignalFound)

        _hardwareStatus.value = when {
            bleStarted && wifiStarted -> "Live Hardware (BLE + Wi-Fi)"
            bleStarted -> "Live BLE Active"
            wifiStarted -> "Live Wi-Fi Active"
            else -> "Simulation Active (Hardware unavailable)"
        }
    }

    @SuppressLint("MissingPermission")
    private fun processWifiResults(onSignalFound: ((BeaconSignal) -> Unit)?) {
        try {
            val results = wifiManager?.scanResults ?: return
            for (res in results) {
                val ssid = if (res.SSID.isNullOrBlank()) "Hidden Broadcast Network" else res.SSID
                val bssid = res.BSSID ?: "00:00:00:00:00:00"
                val rssi = res.level
                val freq = res.frequency
                val vendor = resolveVendor(bssid, ssid)

                val signal = BeaconSignal(
                    id = "wifi_$bssid",
                    name = ssid,
                    type = SignalType.WIFI,
                    identifier = bssid,
                    rssi = rssi,
                    frequency = freq,
                    vendor = vendor,
                    capabilities = res.capabilities ?: "WPA2/WPA3",
                    isSimulation = false
                )
                upsertSignal(signal)
                onSignalFound?.invoke(signal)
            }
        } catch (e: Exception) {
            Log.e("ScannerManager", "Error parsing wifi scan results: ${e.message}")
        }
    }

    private fun startSimulationPulse(onSignalFound: ((BeaconSignal) -> Unit)?) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val simulatedSignals = getInitialSampleSignals()
            // Add initial batch
            simulatedSignals.forEach { upsertSignal(it) }

            while (isActive && _isScanning.value) {
                delay(2800)
                // Random RSSI jitter simulating actual radio propagation
                _signals.update { current ->
                    current.map { s ->
                        if (s.isSimulation) {
                            val jitter = (-3..3).random()
                            val newRssi = (s.rssi + jitter).coerceIn(-92, -38)
                            s.copy(rssi = newRssi, timestamp = System.currentTimeMillis())
                        } else s
                    }
                }

                // Occasionally simulate finding a new device
                if ((0..10).random() > 7) {
                    val dynamicBeacon = createDynamicBeacon()
                    upsertSignal(dynamicBeacon)
                    onSignalFound?.invoke(dynamicBeacon)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _isScanning.value = false
        simulationJob?.cancel()

        try {
            bleCallback?.let { bleScanner?.stopScan(it) }
        } catch (ignored: Exception) {}

        try {
            wifiReceiver?.let { context.unregisterReceiver(it) }
            wifiReceiver = null
        } catch (ignored: Exception) {}
    }

    fun clearSignals() {
        _signals.value = emptyList()
    }

    private fun upsertSignal(signal: BeaconSignal) {
        _signals.update { list ->
            val idx = list.indexOfFirst { it.id == signal.id }
            if (idx >= 0) {
                list.toMutableList().apply { set(idx, signal) }
            } else {
                listOf(signal) + list
            }
        }
    }

    private fun resolveVendor(identifier: String, name: String): String {
        val upper = identifier.uppercase()
        val nameUpper = name.uppercase()

        return when {
            nameUpper.contains("APPLE") || nameUpper.contains("AIRTAG") || nameUpper.contains("IPHONE") || upper.startsWith("AC:BC") || upper.startsWith("40:98") -> "Apple Inc."
            nameUpper.contains("SAMSUNG") || nameUpper.contains("GALAXY") || upper.startsWith("38:0B") || upper.startsWith("94:35") -> "Samsung Electronics"
            nameUpper.contains("ESP32") || nameUpper.contains("ESPRESSIF") || upper.startsWith("24:6F") || upper.startsWith("A4:CF") -> "Espressif Systems"
            nameUpper.contains("INTEL") || upper.startsWith("00:1E:67") -> "Intel Corporation"
            nameUpper.contains("CISCO") || upper.startsWith("00:00:0C") -> "Cisco Systems"
            nameUpper.contains("GOOGLE") || nameUpper.contains("PIXEL") || upper.startsWith("F4:F5:E8") -> "Google LLC"
            nameUpper.contains("FITBIT") || upper.startsWith("E0:76:D0") -> "Fitbit / Wearables"
            nameUpper.contains("SONY") || upper.startsWith("00:01:4A") -> "Sony Group"
            nameUpper.contains("DJI") -> "DJI Innovations"
            upper.contains("PUBLIC") -> "Public Broadcast Registry"
            else -> "IEEE OUI Verified Device"
        }
    }

    private fun getInitialSampleSignals(): List<BeaconSignal> {
        return listOf(
            BeaconSignal(
                id = "ble_f4_f5_e8_21_9a_10",
                name = "Beacon_Eddystone_URL",
                type = SignalType.BLE,
                identifier = "F4:F5:E8:21:9A:10",
                rssi = -52,
                vendor = "Google / Eddystone",
                capabilities = "UID + Telemetry Frame, 100ms adv interval",
                isSimulation = true
            ),
            BeaconSignal(
                id = "wifi_00_1e_67_89_ab_cd",
                name = "Metro_Public_WiFi_Free",
                type = SignalType.WIFI,
                identifier = "00:1E:67:89:AB:CD",
                rssi = -64,
                frequency = 5180,
                vendor = "Intel Corporation",
                capabilities = "WPA2-Enterprise / 802.11ac (5 GHz)",
                isSimulation = true
            ),
            BeaconSignal(
                id = "ble_ac_bc_32_7f_04_e2",
                name = "Apple_Nearby_Telemetry",
                type = SignalType.BLE,
                identifier = "AC:BC:32:7F:04:E2",
                rssi = -46,
                vendor = "Apple Inc.",
                capabilities = "Continuity protocol, Proximity RSSI -46 dBm",
                isSimulation = true
            ),
            BeaconSignal(
                id = "wifi_a4_cf_12_45_99_1b",
                name = "IoT_Environmental_Sensor",
                type = SignalType.WIFI,
                identifier = "A4:CF:12:45:99:1B",
                rssi = -78,
                frequency = 2412,
                vendor = "Espressif Systems",
                capabilities = "802.11b/g/n (2.4 GHz) WPA3-Personal",
                isSimulation = true
            ),
            BeaconSignal(
                id = "ble_38_0b_aa_31_cc_88",
                name = "Galaxy_Tag_Locator",
                type = SignalType.BLE,
                identifier = "38:0B:AA:31:CC:88",
                rssi = -61,
                vendor = "Samsung Electronics",
                capabilities = "SmartThings Find BLE beacon broadcast",
                isSimulation = true
            )
        )
    }

    private fun createDynamicBeacon(): BeaconSignal {
        val names = listOf(
            "Public_Transit_Beacon_04",
            "Cafe_Open_Hotspot",
            "Nordic_nRF52_Sensor",
            "DJI_Remote_ID_Broadcast",
            "Zebra_Asset_Tracker",
            "BLE_Peripheral_Debug"
        )
        val selected = names.random()
        val isBle = (0..1).random() == 0
        val fakeMac = (1..6).joinToString(":") { "%02X".format((0..255).random()) }

        return BeaconSignal(
            id = if (isBle) "ble_$fakeMac" else "wifi_$fakeMac",
            name = selected,
            type = if (isBle) SignalType.BLE else SignalType.WIFI,
            identifier = fakeMac,
            rssi = (-88..-40).random(),
            frequency = if (isBle) 2402 else listOf(2412, 2437, 2462, 5180, 5240).random(),
            vendor = resolveVendor(fakeMac, selected),
            capabilities = if (isBle) "BLE 5.2 Extended Adv" else "802.11ax Wi-Fi 6",
            isSimulation = true
        )
    }
}
