package com.kai.assistant.data.repository

import android.content.Context
import com.kai.assistant.core.memory.*
import com.kai.assistant.data.local.KaiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MemoryManagerImpl(
    private val context: Context,
    private val store: MemoryStore,
    private val cache: MemoryCache
) : MemoryManager {

    override val memoryStore: MemoryStore = store
    override val memoryCache: MemoryCache = cache

    override suspend fun initialize() {
        // Load any cached data if needed
    }

    override suspend fun shutdown() {
        memoryCache.clearAll()
    }

    override suspend fun exportData(): ExportedData = withContext(Dispatchers.IO) {
        val conversations = memoryStore.listConversations(Int.MAX_VALUE).mapNotNull { summary ->
            memoryStore.getConversationContext(summary.id)
        }
        val preferences = memoryStore.getAllPreferences()
        val facts = memoryStore.getFacts()
        ExportedData(
            conversations = conversations,
            preferences = preferences,
            facts = facts
        )
    }

    override suspend fun importData(data: ExportedData) = withContext(Dispatchers.IO) {
        data.conversations.forEach { memoryStore.saveConversationContext(it) }
        data.preferences.forEach { (key, value) -> memoryStore.savePreference(key, value) }
        data.facts.forEach { memoryStore.saveFact(it.key, it.value, it.category) }
        memoryCache.clearAll()
    }

    override suspend fun clearAllData(confirm: Boolean) {
        if (!confirm) throw IllegalArgumentException("Confirmation required")
        memoryStore.deleteAllConversations()
        memoryStore.deleteAllPreferences()
        memoryStore.deleteAllFacts()
        memoryCache.clearAll()
    }
}