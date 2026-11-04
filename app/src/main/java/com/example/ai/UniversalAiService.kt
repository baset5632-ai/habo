package com.example.ai

import android.util.Log
import com.example.data.ApiConfigEntity
import com.example.model.ApiProviderType
import com.example.model.JarvisAgentResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "UniversalAiService"

class UniversalAiService {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Strict JSON Schema for Firebase AI Logic per skill rules (using Schema.obj)
    private val jarvisResponseSchema by lazy {
        val uiStepSchema = Schema.obj(
            properties = mapOf(
                "step" to Schema.integer(description = "Sequential step number starting at 1"),
                "action" to Schema.enumeration(
                    values = listOf("OPEN", "CLICK", "TYPE", "SCROLL", "FIND_TEXT"),
                    description = "UI automation action type"
                ),
                "target" to Schema.string(description = "Button text, view ID, or UI element label"),
                "value" to Schema.string(description = "Text to type or scroll direction if applicable")
            )
        )

        val fileSpecSchema = Schema.obj(
            properties = mapOf(
                "filename" to Schema.string(description = "File name with extension, e.g., index.html, style.css, main.py"),
                "content" to Schema.string(description = "Complete, production-ready source code content")
            )
        )

        val fileOperationsSchema = Schema.obj(
            properties = mapOf(
                "folder_path" to Schema.string(description = "Target folder path, e.g., /storage/emulated/0/AI_Projects/Project_Name"),
                "files" to Schema.array(
                    items = fileSpecSchema,
                    description = "List of complete code files to create in the folder"
                )
            )
        )

        val commandSchema = Schema.obj(
            properties = mapOf(
                "type" to Schema.enumeration(
                    values = listOf(
                        "APP_AUTOMATION",
                        "OPEN_SETTINGS",
                        "CREATE_CODE_FILES",
                        "SCREEN_ACTION",
                        "CHAT_ONLY",
                        "IGNORE"
                    ),
                    description = "Primary command execution type"
                ),
                "target_app_or_intent" to Schema.string(
                    description = "Android package name (e.g., org.telegram.messenger, com.zhiliaoapp.musically) or Settings intent action (e.g., android.settings.WIFI_SETTINGS)"
                ),
                "ui_steps" to Schema.array(
                    items = uiStepSchema,
                    description = "Ordered UI steps for Accessibility automation"
                ),
                "file_operations" to fileOperationsSchema
            )
        )

        Schema.obj(
            properties = mapOf(
                "voice_reply" to Schema.string(
                    description = "Natural, warm, friendly Kurdish (Sorani) spoken reply to the user"
                ),
                "assigned_name" to Schema.string(
                    description = "Current assigned name of the assistant (empty if none set, or updated if user named the assistant)"
                ),
                "require_wake_word" to Schema.boolean(
                    description = "True if user requested wake-word lock ('تا ناوت نەهێنم هیچ کارێک مەکە'), false otherwise"
                ),
                "should_execute" to Schema.boolean(
                    description = "True if the command should be executed, false if ignored"
                ),
                "command" to commandSchema
            )
        )
    }

    private val firebaseGeminiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = jarvisResponseSchema
            }
        )
    }

    fun buildSystemPrompt(currentName: String, requireWakeWord: Boolean): String {
        val nameContext = if (currentName.isBlank()) {
            "ئێستا هیچ ناوێکی تایبەتت بۆ دانەنراوە (assigned_name = \"\"). لە خۆتەوە هیچ ناوێک وەک جاک یان هەر ناوێکی تر لە خۆت مەنێ تا بەکارهێنەر خۆی ناوت لێ دەنێت."
        } else {
            "ناوی فەرمیی ئێستای تۆ بریتییە لە: \"$currentName\"."
        }

        val wakeWordContext = if (requireWakeWord) {
            "دۆخی قوفڵی ناو چالاکە (require_wake_word = true). ئەگەر ڕستەی بەکارهێنەر ناوی تۆی تێدا نەبوو (جگە لە کاتی گۆڕینی ناو یان لادانی قوفڵ)، دەبێت should_execute = false و command.type = \"IGNORE\" و voice_reply = \"\" بگەڕێنیتەوە."
        } else {
            "دۆخی قوفڵی ناو ناچالاکە (require_wake_word = false). وەڵامی هەموو فەرمانێکی بەکارهێنەر بدەرەوە."
        }

        return """
تۆ مێشکی سەرەکی و بزوێنەری ئەپێکی ئەندرۆیدی زۆر پێشکەوتوویت کە هاوشێوەی (Jarvis) کار دەکات. تۆ یاریدەدەرێکی دەنگی و نووسینی تەواو سەربەخۆیت (Autonomous Android AI Agent) کە دەسەڵاتی تەواوت هەیە بەسەر سیستەمی مۆبایلەکەدا لە ڕێگەی (Accessibility Service, System Intents, File System).
وەک هاوڕێیەکی نزیک، زیرەک و دڵسۆز بە زمانی کوردیی شیرین قسە لەگەڵ بەکارهێنەر دەکەیت.

بارودۆخی ئێستا:
- $nameContext
- $wakeWordContext

یاسا بنەڕەتییەکان:
1. ئەگەر بەکارهێنەر وتی "تۆ ناوت [ناوێک]ـە" یان "ناوی تۆ [ناوێک]ە"، دەستبەجێ assigned_name بکە بەو ناوە و بە گەرمی پێشوازی لێ بکە.
2. ئەگەر بەکارهێنەر وتی "تا ناوت نەهێنم هیچ کارێک مەکە" یان هاوشێوەی ئەوە، require_wake_word بکە بە true. ئەگەر وتی "بەبێ ناو وەڵام بدەرەوە" یان "قوفڵی ناو لاببە"، require_wake_word بکە بە false.
3. ئەگەر داوای کردنەوە یان کارکردن لەناو ئەپێک کرا (وەک تێلیگرام org.telegram.messenger، تیکتۆک com.zhiliaoapp.musically، یوتیوب com.google.android.youtube، واتسئەپ com.whatsapp، ئینستاگرام com.instagram.android، فەیسبووک com.facebook.katana، کرۆم com.android.chrome)، command.type بکە بە "APP_AUTOMATION"، ناوی پاکێجەکە بنووسە لە target_app_or_intent، و هەنگاوەکانی سەر شاشەکە لە ui_steps بنووسە (OPEN, FIND_TEXT, CLICK, TYPE, SCROLL).
4. ئەگەر داوای گۆڕانکاری یان کردنەوەی بەشێکی سێتینگ کرا (وەک وایفای android.settings.WIFI_SETTINGS، بلوتوز android.settings.BLUETOOTH_SETTINGS، ڕووناکی شاشە android.settings.DISPLAY_SETTINGS، دەنگ android.settings.SOUND_SETTINGS، پاتری android.settings.BATTERY_SAVER_SETTINGS، شوێن/GPS android.settings.LOCATION_SOURCE_SETTINGS، دەستگەیشتن android.settings.ACCESSIBILITY_SETTINGS، یان سێتینگی گشتی android.settings.SETTINGS)، command.type بکە بە "OPEN_SETTINGS" و ئینتێنتەکە لە target_app_or_intent دابنێ.
5. ئەگەر داوای دروستکردنی فۆڵدەر یان نووسینی کۆد کرا (وەک وێبسایت، HTML/CSS/JS، Python، یان هەر پڕۆژەیەک)، command.type بکە بە "CREATE_CODE_FILES"، لە file_operations.folder_path ڕێڕەوی "/storage/emulated/0/AI_Projects/<Project_Name>" دابنێ و هەموو فایلەکان بە کۆدی تەواو و بێ کەموکوڕی لە ناو files بنووسە.
6. هەمیشە تەنیا و تەنیا JSONـی پاک بگەڕێنەوە بەبێ هیچ شیکردنەوەیەکی دەرەوەی JSON.
""".trimIndent()
    }

    /**
     * Pre-processes deterministic wake-word & naming rules and then queries the active AI provider.
     */
    suspend fun processUserCommand(
        userText: String,
        currentName: String,
        requireWakeWord: Boolean,
        activeConfig: ApiConfigEntity
    ): Result<JarvisAgentResponse> = withContext(Dispatchers.IO) {
        val trimmedInput = userText.trim()

        // 1. Check if user is explicitly setting a new name in Kurdish/English
        val extractedNewName = extractAssignedNameFromText(trimmedInput)
        val effectiveName = extractedNewName ?: currentName

        // 2. Check if user is explicitly toggling wake-word lock in this sentence
        val wakeWordCommand = detectWakeWordToggle(trimmedInput)
        val effectiveWakeWord = wakeWordCommand ?: requireWakeWord

        // 3. Strict Wake-Word & Noise Immunity check:
        // If wake-word lock is active, and the user is NOT setting a name or toggling the lock right now,
        // verify that the input contains the assigned name.
        if (effectiveWakeWord && effectiveName.isNotBlank() && extractedNewName == null && wakeWordCommand == null) {
            val normalizedInput = normalizeKurdish(trimmedInput)
            val normalizedName = normalizeKurdish(effectiveName)
            if (!normalizedInput.contains(normalizedName)) {
                Log.d(TAG, "Strict Wake-Word Lock active: '$effectiveName' not found in '$trimmedInput'. Returning IGNORE.")
                return@withContext Result.success(
                    JarvisAgentResponse.createIgnoreResponse(
                        currentName = effectiveName,
                        requireWakeWord = true
                    )
                )
            }
        }

        val systemPrompt = buildSystemPrompt(effectiveName, effectiveWakeWord)
        val fullUserPrompt = """
فەرمانی بەکارهێنەر: "$trimmedInput"
ناوی ئێستای یاریدەدەر: "$effectiveName"
قوفڵی ناو (require_wake_word): $effectiveWakeWord

تکایە وەڵامەکە بە قاڵبی فەرمیی JSON بگەڕێنەوە.
""".trimIndent()

        try {
            val providerType = ApiProviderType.fromId(activeConfig.providerId)
            val rawResponse = withTimeout(60_000L) {
                when (providerType) {
                    ApiProviderType.FIREBASE_GEMINI -> {
                        val response = firebaseGeminiModel.generateContent(
                            content {
                                text(systemPrompt)
                                text(fullUserPrompt)
                            }
                        )
                        response.text ?: throw IllegalStateException("مۆدێلی Gemini وەڵامێکی بەتاڵی گەڕاندەوە.")
                    }

                    ApiProviderType.GOOGLE_GEMINI_CUSTOM -> {
                        callCustomGeminiRestApi(
                            apiKey = activeConfig.apiKey,
                            modelName = activeConfig.modelName,
                            baseUrl = activeConfig.baseUrl,
                            systemPrompt = systemPrompt,
                            userPrompt = fullUserPrompt
                        )
                    }

                    ApiProviderType.OPENAI -> {
                        callOpenAiCompatibleApi(
                            apiKey = activeConfig.apiKey,
                            modelName = activeConfig.modelName,
                            baseUrl = activeConfig.baseUrl,
                            systemPrompt = systemPrompt,
                            userPrompt = fullUserPrompt,
                            providerLabel = "OpenAI"
                        )
                    }

                    ApiProviderType.ANTHROPIC_CLAUDE -> {
                        callAnthropicClaudeApi(
                            apiKey = activeConfig.apiKey,
                            modelName = activeConfig.modelName,
                            baseUrl = activeConfig.baseUrl,
                            systemPrompt = systemPrompt,
                            userPrompt = fullUserPrompt
                        )
                    }

                    ApiProviderType.CUSTOM_ENDPOINT -> {
                        callOpenAiCompatibleApi(
                            apiKey = activeConfig.apiKey,
                            modelName = activeConfig.modelName,
                            baseUrl = activeConfig.baseUrl,
                            systemPrompt = systemPrompt,
                            userPrompt = fullUserPrompt,
                            providerLabel = "Custom API"
                        )
                    }
                }
            }

            val parsed = JarvisAgentResponse.fromJsonString(
                rawText = rawResponse,
                fallbackName = effectiveName,
                fallbackWakeWord = effectiveWakeWord
            )

            // Guarantee deterministic name & wake-word updates if detected in user text
            val finalName = if (!extractedNewName.isNullOrBlank()) {
                extractedNewName
            } else {
                parsed.assignedName
            }
            val finalWakeWord = wakeWordCommand ?: parsed.requireWakeWord

            Result.success(
                parsed.copy(
                    assignedName = finalName,
                    requireWakeWord = finalWakeWord
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI inference timed out after 60s", e)
            Result.failure(Exception("پەیوەندییەکە کاتی زۆری خایاند (Timeout 60s). تکایە دووبارە هەوڵ بدەرەوە."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI inference failed", e)
            Result.failure(e)
        }
    }

    private fun callCustomGeminiRestApi(
        apiKey: String,
        modelName: String,
        baseUrl: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("تکایە سەرەتا کلیلی (Google Gemini API Key) لە بەشی بەڕێوەبەری API داخڵ بکە، یان بگۆڕە بۆ Google Gemini (Built-in Live).")
        }
        val cleanBase = baseUrl.trimEnd('/')
        val url = "$cleanBase/models/$modelName:generateContent?key=$apiKey"

        val bodyJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("هەڵەی Gemini API (${response.code}): $respBody")
            }
            val json = JSONObject(respBody)
            return json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: throw IllegalStateException("وەڵامی Gemini خاڵی بوو.")
        }
    }

    private fun callOpenAiCompatibleApi(
        apiKey: String,
        modelName: String,
        baseUrl: String,
        systemPrompt: String,
        userPrompt: String,
        providerLabel: String
    ): String {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("تکایە سەرەتا کلیلی ($providerLabel API Key) لە بەشی بەڕێوەبەری API دابنێ.")
        }
        val cleanBase = baseUrl.trimEnd('/')
        val endpointUrl = if (cleanBase.endsWith("/chat/completions")) {
            cleanBase
        } else {
            "$cleanBase/chat/completions"
        }

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", modelName)
            put("messages", messages)
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("هەڵەی $providerLabel (${response.code}): $respBody")
            }
            val json = JSONObject(respBody)
            return json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?: throw IllegalStateException("وەڵامی $providerLabel خاڵی بوو.")
        }
    }

    private fun callAnthropicClaudeApi(
        apiKey: String,
        modelName: String,
        baseUrl: String,
        systemPrompt: String,
        userPrompt: String
    ): String {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("تکایە سەرەتا کلیلی (Anthropic Claude API Key) لە بەشی بەڕێوەبەری API دابنێ.")
        }
        val cleanBase = baseUrl.trimEnd('/')
        val endpointUrl = if (cleanBase.endsWith("/messages")) {
            cleanBase
        } else {
            "$cleanBase/messages"
        }

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", modelName)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("هەڵەی Claude API (${response.code}): $respBody")
            }
            val json = JSONObject(respBody)
            return json.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: throw IllegalStateException("وەڵامی Claude خاڵی بوو.")
        }
    }

    /**
     * Extracts custom assistant name when user says:
     * "تۆ ناوت [ناو]ـە" or "تۆ ناوت [ناو]ە" or "ناوی تۆ [ناو]ـە" or "ناوت بنێ [ناو]"
     */
    fun extractAssignedNameFromText(text: String): String? {
        val patterns = listOf(
            Regex("""(?:تۆ\s+ناوت|ناوی\s+تۆ|ناوت\s+بکە\s+بە|ناوت\s+بنێ)\s+([^\s،.,!؟?]+?)(?:ـە|ە|یە)?(?:\s|$|[،.,!؟?])"""),
            Regex("""(?i)(?:your\s+name\s+is|call\s+yourself)\s+([a-zA-Z0-9_\u0600-\u06FF]+)""")
        )
        for (regex in patterns) {
            val match = regex.find(text.trim())
            if (match != null) {
                val rawName = match.groupValues[1]
                    .trim()
                    .removeSuffix("ـە")
                    .removeSuffix("یە")
                    .trim('"', '\'', '«', '»', '[', ']')
                if (rawName.isNotBlank() && rawName.length <= 25) {
                    return rawName
                }
            }
        }
        return null
    }

    /**
     * Detects explicit wake-word lock/unlock commands in Kurdish or English.
     */
    fun detectWakeWordToggle(text: String): Boolean? {
        val normalized = normalizeKurdish(text)
        val lockPhrases = listOf(
            "تا ناوت نەهێنم",
            "تا ناوت نەڵێم",
            "تا نەڵێم",
            "بەبێ ناوی خۆت",
            "قوفڵی ناو چالاک",
            "تەنها کاتێک بانگت دەکەم"
        )
        if (lockPhrases.any { normalized.contains(normalizeKurdish(it)) }) {
            return true
        }

        val unlockPhrases = listOf(
            "قوفڵی ناو لاببە",
            "قوفڵی ناو ناچالاک",
            "بەبێ ناو کار بکە",
            "بەبێ ناو وەڵام بدەرەوە",
            "هەموو کات وەڵام بدەرەوە"
        )
        if (unlockPhrases.any { normalized.contains(normalizeKurdish(it)) }) {
            return false
        }
        return null
    }

    private fun normalizeKurdish(input: String): String {
        return input.lowercase()
            .replace("ك", "ک")
            .replace("ي", "ی")
            .replace("ى", "ی")
            .replace("ة", "ە")
            .replace("ھ", "ه")
            .replace("ـ", "")
            .trim()
    }
}
