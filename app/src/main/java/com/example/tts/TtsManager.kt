package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.example.model.TtsSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String?>(null)
    val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

    private var settings: TtsSettings = TtsSettings()

    init {
        // Attempt initialization with Google TTS engine if available for natural sounding speech
        try {
            textToSpeech = TextToSpeech(context, this, "com.google.android.tts")
        } catch (e: Exception) {
            Log.w("TtsManager", "Google TTS engine not available directly, using default: ${e.message}")
            textToSpeech = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "US English language not supported for TTS, falling back to default locale")
                textToSpeech?.setLanguage(Locale.getDefault())
            }

            // Pick a smooth natural voice if Google TTS voice packs are installed
            try {
                val voices = textToSpeech?.voices
                val naturalVoice = voices?.firstOrNull { voice: Voice ->
                    voice.locale.language == Locale.ENGLISH.language &&
                            (voice.name.contains("en-us-x", ignoreCase = true) || voice.name.contains("wavenet", ignoreCase = true))
                }
                if (naturalVoice != null) {
                    textToSpeech?.voice = naturalVoice
                }
            } catch (ignored: Exception) {
            }

            applySettings(settings)
            _isReady.value = true
            Log.d("TtsManager", "TTS Engine initialized successfully")
        } else {
            Log.e("TtsManager", "TTS initialization failed with status $status")
            _isReady.value = false
        }
    }

    fun updateSettings(newSettings: TtsSettings) {
        settings = newSettings
        applySettings(newSettings)
    }

    private fun applySettings(s: TtsSettings) {
        textToSpeech?.apply {
            setSpeechRate(s.speechRate)
            setPitch(s.pitch)
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!settings.enabled) return
        if (!_isReady.value) {
            Log.w("TtsManager", "TTS not ready yet. Queued message was: $text")
            return
        }

        _lastSpokenText.value = text
        textToSpeech?.speak(text, queueMode, null, "UTTERANCE_ID_${System.currentTimeMillis()}")
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        _isReady.value = false
    }

    // High level speech events
    fun announceScanStarted() {
        if (settings.announceScanEvents) {
            speak("Scanning active. Listening for nearby BLE beacons and public Wi-Fi broadcast identifiers.")
        }
    }

    fun announceScanResults(bleCount: Int, wifiCount: Int) {
        if (settings.announceScanEvents) {
            val speech = "Scan cycle updated. Detected $bleCount Bluetooth beacons and $wifiCount Wi-Fi networks."
            speak(speech)
        }
    }

    fun announceProximityAlert(name: String, rssi: Int) {
        if (rssi >= settings.proximityAlertThreshold) {
            speak("Proximity alert. Device $name is within close range, signal strength $rssi d-B-m.")
        }
    }

    fun announceOsintCompleted(query: String, matchCount: Int) {
        speak("OSINT aggregation complete for $query. Found $matchCount public intelligence references.")
    }

    fun announceImageMatchCompleted(matchesCount: Int) {
        speak("Image verification completed. $matchesCount public records correlated.")
    }
}
