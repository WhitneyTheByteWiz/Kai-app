package com.kai.assistant.core.memory

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Memory layer interface for persistent storage
 */
interface MemoryStore {
    // Short-term context (current conversation)
    suspend fun saveConversationContext(context: ConversationContext)
    suspend fun getConversationContext(id: String): ConversationContext?
    suspend fun deleteConversationContext(id: String)
    suspend fun listConversations(limit: Int = 50): List<ConversationSummary>

    // Long-term preferences
    suspend fun savePreference(key: String, value: String)
    suspend fun getPreference(key: String, default: String): String
    suspend fun deletePreference(key: String)

    // User-controlled stored information
    suspend fun saveFact(key: String, value: String, category: FactCategory)
    suspend fun getFacts(category: FactCategory? = null): List<Fact>
    suspend fun deleteFact(key: String)

    // Device-specific state
    suspend fun saveDeviceState(key: String, value: String)
    suspend fun getDeviceState(key: String): String?
    
    suspend fun deleteAllConversations()
    suspend fun deleteAllPreferences()
    suspend fun deleteAllFacts()
    suspend fun getAllPreferences(): Map<String, String>
}

/**
 * Conversation context with full history
 */
@Serializable
data class ConversationContext(
    val id: String,
    val messages: List<ChatMessage>,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ConversationSummary(
    val id: String,
    val title: String,
    val messageCount: Int,
    val lastMessagePreview: String,
    val updatedAt: Long
)

/**
 * Chat message for storage
 */
@Serializable
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String? = null
)

/**
 * User facts/preferences
 */
@Serializable
data class Fact(
    val key: String,
    val value: String,
    val category: FactCategory,
    val confidence: Float = 1.0f,
    val source: FactSource = FactSource.USER_PROVIDED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class FactCategory {
    PERSONAL, PREFERENCES, CONTACTS, LOCATIONS, SCHEDULE, DEVICES, OTHER
}

enum class FactSource { USER_PROVIDED, INFERRED, IMPORTED }

/**
 * In-memory cache with persistence
 */
interface MemoryCache {
    val conversationContext: StateFlow<ConversationContext?>
    val preferences: StateFlow<Map<String, String>>

    fun updateConversationContext(context: ConversationContext)
    fun updatePreference(key: String, value: String)
    fun clearConversation()
    fun clearAll()
}

/**
 * Memory manager coordinating all layers
 */
interface MemoryManager {
    val memoryStore: MemoryStore
    val memoryCache: MemoryCache

    suspend fun initialize()
    suspend fun shutdown()
    suspend fun exportData(): ExportedData
    suspend fun importData(data: ExportedData)
    suspend fun clearAllData(confirm: Boolean)
}

@Serializable
data class ExportedData(
    val conversations: List<ConversationContext>,
    val preferences: Map<String, String>,
    val facts: List<Fact>,
    val exportedAt: Long = System.currentTimeMillis(),
    val version: Int = 1
)