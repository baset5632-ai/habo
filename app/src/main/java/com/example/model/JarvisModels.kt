package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class ApiProviderType(
    val id: String,
    val titleKu: String,
    val subtitleKu: String,
    val defaultModel: String,
    val defaultEndpoint: String,
    val requiresApiKey: Boolean
) {
    FIREBASE_GEMINI(
        id = "FIREBASE_GEMINI",
        titleKu = "Google Gemini (Built-in Live)",
        subtitleKu = "بزوێنەری سەرەکی بێ پێویستی بە کلیل (Firebase AI Logic + Multimodal)",
        defaultModel = "gemini-3.8-flash",
        defaultEndpoint = "firebase://ai-logic/gemini",
        requiresApiKey = false
    ),
    GOOGLE_GEMINI_CUSTOM(
        id = "GOOGLE_GEMINI_CUSTOM",
        titleKu = "Google Gemini API (Custom Key)",
        subtitleKu = "بەکارهێنانی کلیلی تایبەتی خۆت بۆ Gemini Multimodal & Live",
        defaultModel = "gemini-3.8-flash",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta",
        requiresApiKey = true
    ),
    OPENAI(
        id = "OPENAI",
        titleKu = "OpenAI API (GPT-4o / Realtime)",
        subtitleKu = "ChatGPT / GPT-4o / Realtime Voice & Automation",
        defaultModel = "gpt-4o",
        defaultEndpoint = "https://api.openai.com/v1",
        requiresApiKey = true
    ),
    ANTHROPIC_CLAUDE(
        id = "ANTHROPIC_CLAUDE",
        titleKu = "Anthropic Claude API",
        subtitleKu = "Claude 3.5 Sonnet / Claude 3 Opus بۆ کۆد و زیرەکی قووڵ",
        defaultModel = "claude-3-5-sonnet-20241022",
        defaultEndpoint = "https://api.anthropic.com/v1",
        requiresApiKey = true
    ),
    CUSTOM_ENDPOINT(
        id = "CUSTOM_ENDPOINT",
        titleKu = "Custom API (DeepSeek / OpenRouter / Groq)",
        subtitleKu = "هەر مۆدێلێکی دەرەکی بە شێوازی OpenAI-Compatible Endpoint",
        defaultModel = "deepseek-chat",
        defaultEndpoint = "https://api.deepseek.com/v1",
        requiresApiKey = true
    );

    companion object {
        fun fromId(id: String): ApiProviderType {
            return entries.firstOrNull { it.id == id } ?: FIREBASE_GEMINI
        }
    }
}

data class UiStep(
    val step: Int,
    val action: String, // OPEN | CLICK | TYPE | SCROLL | FIND_TEXT
    val target: String,
    val value: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("step", step)
        put("action", action)
        put("target", target)
        put("value", value)
    }
}

data class GeneratedFileSpec(
    val filename: String,
    val content: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("filename", filename)
        put("content", content)
    }
}

data class FileOperationsSpec(
    val folderPath: String,
    val files: List<GeneratedFileSpec>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("folder_path", folderPath)
        val arr = JSONArray()
        files.forEach { arr.put(it.toJson()) }
        put("files", arr)
    }
}

data class JarvisCommandSpec(
    val type: String, // APP_AUTOMATION | OPEN_SETTINGS | CREATE_CODE_FILES | SCREEN_ACTION | CHAT_ONLY | IGNORE
    val targetAppOrIntent: String,
    val uiSteps: List<UiStep>,
    val fileOperations: FileOperationsSpec
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("target_app_or_intent", targetAppOrIntent)
        val stepsArr = JSONArray()
        uiSteps.forEach { stepsArr.put(it.toJson()) }
        put("ui_steps", stepsArr)
        put("file_operations", fileOperations.toJson())
    }
}

data class JarvisAgentResponse(
    val voiceReply: String,
    val assignedName: String,
    val requireWakeWord: Boolean,
    val shouldExecute: Boolean,
    val command: JarvisCommandSpec
) {
    fun toStrictJsonString(indentSpaces: Int = 2): String {
        val root = JSONObject().apply {
            put("voice_reply", voiceReply)
            put("assigned_name", assignedName)
            put("require_wake_word", requireWakeWord)
            put("should_execute", shouldExecute)
            put("command", command.toJson())
        }
        return root.toString(indentSpaces)
    }

    companion object {
        fun createIgnoreResponse(currentName: String, requireWakeWord: Boolean): JarvisAgentResponse {
            return JarvisAgentResponse(
                voiceReply = "",
                assignedName = currentName,
                requireWakeWord = requireWakeWord,
                shouldExecute = false,
                command = JarvisCommandSpec(
                    type = "IGNORE",
                    targetAppOrIntent = "",
                    uiSteps = emptyList(),
                    fileOperations = FileOperationsSpec(
                        folderPath = "",
                        files = emptyList()
                    )
                )
            )
        }

        fun createStartGreeting(currentName: String, requireWakeWord: Boolean): JarvisAgentResponse {
            return JarvisAgentResponse(
                voiceReply = "سڵاو، چیت ئەوێت بۆت بکەم؟",
                assignedName = currentName,
                requireWakeWord = requireWakeWord,
                shouldExecute = true,
                command = JarvisCommandSpec(
                    type = "CHAT_ONLY",
                    targetAppOrIntent = "",
                    uiSteps = emptyList(),
                    fileOperations = FileOperationsSpec(
                        folderPath = "",
                        files = emptyList()
                    )
                )
            )
        }

        fun fromJsonString(rawText: String, fallbackName: String, fallbackWakeWord: Boolean): JarvisAgentResponse {
            val cleaned = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val jsonStart = cleaned.indexOf('{')
            val jsonEnd = cleaned.lastIndexOf('}')
            val jsonCandidate = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned.substring(jsonStart, jsonEnd + 1)
            } else {
                cleaned
            }

            val obj = JSONObject(jsonCandidate)
            val voiceReply = obj.optString("voice_reply", "")
            val assignedName = obj.optString("assigned_name", fallbackName)
            val requireWakeWord = if (obj.has("require_wake_word")) {
                obj.optBoolean("require_wake_word", fallbackWakeWord)
            } else {
                fallbackWakeWord
            }
            val shouldExecute = obj.optBoolean("should_execute", true)

            val cmdObj = obj.optJSONObject("command") ?: JSONObject()
            val cmdType = cmdObj.optString("type", "CHAT_ONLY").uppercase()
            val targetAppOrIntent = cmdObj.optString("target_app_or_intent", "")

            val stepsList = mutableListOf<UiStep>()
            val stepsArr = cmdObj.optJSONArray("ui_steps")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    val sObj = stepsArr.optJSONObject(i) ?: continue
                    stepsList.add(
                        UiStep(
                            step = sObj.optInt("step", i + 1),
                            action = sObj.optString("action", "CLICK").uppercase(),
                            target = sObj.optString("target", ""),
                            value = sObj.optString("value", "")
                        )
                    )
                }
            }

            val fileOpsObj = cmdObj.optJSONObject("file_operations") ?: JSONObject()
            val folderPath = fileOpsObj.optString("folder_path", "")
            val filesList = mutableListOf<GeneratedFileSpec>()
            val filesArr = fileOpsObj.optJSONArray("files")
            if (filesArr != null) {
                for (i in 0 until filesArr.length()) {
                    val fObj = filesArr.optJSONObject(i) ?: continue
                    val fname = fObj.optString("filename", "").trim()
                    val fcontent = fObj.optString("content", "")
                    if (fname.isNotEmpty()) {
                        filesList.add(GeneratedFileSpec(filename = fname, content = fcontent))
                    }
                }
            }

            return JarvisAgentResponse(
                voiceReply = voiceReply,
                assignedName = assignedName,
                requireWakeWord = requireWakeWord,
                shouldExecute = shouldExecute,
                command = JarvisCommandSpec(
                    type = cmdType,
                    targetAppOrIntent = targetAppOrIntent,
                    uiSteps = stepsList,
                    fileOperations = FileOperationsSpec(
                        folderPath = folderPath,
                        files = filesList
                    )
                )
            )
        }
    }
}
