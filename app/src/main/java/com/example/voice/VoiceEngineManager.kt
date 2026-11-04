package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "VoiceEngineManager"

class VoiceEngineManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var continuousSessionActive = false

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val kurdishLocales = listOf(
                        Locale.forLanguageTag("ckb-IQ"),
                        Locale.forLanguageTag("ckb"),
                        Locale.forLanguageTag("ku"),
                        Locale.forLanguageTag("ar-IQ"),
                        Locale.forLanguageTag("fa-IR"),
                        Locale.getDefault()
                    )
                    for (loc in kurdishLocales) {
                        val res = engine.setLanguage(loc)
                        if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                            break
                        }
                    }
                    engine.setPitch(1.0f)
                    engine.setSpeechRate(0.96f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            if (continuousSessionActive) {
                                mainHandler.postDelayed({ startListening() }, 400L)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                    isTtsReady = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS init failed", e)
        }
    }

    fun speakKurdish(text: String) {
        if (text.isBlank()) return
        mainHandler.post {
            _isSpeaking.value = true
            if (isTtsReady && tts != null) {
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_utt_${System.currentTimeMillis()}")
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "jarvis_utt_${System.currentTimeMillis()}")
                // Fallback timer in case emulator TTS engine doesn't fire onDone
                val estimatedMs = (text.length * 65L).coerceIn(1800L, 6000L)
                mainHandler.postDelayed({
                    _isSpeaking.value = false
                }, estimatedMs)
            } else {
                val estimatedMs = (text.length * 60L).coerceIn(1500L, 4500L)
                mainHandler.postDelayed({
                    _isSpeaking.value = false
                }, estimatedMs)
            }
        }
    }

    fun setContinuousSession(active: Boolean) {
        continuousSessionActive = active
        if (!active) {
            stopListening()
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun startListening(languageTag: String = "ckb-IQ") {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "SpeechRecognizer not available on this device/emulator")
                _isListening.value = false
                return@post
            }
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                _partialTranscript.value = ""
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer", e)
            }
            _isListening.value = false
            _audioLevel.value = 0f
            _partialTranscript.value = ""
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _audioLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _audioLevel.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _audioLevel.value = 0f
            if (continuousSessionActive && !_isSpeaking.value) {
                // Gently retry listening if session is still active and error wasn't permission failure
                if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS &&
                    error != SpeechRecognizer.ERROR_CLIENT
                ) {
                    mainHandler.postDelayed({
                        if (continuousSessionActive && !_isSpeaking.value) {
                            startListening()
                        }
                    }, 1500L)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _audioLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestText = matches?.firstOrNull()?.trim().orEmpty()
            _partialTranscript.value = ""
            if (bestText.isNotEmpty()) {
                onSpeechRecognized(bestText)
            } else if (continuousSessionActive) {
                mainHandler.postDelayed({ startListening() }, 800L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotEmpty()) {
                _partialTranscript.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun release() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                tts?.stop()
                tts?.shutdown()
                tts = null
            } catch (e: Exception) {
                Log.w(TAG, "Release error", e)
            }
        }
    }
}
