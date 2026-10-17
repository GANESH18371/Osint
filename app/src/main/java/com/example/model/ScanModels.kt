package com.example.model

enum class SignalType {
    BLE,
    WIFI
}

data class BeaconSignal(
    val id: String,
    val name: String,
    val type: SignalType,
    val identifier: String, // MAC or BSSID
    val rssi: Int, // dBm
    val frequency: Int = 0, // MHz (e.g., 2412, 5180)
    val vendor: String = "Generic / Unknown",
    val capabilities: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSimulation: Boolean = false
) {
    val signalPercent: Int
        get() {
            // Map -100 dBm..-30 dBm to 0..100%
            val clamped = rssi.coerceIn(-100, -30)
            return ((clamped + 100) * 100) / 70
        }

    val estimatedDistanceMeters: Double
        get() {
            // Path-loss approximation: d = 10 ^ ((Measured Power - RSSI) / (10 * N))
            val txPower = -59
            if (rssi == 0) return -1.0
            val ratio = (txPower - rssi) / 20.0
            return Math.pow(10.0, ratio).coerceIn(0.1, 50.0)
        }
}

data class OsintProfileMatch(
    val platform: String,
    val handleOrQuery: String,
    val profileUrl: String,
    val confidenceScore: Float,
    val status: String,
    val details: String
)

data class OsintReport(
    val query: String,
    val targetType: String,
    val matches: List<OsintProfileMatch> = emptyList(),
    val riskAssessment: String = "Low",
    val ethicalNotice: String = "Compliant with Open-Source Intelligence (OSINT) guidelines. Only public broadcast identifiers & public APIs queried.",
    val timestamp: Long = System.currentTimeMillis()
)

data class ImageMatchResult(
    val imageHash: String,
    val matchedEntities: List<String> = emptyList(),
    val publicProfileMatches: List<OsintProfileMatch> = emptyList(),
    val similarityScore: Float = 0.0f,
    val statusMessage: String = "Verification completed via public web indices.",
    val ethicalComplianceVerified: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackendStatus(
    val baseUrl: String = "http://10.0.2.2:8000",
    val isOnline: Boolean = false,
    val latencyMs: Long = 0,
    val serverName: String = "BeaconOSINT FastAPI Daemon",
    val lastChecked: Long = 0
)

data class TtsSettings(
    val enabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val proximityAlertThreshold: Int = -60, // dBm
    val announceScanEvents: Boolean = true
)
