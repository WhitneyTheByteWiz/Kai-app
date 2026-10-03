package com.kai.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSHelper(private val context: Context) {
    companion object {
        private const val TAG = "KAI_TTS"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingUtterance: String? = null

    fun initialize(onReady: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                } else {
                    isInitialized = true
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)
                    onReady()
                    pendingUtterance?.let { speak(it) }
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        
        if (!isInitialized) {
            pendingUtterance = text
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kai_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.1f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.1f, 2.0f))
    }
}