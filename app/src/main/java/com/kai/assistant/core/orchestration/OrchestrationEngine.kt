package com.kai.assistant.core.orchestration

import com.kai.assistant.core.ai.AIProvider
import com.kai.assistant.core.ai.ChatCompletionRequest
import com.kai.assistant.core.ai.ChatMessage
import com.kai.assistant.core.ai.MessageRole
import com.kai.assistant.core.tools.Tool
import com.kai.assistant.core.tools.ToolConfirmation
import com.kai.assistant.core.tools.ToolExecutionContext
import com.kai.assistant.core.tools.ToolRegistry
import com.kai.assistant.core.tools.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Assistant state machine
 */
enum class AssistantState {
    IDLE,           // Waiting for input
    LISTENING,      // Speech recognition active
    THINKING,       // AI processing / reasoning
    EXECUTING,      // Tool execution in progress
    SPEAKING,       // TTS playback active
    ERROR,          // Error state
    CONFIRMING      // Waiting for user confirmation
}

/**
 * Orchestration events for UI updates
 */
sealed class OrchestrationEvent {
    data class StateChanged(val state: AssistantState) : OrchestrationEvent()
    data class UserInputReceived(val text: String, val isVoice: Boolean) : OrchestrationEvent()
    object AIResponseStarted : OrchestrationEvent()
    data class AIResponseChunk(val text: String) : OrchestrationEvent()
    data class AIResponseCompleted(val fullResponse: String) : OrchestrationEvent()
    data class ToolSelected(val tool: Tool, val args: Map<String, String>) : OrchestrationEvent()
    data class ToolExecutionStarted(val toolId: String) : OrchestrationEvent()
    data class ToolExecutionProgress(val toolId: String, val progress: Float) : OrchestrationEvent()
    data class ToolExecutionCompleted(val result: ToolResult) : OrchestrationEvent()
    data class ConfirmationRequired(val confirmation: ToolConfirmation, val context: ToolExecutionContext) : OrchestrationEvent()
    data class ConfirmationResolved(val confirmed: Boolean) : OrchestrationEvent()
    data class ErrorOccurred(val error: OrchestrationError) : OrchestrationEvent()
    object ConversationCleared : OrchestrationEvent()
}

sealed class OrchestrationError(open val errorMessage: String) : Exception() {
    data class AIProviderError(override val errorMessage: String) : OrchestrationError(errorMessage)
    data class ToolExecutionError(override val errorMessage: String) : OrchestrationError(errorMessage)
    data class VoiceInputError(override val errorMessage: String) : OrchestrationError(errorMessage)
    data class VoiceOutputError(override val errorMessage: String) : OrchestrationError(errorMessage)
    data class PermissionError(override val errorMessage: String, val permission: String) : OrchestrationError(errorMessage)
    data class NetworkError(override val errorMessage: String) : OrchestrationError(errorMessage)
    data class UnknownError(override val errorMessage: String) : OrchestrationError(errorMessage)
}

/**
 * Main orchestration engine
 */
interface OrchestrationEngine {
    val state: StateFlow<AssistantState>
    val events: Channel<OrchestrationEvent>

    suspend fun processUserInput(text: String, isVoice: Boolean)
    suspend fun processVoiceInput(audioData: ByteArray)
    fun cancelCurrentOperation()
    fun confirmAction(confirmed: Boolean)
    suspend fun clearConversation()
}

/**
 * Orchestration configuration
 */
@Serializable
data class OrchestrationConfig(
    val defaultAIProviderId: String,
    val maxConversationHistory: Int = 50,
    val maxToolExecutionTimeMs: Long = 30000,
    val enableStreaming: Boolean = true,
    val enableVoiceActivation: Boolean = false,
    val voiceActivationKeyword: String = "hey kai",
    val confirmationTimeoutMs: Long = 30000
)

/**
 * Conversation context for multi-turn interactions
 */
@Serializable
data class ConversationContext(
    val id: String,
    val messages: List<ChatMessage>,
    val activeToolId: String? = null,
    val pendingConfirmation: ToolConfirmation? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Intent classification for routing
 */
@Serializable
sealed class UserIntent {
    data class Informational(val query: String, val confidence: Float) : UserIntent()
    data class Action(val toolId: String, val args: Map<String, String>, val confidence: Float) : UserIntent()
    data class MultiStep(val steps: List<ActionStep>, val confidence: Float) : UserIntent()
    data class Clarification(val question: String, val options: List<String>) : UserIntent()
    data class Unknown(val rawInput: String) : UserIntent()
}

@Serializable
data class ActionStep(
    val toolId: String,
    val args: Map<String, String>,
    val dependsOn: List<String> = emptyList(), // Previous step IDs
    val description: String
)