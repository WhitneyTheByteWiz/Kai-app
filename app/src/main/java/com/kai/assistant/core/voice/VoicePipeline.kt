package com.kai.assistant.core.voice

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Voice input interface
 */
interface VoiceInput {
    val isListening: StateFlow<Boolean>
    val recognitionResults: Channel<VoiceRecognitionResult>

    suspend fun startListening()
    suspend fun stopListening()
    fun cancel()
    suspend fun checkPermissions(): PermissionStatus
    suspend fun requestPermissions(): PermissionStatus
}

sealed class VoiceRecognitionResult {
    data class Partial(val text: String) : VoiceRecognitionResult()
    data class Final(val text: String, val confidence: Float) : VoiceRecognitionResult()
    data class Error(val error: VoiceInputError) : VoiceRecognitionResult()
    object Timeout : VoiceRecognitionResult()
    object Cancelled : VoiceRecognitionResult()
}

sealed class VoiceInputError(open val errorMessage: String) : Exception() {
    data class PermissionDenied(override val errorMessage: String) : VoiceInputError(errorMessage)
    data class ServiceUnavailable(override val errorMessage: String) : VoiceInputError(errorMessage)
    data class AudioError(override val errorMessage: String) : VoiceInputError(errorMessage)
    data class NetworkError(override val errorMessage: String) : VoiceInputError(errorMessage)
    data class RecognitionError(override val errorMessage: String) : VoiceInputError(errorMessage)
    data class UnknownError(override val errorMessage: String) : VoiceInputError(errorMessage)
}

enum class PermissionStatus {
    GRANTED, DENIED, PERMANENTLY_DENIED, NOT_REQUESTED
}

/**
 * Voice output (TTS) interface
 */
interface VoiceOutput {
    val isSpeaking: StateFlow<Boolean>
    val playbackState: StateFlow<PlaybackState>

    suspend fun speak(text: String, config: TTSConfig = TTSConfig()): TTSResult
    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    fun getAvailableVoices(): List<Voice>
    fun setVoice(voice: Voice)
}

enum class PlaybackState {
    IDLE, PLAYING, PAUSED, COMPLETED, ERROR
}

data class TTSConfig(
    val language: String = "en-US",
    val voiceName: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val useNetworkVoice: Boolean = true
)

sealed class TTSResult {
    data class Success(val utteranceId: String) : TTSResult()
    data class Failure(val error: TTSError) : TTSResult()
}

sealed class TTSError(open val errorMessage: String) : Exception() {
    data class EngineNotAvailable(override val errorMessage: String) : TTSError(errorMessage)
    data class LanguageNotSupported(override val errorMessage: String) : TTSError(errorMessage)
    data class SynthesisFailed(override val errorMessage: String) : TTSError(errorMessage)
    data class AudioError(override val errorMessage: String) : TTSError(errorMessage)
    data class UnknownError(override val errorMessage: String) : TTSError(errorMessage)
}

data class Voice(
    val id: String,
    val name: String,
    val language: String,
    val gender: VoiceGender,
    val quality: VoiceQuality,
    val isNetworkRequired: Boolean
)

enum class VoiceGender { MALE, FEMALE, NEUTRAL }
enum class VoiceQuality { LOW, MEDIUM, HIGH, VERY_HIGH }

/**
 * Voice pipeline coordinator
 */
interface VoicePipeline {
    val voiceInput: VoiceInput
    val voiceOutput: VoiceOutput

    suspend fun processVoiceCommand(): VoiceCommandResult?
}

sealed class VoiceCommandResult {
    data class Recognized(val text: String, val confidence: Float) : VoiceCommandResult()
    data class Failed(val error: VoiceInputError) : VoiceCommandResult()
    object Cancelled : VoiceCommandResult()
}