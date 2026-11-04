package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "api_configs")
data class ApiConfigEntity(
    @PrimaryKey val providerId: String,
    val apiKey: String,
    val modelName: String,
    val baseUrl: String,
    val isActive: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_logs")
data class ConversationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userPrompt: String,
    val voiceReply: String,
    val strictJson: String,
    val commandType: String,
    val executionStatus: String,
    val providerId: String,
    val wasIgnored: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_projects")
data class GeneratedProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectName: String,
    val folderPath: String,
    val actualSavedPath: String,
    val fileCount: Int,
    val filesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface JarvisDao {
    @Query("SELECT * FROM api_configs ORDER BY providerId ASC")
    fun getAllApiConfigs(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveApiConfig(): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE providerId = :providerId LIMIT 1")
    suspend fun getApiConfigById(providerId: String): ApiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApiConfig(config: ApiConfigEntity)

    @Query("UPDATE api_configs SET isActive = CASE WHEN providerId = :activeId THEN 1 ELSE 0 END")
    suspend fun setActiveProvider(activeId: String)

    @Query("SELECT * FROM conversation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ConversationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConversationLogEntity)

    @Query("DELETE FROM conversation_logs")
    suspend fun clearLogs()

    @Query("SELECT * FROM generated_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<GeneratedProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: GeneratedProjectEntity): Long

    @Query("DELETE FROM generated_projects WHERE id = :id")
    suspend fun deleteProject(id: Long)
}

@Database(
    entities = [
        ApiConfigEntity::class,
        ConversationLogEntity::class,
        GeneratedProjectEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_autonomous_agent.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
