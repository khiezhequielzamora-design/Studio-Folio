package com.example.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.AudioTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "AudioOverviewPlayer"

class AudioOverviewPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var turns: List<AudioTurn> = emptyList()
    private var speechRate: Float = 1.0f

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeTurnIndex = MutableStateFlow(-1)
    val activeTurnIndex: StateFlow<Int> = _activeTurnIndex.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "US English TTS language data not supported, using default.")
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val idx = utteranceId?.removePrefix("turn_")?.toIntOrNull() ?: return
                        _activeTurnIndex.value = idx
                    }

                    override fun onDone(utteranceId: String?) {
                        val idx = utteranceId?.removePrefix("turn_")?.toIntOrNull() ?: return
                        if (_isPlaying.value) {
                            val nextIdx = idx + 1
                            if (nextIdx < turns.size) {
                                speakTurn(nextIdx)
                            } else {
                                _isPlaying.value = false
                                _activeTurnIndex.value = -1
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        _statusMessage.value = "Audio playback encountered a speech synthesis error."
                    }
                })
                _isReady.value = true
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status $status")
                _isReady.value = false
                _statusMessage.value = "Device Text-to-Speech engine unavailable; you can still read the full script below."
            }
        }
    }

    fun loadTurns(newTurns: List<AudioTurn>) {
        if (turns != newTurns) {
            stop()
            turns = newTurns
        }
    }

    fun togglePlayPause() {
        if (!_isReady.value) {
            _statusMessage.value = "Speech synthesis engine is not ready on this device."
            return
        }
        if (turns.isEmpty()) return

        if (_isPlaying.value) {
            pause()
        } else {
            val startIndex = _activeTurnIndex.value.takeIf { it in turns.indices } ?: 0
            _isPlaying.value = true
            speakTurn(startIndex)
        }
    }

    fun playFromTurn(index: Int) {
        if (!_isReady.value || index !in turns.indices) return
        _isPlaying.value = true
        speakTurn(index)
    }

    fun setSpeed(speed: Float) {
        speechRate = speed
        _playbackSpeed.value = speed
        if (_isPlaying.value && _activeTurnIndex.value in turns.indices) {
            speakTurn(_activeTurnIndex.value)
        }
    }

    fun pause() {
        _isPlaying.value = false
        tts?.stop()
    }

    fun stop() {
        _isPlaying.value = false
        _activeTurnIndex.value = -1
        tts?.stop()
    }

    private fun speakTurn(index: Int) {
        val turn = turns.getOrNull(index) ?: return
        val engine = tts ?: return
        val isAlex = turn.speaker.equals("Alex", ignoreCase = true)
        engine.setPitch(if (isAlex) 0.94f else 1.10f)
        engine.setSpeechRate(speechRate)
        _activeTurnIndex.value = index
        engine.speak(turn.line, TextToSpeech.QUEUE_FLUSH, null, "turn_$index")
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
