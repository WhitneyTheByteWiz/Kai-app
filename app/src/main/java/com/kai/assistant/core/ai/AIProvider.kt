package com.kai.assistant.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Abstract interface for AI providers. Allows Kai to be provider-agnostic.
 * Supports multiple AI backends (OpenAI, Anthropic, local models, etc.)
 */
interface AIProvider {

    /**
     * Unique identifier for this provider
     */
    val id: String

    /**
     * Human-readable name for display
     */
    val name: String

    /**
     * Whether this provider is currently available
     */
    val isAvailable: Boolean

    /**
     * Get available models for this provider
     */
    suspend fun getModels(): Result<List<AIModel>>

    /**
     * Send a chat completion request
     */
    suspend fun completeChat(request: ChatCompletionRequest): Result<ChatCompletionResponse>

    /**
     * Stream a chat completion response
     */
    fun streamChat(request: ChatCompletionRequest): Flow<Result<ChatCompletionChunk>>

    /**
     * Get embeddings for text
     */
    suspend fun getEmbeddings(texts: List<String>): Result<List<FloatArray>>

    /**
     * Check if provider is healthy
     */
    suspend fun healthCheck(): Boolean
}

/**
 * AI Model metadata
 */
@Serializable
data class AIModel(
    val id: String,
    val name: String,
    val description: String?,
    val contextWindow: Int,
    val supportsStreaming: Boolean,
    val supportsTools: Boolean,
    val supportsVision: Boolean,
    val pricing: ModelPricing?
)

@Serializable
data class ModelPricing(
    val inputTokensPerMillion: Double,
    val outputTokensPerMillion: Double
)

/**
 * Chat completion request
 */
@Serializable
data class ChatCompletionRequest(
    val messages: List<ChatMessage>,
    val model: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val tools: List<ToolDefinition> = emptyList(),
    val toolChoice: ToolChoice = ToolChoice.AUTO,
    val systemPrompt: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Chat message
 */
@Serializable
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val name: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null
)

enum class MessageRole {
    SYSTEM, USER, ASSISTANT, TOOL
}

/**
 * Tool definition for function calling
 */
@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String>, // JSON Schema as string map
    val required: List<String> = emptyList()
)

enum class ToolChoice {
    AUTO, NONE, REQUIRED
}

/**
 * Tool call from AI
 */
@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String // JSON string
)

/**
 * Chat completion response
 */
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: TokenUsage?,
    val created: Long = System.currentTimeMillis() / 1000
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: FinishReason
)

enum class FinishReason {
    STOP, LENGTH, TOOL_CALLS, CONTENT_FILTER, ERROR
}

@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Streaming chunk
 */
@Serializable
data class ChatCompletionChunk(
    val id: String,
    val model: String,
    val choices: List<ChatChunkChoice>,
    val created: Long = System.currentTimeMillis() / 1000
)

@Serializable
data class ChatChunkChoice(
    val index: Int,
    val delta: ChatMessageDelta,
    val finishReason: FinishReason?
)

@Serializable
data class ChatMessageDelta(
    val role: MessageRole? = null,
    val content: String? = null,
    val toolCalls: List<ToolCall>? = null
)

/**
 * Result wrapper for error handling
 */
sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val error: AIError) : Result<Nothing>()
}

/**
 * AI-specific errors
 */
sealed class AIError(open val errorMessage: String) : Exception() {
    data class NetworkError(override val errorMessage: String) : AIError(errorMessage)
    data class AuthenticationError(override val errorMessage: String) : AIError(errorMessage)
    data class RateLimitError(override val errorMessage: String, val retryAfterSeconds: Int? = null) : AIError(errorMessage)
    data class ModelNotFoundError(override val errorMessage: String) : AIError(errorMessage)
    data class ContextWindowExceeded(override val errorMessage: String) : AIError(errorMessage)
    data class ContentFilterError(override val errorMessage: String) : AIError(errorMessage)
    data class UnknownError(override val errorMessage: String) : AIError(errorMessage)
}