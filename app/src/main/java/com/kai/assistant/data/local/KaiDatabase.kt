package com.kai.assistant.data.local

import androidx.room.*
import com.kai.assistant.core.memory.ChatMessage
import com.kai.assistant.core.memory.ConversationContext
import com.kai.assistant.core.memory.Fact
import com.kai.assistant.core.memory.FactCategory
import com.kai.assistant.core.memory.FactSource
import com.kai.assistant.core.memory.MessageRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val messagesJson: String,
    val metadataJson: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromContext(context: ConversationContext): ConversationEntity {
            val json = Json { prettyPrint = true }
            return ConversationEntity(
                id = context.id,
                title = generateTitle(context.messages),
                messagesJson = json.encodeToString(context.messages),
                metadataJson = json.encodeToString(context.metadata),
                createdAt = context.createdAt,
                updatedAt = context.updatedAt
            )
        }

        private fun generateTitle(messages: List<ChatMessage>): String {
            val firstUserMessage = messages.firstOrNull { it.role == MessageRole.USER }
            return firstUserMessage?.content?.take(50) ?: "New Conversation"
        }
    }

    fun toContext(): ConversationContext {
        val json = Json { ignoreUnknownKeys = true }
        return ConversationContext(
            id = id,
            messages = json.decodeFromString(messagesJson),
            metadata = json.decodeFromString(metadataJson),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: Int, // FactCategory ordinal
    val confidence: Float,
    val source: Int, // FactSource ordinal
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toFact(): Fact = Fact(
        key = key,
        value = value,
        category = FactCategory.values()[category],
        confidence = confidence,
        source = FactSource.values()[source],
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromFact(fact: Fact): FactEntity = FactEntity(
            key = fact.key,
            value = fact.value,
            category = fact.category.ordinal,
            confidence = fact.confidence,
            source = fact.source.ordinal,
            createdAt = fact.createdAt,
            updatedAt = fact.updatedAt
        )
    }
}

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ConversationEntity>

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int
}

@Dao
interface FactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: FactEntity)

    @Query("SELECT * FROM facts WHERE key = :key")
    suspend fun getByKey(key: String): FactEntity?

    @Query("SELECT * FROM facts WHERE category = :category")
    suspend fun getByCategory(category: Int): List<FactEntity>

    @Query("SELECT * FROM facts")
    suspend fun getAll(): List<FactEntity>

    @Query("DELETE FROM facts WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM facts")
    suspend fun deleteAll()
}

@Dao
interface PreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preference: PreferenceEntity)

    @Query("SELECT * FROM preferences WHERE key = :key")
    suspend fun getByKey(key: String): PreferenceEntity?

    @Query("SELECT * FROM preferences")
    suspend fun getAll(): List<PreferenceEntity>

    @Query("DELETE FROM preferences WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM preferences")
    suspend fun deleteAll()
}

@Database(
    entities = [ConversationEntity::class, FactEntity::class, PreferenceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KaiDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun factDao(): FactDao
    abstract fun preferenceDao(): PreferenceDao

    companion object {
        @Volatile private var INSTANCE: KaiDatabase? = null

        fun getInstance(context: android.content.Context): KaiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KaiDatabase::class.java,
                    "kai_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}