package com.kai.assistant.data.repository

import android.content.Context
import com.kai.assistant.core.memory.*
import com.kai.assistant.data.local.ConversationEntity
import com.kai.assistant.data.local.FactEntity
import com.kai.assistant.data.local.KaiDatabase
import com.kai.assistant.data.local.PreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomMemoryStore(private val context: Context) : MemoryStore {
    private val database = KaiDatabase.getInstance(context)

    override suspend fun saveConversationContext(context: ConversationContext) = withContext(Dispatchers.IO) {
        database.conversationDao().insert(ConversationEntity.fromContext(context))
    }

    override suspend fun getConversationContext(id: String): ConversationContext? = withContext(Dispatchers.IO) {
        database.conversationDao().getById(id)?.toContext()
    }

    override suspend fun deleteConversationContext(id: String) = withContext(Dispatchers.IO) {
        database.conversationDao().deleteById(id)
    }

    override suspend fun listConversations(limit: Int): List<ConversationSummary> = withContext(Dispatchers.IO) {
        database.conversationDao().getRecent(limit).map { entity ->
            val ctx = entity.toContext()
            ConversationSummary(
                id = ctx.id,
                title = entity.title,
                messageCount = ctx.messages.size,
                lastMessagePreview = ctx.messages.lastOrNull()?.content?.take(100) ?: "",
                updatedAt = entity.updatedAt
            )
        }
    }

    override suspend fun savePreference(key: String, value: String) = withContext(Dispatchers.IO) {
        database.preferenceDao().insert(PreferenceEntity(key, value))
    }

    override suspend fun getPreference(key: String, default: String): String = withContext(Dispatchers.IO) {
        database.preferenceDao().getByKey(key)?.value ?: default
    }

    override suspend fun deletePreference(key: String) = withContext(Dispatchers.IO) {
        database.preferenceDao().deleteByKey(key)
    }

    override suspend fun saveFact(key: String, value: String, category: FactCategory) = withContext(Dispatchers.IO) {
        val fact = Fact(
            key = key,
            value = value,
            category = category,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.factDao().insert(FactEntity.fromFact(fact))
    }

    override suspend fun getFacts(category: FactCategory?): List<Fact> = withContext(Dispatchers.IO) {
        if (category != null) {
            database.factDao().getByCategory(category.ordinal).map { it.toFact() }
        } else {
            database.factDao().getAll().map { it.toFact() }
        }
    }

    override suspend fun deleteFact(key: String) = withContext(Dispatchers.IO) {
        database.factDao().deleteByKey(key)
    }

    override suspend fun saveDeviceState(key: String, value: String) = withContext(Dispatchers.IO) {
        database.preferenceDao().insert(PreferenceEntity("device_$key", value))
    }

    override suspend fun getDeviceState(key: String): String? = withContext(Dispatchers.IO) {
        database.preferenceDao().getByKey("device_$key")?.value
    }

    override suspend fun deleteAllConversations() = withContext(Dispatchers.IO) {
        database.conversationDao().deleteAll()
    }

    override suspend fun deleteAllPreferences() = withContext(Dispatchers.IO) {
        database.preferenceDao().deleteAll()
    }

    override suspend fun deleteAllFacts() = withContext(Dispatchers.IO) {
        database.factDao().deleteAll()
    }

    override suspend fun getAllPreferences(): Map<String, String> = withContext(Dispatchers.IO) {
        database.preferenceDao().getAll().associate { it.key to it.value }
    }
}