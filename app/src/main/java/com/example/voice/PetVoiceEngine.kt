package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.sin

class PetVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var lipSyncJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _mouthOpenAmount = MutableStateFlow(0f)
    val mouthOpenAmount: StateFlow<Float> = _mouthOpenAmount.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    var isMuted: Boolean = false
    private var currentPitch: Float = 1.12f
    private var currentSpeed: Float = 1.0f
    private var currentLanguageTag: String = "en-US"

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    applyLanguageAndVoice(currentLanguageTag, currentPitch, currentSpeed)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                            startLipSyncAnimation()
                        }

                        override fun onDone(utteranceId: String?) {
                            stopLipSyncAnimation()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            stopLipSyncAnimation()
                        }
                    })
                } else {
                    Log.w("PetVoiceEngine", "TTS initialization returned status $status")
                }
            }
        } catch (e: Exception) {
            Log.e("PetVoiceEngine", "TTS unavailable", e)
        }
    }

    fun applyLanguageAndVoice(languageTag: String, pitch: Float, speed: Float) {
        currentLanguageTag = languageTag
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
        val engine = tts ?: return
        if (!isTtsReady) return
        try {
            val locale = Locale.forLanguageTag(languageTag)
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.US
            }
            engine.setPitch(currentPitch)
            engine.setSpeechRate(currentSpeed)
        } catch (e: Exception) {
            Log.w("PetVoiceEngine", "Could not apply locale $languageTag", e)
        }
    }

    fun speak(text: String) {
        if (isMuted || text.isBlank()) return
        val cleanText = text.replace(Regex("[*#~_]"), "").trim()
        if (cleanText.isBlank()) return

        val engine = tts
        if (engine != null && isTtsReady) {
            applyLanguageAndVoice(currentLanguageTag, currentPitch, currentSpeed)
            val utteranceId = UUID.randomUUID().toString()
            engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            // Visual lip-sync fallback when hardware TTS is warming up
            scope.launch {
                _isSpeaking.value = true
                startLipSyncAnimation()
                val durationMs = (cleanText.length * 48L).coerceIn(1200L, 6000L)
                delay(durationMs)
                stopLipSyncAnimation()
            }
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        stopLipSyncAnimation()
    }

    private fun startLipSyncAnimation() {
        lipSyncJob?.cancel()
        lipSyncJob = scope.launch(Dispatchers.Main) {
            var tick = 0f
            while (isActive && _isSpeaking.value) {
                tick += 0.55f
                val syllablePulse = ((sin(tick.toDouble()) + 1.0) * 0.42 + (sin(tick * 2.3) + 1.0) * 0.14).toFloat()
                _mouthOpenAmount.value = syllablePulse.coerceIn(0.08f, 1.0f)
                delay(55L)
            }
            _mouthOpenAmount.value = 0f
        }
    }

    private fun stopLipSyncAnimation() {
        _isSpeaking.value = false
        lipSyncJob?.cancel()
        _mouthOpenAmount.value = 0f
    }

    fun startListening(languageTag: String, onFinalResult: (String) -> Unit) {
        _voiceError.value = null
        stopSpeaking()
        scope.launch(Dispatchers.Main) {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    _voiceError.value = "Speech recognition service is not installed on this device. You can type below anytime!"
                    return@launch
                }
                speechRecognizer?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialTranscript.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that! Tap the mic to speak again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap the mic when ready!"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice chat."
                            else -> "Voice input paused (code $error). Try again or use text chat."
                        }
                        _voiceError.value = msg
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim().orEmpty()
                        _partialTranscript.value = best
                        if (best.isNotBlank()) {
                            onFinalResult(best)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull().orEmpty()
                        if (partial.isNotBlank()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e("PetVoiceEngine", "SpeechRecognizer error", e)
                _isListening.value = false
                _voiceError.value = e.localizedMessage ?: "Unable to start microphone."
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _isListening.value = false
        }
    }

    fun playChimeEffect(happy: Boolean = true) {
        if (isMuted) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 55)
            val tone = if (happy) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP
            toneGen.startTone(tone, 130)
            scope.launch {
                delay(200L)
                toneGen.release()
            }
        } catch (_: Exception) {
        }
    }

    fun clearVoiceError() {
        _voiceError.value = null
    }

    fun shutdown() {
        lipSyncJob?.cancel()
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
