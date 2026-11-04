package com.example.data

import android.content.Context
import com.example.model.ApiProviderType
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val dao: JarvisDao,
    context: Context
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "jarvis_identity_prefs",
        Context.MODE_PRIVATE
    )

    val allApiConfigs: Flow<List<ApiConfigEntity>> = dao.getAllApiConfigs()
    val allLogs: Flow<List<ConversationLogEntity>> = dao.getAllLogs()
    val allProjects: Flow<List<GeneratedProjectEntity>> = dao.getAllProjects()

    fun getAssignedName(): String {
        return prefs.getString(KEY_ASSIGNED_NAME, "") ?: ""
    }

    fun setAssignedName(name: String) {
        prefs.edit().putString(KEY_ASSIGNED_NAME, name.trim()).apply()
    }

    fun isWakeWordRequired(): Boolean {
        return prefs.getBoolean(KEY_REQUIRE_WAKE_WORD, false)
    }

    fun setWakeWordRequired(required: Boolean) {
        prefs.edit().putBoolean(KEY_REQUIRE_WAKE_WORD, required).apply()
    }

    fun isFloatingWidgetEnabled(): Boolean {
        return prefs.getBoolean(KEY_FLOATING_WIDGET, true)
    }

    fun setFloatingWidgetEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_WIDGET, enabled).apply()
    }

    suspend fun ensureDefaultProvidersSeeded() {
        for (provider in ApiProviderType.entries) {
            val existing = dao.getApiConfigById(provider.id)
            if (existing == null) {
                dao.upsertApiConfig(
                    ApiConfigEntity(
                        providerId = provider.id,
                        apiKey = "",
                        modelName = provider.defaultModel,
                        baseUrl = provider.defaultEndpoint,
                        isActive = provider == ApiProviderType.FIREBASE_GEMINI
                    )
                )
            }
        }
        val active = dao.getActiveApiConfig()
        if (active == null) {
            dao.setActiveProvider(ApiProviderType.FIREBASE_GEMINI.id)
        }
    }

    suspend fun getActiveConfig(): ApiConfigEntity {
        ensureDefaultProvidersSeeded()
        return dao.getActiveApiConfig() ?: ApiConfigEntity(
            providerId = ApiProviderType.FIREBASE_GEMINI.id,
            apiKey = "",
            modelName = ApiProviderType.FIREBASE_GEMINI.defaultModel,
            baseUrl = ApiProviderType.FIREBASE_GEMINI.defaultEndpoint,
            isActive = true
        )
    }

    suspend fun saveProviderConfig(
        provider: ApiProviderType,
        apiKey: String,
        modelName: String,
        baseUrl: String,
        makeActive: Boolean
    ) {
        val current = dao.getApiConfigById(provider.id)
        dao.upsertApiConfig(
            ApiConfigEntity(
                providerId = provider.id,
                apiKey = apiKey.trim(),
                modelName = modelName.trim().ifEmpty { provider.defaultModel },
                baseUrl = baseUrl.trim().ifEmpty { provider.defaultEndpoint },
                isActive = if (makeActive) true else (current?.isActive ?: false),
                updatedAt = System.currentTimeMillis()
            )
        )
        if (makeActive) {
            dao.setActiveProvider(provider.id)
        }
    }

    suspend fun selectActiveProvider(providerId: String) {
        ensureDefaultProvidersSeeded()
        dao.setActiveProvider(providerId)
    }

    suspend fun insertConversationLog(log: ConversationLogEntity) {
        dao.insertLog(log)
    }

    suspend fun clearConversationLogs() {
        dao.clearLogs()
    }

    suspend fun insertGeneratedProject(project: GeneratedProjectEntity): Long {
        return dao.insertProject(project)
    }

    suspend fun deleteGeneratedProject(id: Long) {
        dao.deleteProject(id)
    }

    companion object {
        private const val KEY_ASSIGNED_NAME = "assigned_name"
        private const val KEY_REQUIRE_WAKE_WORD = "require_wake_word"
        private const val KEY_FLOATING_WIDGET = "floating_widget_enabled"
    }
}
