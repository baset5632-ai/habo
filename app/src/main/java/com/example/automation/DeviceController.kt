package com.example.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import com.example.data.GeneratedProjectEntity
import com.example.model.FileOperationsSpec
import com.example.model.GeneratedFileSpec
import com.example.model.JarvisAgentResponse
import org.json.JSONArray
import java.io.File

private const val TAG = "DeviceController"

data class ExecutionOutcome(
    val summaryKu: String,
    val createdProject: GeneratedProjectEntity? = null
)

class DeviceController(private val context: Context) {

    private val knownAppPackages = mapOf(
        "telegram" to "org.telegram.messenger",
        "تێلیگرام" to "org.telegram.messenger",
        "تیلیگرام" to "org.telegram.messenger",
        "tiktok" to "com.zhiliaoapp.musically",
        "تیکتۆک" to "com.zhiliaoapp.musically",
        "تیكتۆك" to "com.zhiliaoapp.musically",
        "youtube" to "com.google.android.youtube",
        "یوتیوب" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "واتسئەپ" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "ئینستاگرام" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "فەیسبووک" to "com.facebook.katana",
        "chrome" to "com.android.chrome",
        "کرۆم" to "com.android.chrome",
        "settings" to "com.android.settings",
        "سێتینگ" to "com.android.settings"
    )

    fun executeCommand(response: JarvisAgentResponse): ExecutionOutcome {
        if (!response.shouldExecute || response.command.type.equals("IGNORE", ignoreCase = true)) {
            return ExecutionOutcome(
                summaryKu = "فەرمانەکە فەرامۆش کرا (IGNORE - قوفڵی ناو چالاکە)."
            )
        }

        val cmd = response.command
        return when (cmd.type.uppercase()) {
            "OPEN_SETTINGS" -> {
                val status = openSystemSettings(cmd.targetAppOrIntent)
                if (cmd.uiSteps.isNotEmpty()) {
                    scheduleUiSteps(cmd.uiSteps)
                }
                ExecutionOutcome(summaryKu = status)
            }

            "APP_AUTOMATION" -> {
                val status = launchAppAndAutomate(cmd.targetAppOrIntent, cmd.uiSteps)
                ExecutionOutcome(summaryKu = status)
            }

            "SCREEN_ACTION" -> {
                scheduleUiSteps(cmd.uiSteps)
                ExecutionOutcome(
                    summaryKu = "جێبەجێکردنی ${cmd.uiSteps.size} هەنگاوی سەر شاشە لە ڕێگەی Accessibility."
                )
            }

            "CREATE_CODE_FILES" -> {
                createProjectFiles(cmd.fileOperations)
            }

            else -> {
                // Even if command type is CHAT_ONLY, if file_operations has files, create them!
                if (cmd.fileOperations.files.isNotEmpty()) {
                    createProjectFiles(cmd.fileOperations)
                } else {
                    ExecutionOutcome(summaryKu = "وەڵامی دەنگی و گفتوگۆ جێبەجێ کرا.")
                }
            }
        }
    }

    fun openSystemSettings(targetActionOrKeyword: String): String {
        val raw = targetActionOrKeyword.trim().lowercase()
        val intentAction = when {
            raw.contains("wifi") || raw.contains("وایفای") || raw.contains("ئینتەرنێت") ->
                Settings.ACTION_WIFI_SETTINGS
            raw.contains("bluetooth") || raw.contains("بلوتوز") ->
                Settings.ACTION_BLUETOOTH_SETTINGS
            raw.contains("display") || raw.contains("ڕووناکی") || raw.contains("شاشە") ->
                Settings.ACTION_DISPLAY_SETTINGS
            raw.contains("sound") || raw.contains("دەنگ") ->
                Settings.ACTION_SOUND_SETTINGS
            raw.contains("battery") || raw.contains("پاتری") ->
                Settings.ACTION_BATTERY_SAVER_SETTINGS
            raw.contains("location") || raw.contains("شوێن") || raw.contains("gps") ->
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            raw.contains("accessibility") || raw.contains("دەستگەیشتن") ->
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            raw.contains("storage") || raw.contains("بیرگە") || raw.contains("فایل") ->
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            raw.contains("developer") || raw.contains("گەشەپێدەر") ->
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            raw.startsWith("android.settings.") -> targetActionOrKeyword.trim()
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JarvisAccessibilityService.appendStepLog("کردنەوەی ڕاستەوخۆی سێتینگ: $intentAction")
            "بەشی سێتینگ کرایەوە ($intentAction)"
        } catch (e: Exception) {
            Log.w(TAG, "Specific settings intent failed, falling back to main settings", e)
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                "سێتینگی گشتیی مۆبایل کرایەوە."
            } catch (ex: Exception) {
                "هەنگاوی سێتینگ تۆمار کرا: $intentAction"
            }
        }
    }

    private fun launchAppAndAutomate(targetApp: String, steps: List<com.example.model.UiStep>): String {
        val cleaned = targetApp.trim()
        val resolvedPkg = resolvePackageName(cleaned)
        var launched = false

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(resolvedPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                launched = true
                JarvisAccessibilityService.appendStepLog("ئەپی $resolvedPkg کرایەوە.")
            } else {
                // Check if it's a web/deep-link fallback for Telegram, TikTok, YouTube, etc.
                val webFallback = when {
                    resolvedPkg.contains("telegram") -> "https://web.telegram.org"
                    resolvedPkg.contains("musically") || cleaned.contains("tiktok", true) -> "https://www.tiktok.com"
                    resolvedPkg.contains("youtube") -> "https://m.youtube.com"
                    resolvedPkg.contains("instagram") -> "https://www.instagram.com"
                    resolvedPkg.contains("whatsapp") -> "https://web.whatsapp.com"
                    cleaned.startsWith("http") -> cleaned
                    else -> null
                }
                if (webFallback != null) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                    launched = true
                    JarvisAccessibilityService.appendStepLog("ئەپەکە لەسەر ئامێرەکە نەبوو، لە ڕێگەی وێبەوە کرایەوە: $webFallback")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error launching app $resolvedPkg", e)
        }

        scheduleUiSteps(steps)

        return if (launched) {
            "ئەپی ($resolvedPkg) کرایەوە و ${steps.size} هەنگاوی سەر شاشە خرانە کار."
        } else {
            "فەرمانی ئۆتۆماتیکی بۆ ($resolvedPkg) بە ${steps.size} هەنگاو تۆمار و جێبەجێ کرا."
        }
    }

    private fun scheduleUiSteps(steps: List<com.example.model.UiStep>) {
        if (steps.isEmpty()) return
        val service = JarvisAccessibilityService.instance
        if (service != null) {
            service.executeUiSteps(steps)
        } else {
            steps.forEach { step ->
                JarvisAccessibilityService.appendStepLog(
                    "هەنگاوی ${step.step} [${step.action}]: ئامانج='${step.target}' | بەها='${step.value}'"
                )
            }
        }
    }

    private fun resolvePackageName(input: String): String {
        val lower = input.lowercase()
        for ((key, pkg) in knownAppPackages) {
            if (lower.contains(key)) return pkg
        }
        return if (input.contains(".")) input else "org.telegram.messenger"
    }

    fun createProjectFiles(fileOps: FileOperationsSpec): ExecutionOutcome {
        val requestedPath = fileOps.folderPath.ifBlank {
            "/storage/emulated/0/AI_Projects/Jarvis_Project_${System.currentTimeMillis() % 10000}"
        }
        val projectName = requestedPath.trimEnd('/').substringAfterLast('/').ifBlank {
            "Jarvis_Web_Project"
        }

        val filesToWrite = fileOps.files.ifEmpty {
            listOf(
                GeneratedFileSpec(
                    filename = "index.html",
                    content = """
<!DOCTYPE html>
<html lang="ckb" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$projectName</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <h1>پڕۆژەی دروستکراو لەلایەن Jarvis AI</h1>
        <p>ئەم وێبسایتە بە شێوەی خۆکار دروست کراوە و لەناو فۆڵدەری مۆبایلەکەتدا خەزن کراوە.</p>
        <button onclick="showMessage()">کلیک بکە</button>
        <div id="output"></div>
    </div>
    <script src="app.js"></script>
</body>
</html>
                    """.trimIndent()
                ),
                GeneratedFileSpec(
                    filename = "style.css",
                    content = """
body {
    margin: 0;
    font-family: system-ui, sans-serif;
    background: linear-gradient(135deg, #070B14, #0E1B33);
    color: #F1F5F9;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
}
.container {
    background: rgba(14, 22, 38, 0.9);
    border: 1px solid #00E5FF;
    border-radius: 16px;
    padding: 32px;
    text-align: center;
    max-width: 480px;
    box-shadow: 0 12px 32px rgba(0, 229, 255, 0.15);
}
button {
    background: #00E5FF;
    color: #070B14;
    border: none;
    padding: 12px 24px;
    border-radius: 8px;
    font-weight: bold;
    cursor: pointer;
    margin-top: 16px;
}
                    """.trimIndent()
                ),
                GeneratedFileSpec(
                    filename = "app.js",
                    content = """
function showMessage() {
    const out = document.getElementById('output');
    out.innerHTML = '<p style="color:#10B981;margin-top:16px;">سڵاو! کۆدەکەت بە سەرکەوتوویی کار دەکات.</p>';
}
                    """.trimIndent()
                )
            )
        }

        // 1. Write to App External/Internal AI_Projects directory (always permitted on all Android versions)
        val baseAppDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "AI_Projects/$projectName")
        if (!baseAppDir.exists()) {
            baseAppDir.mkdirs()
        }

        // 2. Also attempt public Download/AI_Projects directory so external file managers see it easily
        var publicSavedDir: File? = null
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val candidate = File(downloadsDir, "AI_Projects/$projectName")
            if (candidate.exists() || candidate.mkdirs()) {
                publicSavedDir = candidate
            }
        } catch (e: Exception) {
            Log.d(TAG, "Public downloads dir not directly writable, using app external storage", e)
        }

        filesToWrite.forEach { spec ->
            val safeName = spec.filename.trimStart('/').ifBlank { "file.txt" }
            val targetFile = File(baseAppDir, safeName)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(spec.content)

            publicSavedDir?.let { pubDir ->
                try {
                    val pubFile = File(pubDir, safeName)
                    pubFile.parentFile?.mkdirs()
                    pubFile.writeText(spec.content)
                } catch (_: Exception) {}
            }
        }

        val finalDisplayPath = publicSavedDir?.absolutePath ?: baseAppDir.absolutePath
        val filesJsonArr = JSONArray()
        filesToWrite.forEach { filesJsonArr.put(it.toJson()) }

        val projectEntity = GeneratedProjectEntity(
            projectName = projectName,
            folderPath = requestedPath,
            actualSavedPath = finalDisplayPath,
            fileCount = filesToWrite.size,
            filesJson = filesJsonArr.toString()
        )

        JarvisAccessibilityService.appendStepLog(
            "فۆڵدەری '$projectName' دروست کرا و ${filesToWrite.size} فایلی کۆدی تێدا نووسرا لە: $finalDisplayPath"
        )

        return ExecutionOutcome(
            summaryKu = "فۆڵدەری ($projectName) دروست کرا و ${filesToWrite.size} فایلی کۆد خەزن کران.",
            createdProject = projectEntity
        )
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Accessibility settings", e)
        }
    }
}
