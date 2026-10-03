package com.kai.assistant.data.repository

import com.kai.assistant.core.memory.ConversationContext
import com.kai.assistant.core.memory.MemoryCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryCache : MemoryCache {
    private val _conversationContext = MutableStateFlow<ConversationContext?>(null)
    override val conversationContext: kotlinx.coroutines.flow.StateFlow<ConversationContext?> = _conversationContext.asStateFlow()

    private val _preferences = MutableStateFlow<Map<String, String>>(emptyMap())
    override val preferences: kotlinx.coroutines.flow.StateFlow<Map<String, String>> = _preferences.asStateFlow()

    override fun updateConversationContext(context: ConversationContext) {
        _conversationContext.value = context
    }

    override fun updatePreference(key: String, value: String) {
        _preferences.value = _preferences.value + (key to value)
    }

    override fun clearConversation() {
        _conversationContext.value = null
    }

    override fun clearAll() {
        _conversationContext.value = null
        _preferences.value = emptyMap()
    }
}