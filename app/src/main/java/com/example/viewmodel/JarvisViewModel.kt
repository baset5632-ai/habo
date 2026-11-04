package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.UniversalAiService
import com.example.automation.DeviceController
import com.example.automation.JarvisAccessibilityService
import com.example.data.ApiConfigEntity
import com.example.data.ConversationLogEntity
import com.example.data.GeneratedProjectEntity
import com.example.data.JarvisDatabase
import com.example.data.JarvisRepository
import com.example.model.ApiProviderType
import com.example.model.FileOperationsSpec
import com.example.model.GeneratedFileSpec
import com.example.model.JarvisAgentResponse
import com.example.voice.VoiceEngineManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

sealed interface AgentUiState {
    data object Idle : AgentUiState
    data object Processing : AgentUiState
    data class Success(val response: JarvisAgentResponse, val executionSummary: String) : AgentUiState
    data class IgnoredNoise(val input: String, val response: JarvisAgentResponse) : AgentUiState
    data class Error(val message: String) : AgentUiState
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getInstance(application)
    private val repository = JarvisRepository(database.jarvisDao(), application)
    private val aiService = UniversalAiService()
    private val deviceController = DeviceController(application)

    val voiceEngine = VoiceEngineManager(application) { recognizedSpeech ->
        submitCommand(recognizedSpeech, fromVoice = true)
    }

    val apiConfigs: StateFlow<List<ApiConfigEntity>> = repository.allApiConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversationLogs: StateFlow<List<ConversationLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedProjects: StateFlow<List<GeneratedProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSessionStarted = MutableStateFlow(false)
    val isSessionStarted: StateFlow<Boolean> = _isSessionStarted.asStateFlow()

    private val _assignedName = MutableStateFlow(repository.getAssignedName())
    val assignedName: StateFlow<String> = _assignedName.asStateFlow()

    private val _requireWakeWord = MutableStateFlow(repository.isWakeWordRequired())
    val requireWakeWord: StateFlow<Boolean> = _requireWakeWord.asStateFlow()

    private val _floatingWidgetEnabled = MutableStateFlow(repository.isFloatingWidgetEnabled())
    val floatingWidgetEnabled: StateFlow<Boolean> = _floatingWidgetEnabled.asStateFlow()

    private val _agentState = MutableStateFlow<AgentUiState>(AgentUiState.Idle)
    val agentState: StateFlow<AgentUiState> = _agentState.asStateFlow()

    private val _latestStrictJson = MutableStateFlow("")
    val latestStrictJson: StateFlow<String> = _latestStrictJson.asStateFlow()

    private val _selectedProject = MutableStateFlow<GeneratedProjectEntity?>(null)
    val selectedProject: StateFlow<GeneratedProjectEntity?> = _selectedProject.asStateFlow()

    val isListening: StateFlow<Boolean> = voiceEngine.isListening
    val isSpeaking: StateFlow<Boolean> = voiceEngine.isSpeaking
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript
    val audioLevel: StateFlow<Float> = voiceEngine.audioLevel

    val isAccessibilityConnected: StateFlow<Boolean> = JarvisAccessibilityService.isConnected
    val activeWindowPackage: StateFlow<String> = JarvisAccessibilityService.activePackage
    val visibleScreenElements: StateFlow<List<String>> = JarvisAccessibilityService.visibleElements
    val accessibilityStepLogs: StateFlow<List<String>> = JarvisAccessibilityService.stepLogs

    init {
        viewModelScope.launch {
            repository.ensureDefaultProvidersSeeded()
            // Initialize default strict JSON preview
            val initialPreview = JarvisAgentResponse.createStartGreeting(
                currentName = _assignedName.value,
                requireWakeWord = _requireWakeWord.value
            )
            _latestStrictJson.value = initialPreview.toStrictJsonString()
        }
    }

    /**
     * Rule 1: When the user presses START, immediately greet in friendly Kurdish:
     * "سڵاو، چیت ئەوێت بۆت بکەم؟"
     */
    fun toggleStartStop(hasAudioPermission: Boolean) {
        val starting = !_isSessionStarted.value
        _isSessionStarted.value = starting

        if (starting) {
            voiceEngine.setContinuousSession(hasAudioPermission)
            val greetingResponse = JarvisAgentResponse.createStartGreeting(
                currentName = _assignedName.value,
                requireWakeWord = _requireWakeWord.value
            )
            val jsonStr = greetingResponse.toStrictJsonString()
            _latestStrictJson.value = jsonStr
            _agentState.value = AgentUiState.Success(
                response = greetingResponse,
                executionSummary = "دەستپێکردنی سیستەمی دەنگی و گوێگرتنی زیندوو"
            )
            voiceEngine.speakKurdish(greetingResponse.voiceReply)

            viewModelScope.launch {
                val active = repository.getActiveConfig()
                repository.insertConversationLog(
                    ConversationLogEntity(
                        userPrompt = "[داگرتنی دوگمەی START]",
                        voiceReply = greetingResponse.voiceReply,
                        strictJson = jsonStr,
                        commandType = "CHAT_ONLY",
                        executionStatus = "دەستبەجێ سڵاوی کرد و ئامادەیە بۆ فەرمان",
                        providerId = active.providerId,
                        wasIgnored = false
                    )
                )
            }
        } else {
            voiceEngine.setContinuousSession(false)
            _agentState.value = AgentUiState.Idle
        }
    }

    fun startSingleVoiceCapture() {
        voiceEngine.startListening("ckb-IQ")
    }

    fun stopVoiceCapture() {
        voiceEngine.stopListening()
    }

    fun updateCustomName(newName: String) {
        val cleaned = newName.trim()
        _assignedName.value = cleaned
        repository.setAssignedName(cleaned)
    }

    fun setWakeWordLock(locked: Boolean) {
        _requireWakeWord.value = locked
        repository.setWakeWordRequired(locked)
    }

    fun setFloatingWidgetActive(enabled: Boolean) {
        _floatingWidgetEnabled.value = enabled
        repository.setFloatingWidgetEnabled(enabled)
    }

    fun submitCommand(userInput: String, fromVoice: Boolean = false) {
        val prompt = userInput.trim()
        if (prompt.isEmpty()) return

        viewModelScope.launch {
            _agentState.value = AgentUiState.Processing
            val activeConfig = repository.getActiveConfig()

            val result = aiService.processUserCommand(
                userText = prompt,
                currentName = _assignedName.value,
                requireWakeWord = _requireWakeWord.value,
                activeConfig = activeConfig
            )

            result.fold(
                onSuccess = { agentResponse ->
                    // Persist updated name if changed
                    if (agentResponse.assignedName != _assignedName.value) {
                        _assignedName.value = agentResponse.assignedName
                        repository.setAssignedName(agentResponse.assignedName)
                    }
                    // Persist updated wake-word lock state
                    if (agentResponse.requireWakeWord != _requireWakeWord.value) {
                        _requireWakeWord.value = agentResponse.requireWakeWord
                        repository.setWakeWordRequired(agentResponse.requireWakeWord)
                    }

                    val strictJson = agentResponse.toStrictJsonString()
                    _latestStrictJson.value = strictJson

                    if (!agentResponse.shouldExecute ||
                        agentResponse.command.type.equals("IGNORE", ignoreCase = true)
                    ) {
                        // Rule 3: Noise / Missing Wake-Word -> Complete silence & IGNORE state
                        _agentState.value = AgentUiState.IgnoredNoise(prompt, agentResponse)
                        repository.insertConversationLog(
                            ConversationLogEntity(
                                userPrompt = prompt,
                                voiceReply = "(بێدەنگ - ناوی یاریدەدەر نەهێنراوە / IGNORE)",
                                strictJson = strictJson,
                                commandType = "IGNORE",
                                executionStatus = "فەرامۆش کرا بەپێی یاسای قوفڵی ناو و ژاوەژاو",
                                providerId = activeConfig.providerId,
                                wasIgnored = true
                            )
                        )
                    } else {
                        // Speak Kurdish response & execute device command
                        if (agentResponse.voiceReply.isNotBlank()) {
                            voiceEngine.speakKurdish(agentResponse.voiceReply)
                        }
                        val outcome = deviceController.executeCommand(agentResponse)
                        outcome.createdProject?.let { proj ->
                            val id = repository.insertGeneratedProject(proj)
                            _selectedProject.value = proj.copy(id = id)
                        }

                        _agentState.value = AgentUiState.Success(
                            response = agentResponse,
                            executionSummary = outcome.summaryKu
                        )

                        repository.insertConversationLog(
                            ConversationLogEntity(
                                userPrompt = prompt,
                                voiceReply = agentResponse.voiceReply,
                                strictJson = strictJson,
                                commandType = agentResponse.command.type,
                                executionStatus = outcome.summaryKu,
                                providerId = activeConfig.providerId,
                                wasIgnored = false
                            )
                        )
                    }
                },
                onFailure = { error ->
                    val errMsg = error.localizedMessage ?: "هەڵەیەک ڕوویدا لە کاتی پەیوەندیکردن بە مۆدێلی زیرەک."
                    _agentState.value = AgentUiState.Error(errMsg)
                }
            )
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            repository.selectActiveProvider(providerId)
        }
    }

    fun saveProviderSettings(
        provider: ApiProviderType,
        apiKey: String,
        modelName: String,
        baseUrl: String,
        makeActive: Boolean
    ) {
        viewModelScope.launch {
            repository.saveProviderConfig(
                provider = provider,
                apiKey = apiKey,
                modelName = modelName,
                baseUrl = baseUrl,
                makeActive = makeActive
            )
        }
    }

    fun triggerDirectSettingsAction(keyword: String) {
        deviceController.openSystemSettings(keyword)
    }

    fun openAccessibilitySettings() {
        deviceController.openAccessibilitySettings()
    }

    fun createInstantProjectTemplate(
        projectName: String,
        files: List<GeneratedFileSpec>
    ) {
        viewModelScope.launch {
            val folderPath = "/storage/emulated/0/AI_Projects/$projectName"
            val outcome = deviceController.createProjectFiles(
                FileOperationsSpec(folderPath = folderPath, files = files)
            )
            outcome.createdProject?.let { proj ->
                val id = repository.insertGeneratedProject(proj)
                _selectedProject.value = proj.copy(id = id)
            }
            val voiceText = "فۆڵدەری $projectName بە سەرکەوتوویی دروست کرا و ${files.size} فایلی کۆدی تێدا خەزن کرا."
            voiceEngine.speakKurdish(voiceText)
        }
    }

    fun selectProjectForInspection(project: GeneratedProjectEntity?) {
        _selectedProject.value = project
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            if (_selectedProject.value?.id == id) {
                _selectedProject.value = null
            }
            repository.deleteGeneratedProject(id)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearConversationLogs()
            JarvisAccessibilityService.clearStepLogs()
        }
    }

    fun parseProjectFiles(project: GeneratedProjectEntity): List<GeneratedFileSpec> {
        val list = mutableListOf<GeneratedFileSpec>()
        try {
            val arr = JSONArray(project.filesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    GeneratedFileSpec(
                        filename = obj.optString("filename", "file_$i.txt"),
                        content = obj.optString("content", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }
}
