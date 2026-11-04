package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.GeneratedProjectEntity
import com.example.model.GeneratedFileSpec
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ReactorBlue
import com.example.ui.theme.ReactorCyan
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CodeWorkspaceScreen(
    projects: List<GeneratedProjectEntity>,
    selectedProject: GeneratedProjectEntity?,
    assignedName: String,
    requireWakeWord: Boolean,
    parseProjectFiles: (GeneratedProjectEntity) -> List<GeneratedFileSpec>,
    onSelectProject: (GeneratedProjectEntity?) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onGenerateViaAiPrompt: (String) -> Unit,
    onCreateInstantTemplate: (String, List<GeneratedFileSpec>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var codePrompt by remember { mutableStateOf("") }
    val wakePrefix = if (requireWakeWord && assignedName.isNotBlank()) "$assignedName " else ""

    if (selectedProject != null) {
        val files = remember(selectedProject) { parseProjectFiles(selectedProject) }
        ProjectDetailExplorer(
            project = selectedProject,
            files = files,
            onBack = { onSelectProject(null) },
            context = context,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberObsidian),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "دروستکردنی فۆڵدەر و نووسینی کۆد (Direct Folder & Code Studio)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ReactorCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "هەموو پڕۆژە و فایلە کۆدەکان (HTML, CSS, JS, Python) ڕاستەوخۆ لەناو فۆڵدەری تایبەت لە بیرگەی مۆبایلەکەدا دروست دەکرێن.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Prompt Card for AI Code & Folder Creation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, ReactorCyan),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = null,
                            tint = ReactorCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "داواکردنی پڕۆژەی نوێ بە زیرەکی دەستکرد",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = codePrompt,
                        onValueChange = { codePrompt = it },
                        placeholder = {
                            Text(
                                text = "بۆ نموونە: کۆدێکی وێبسایتی خواردنگەم بۆ دروست بکە بە HTML و CSS و JS...",
                                color = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ReactorCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_studio_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onCreateInstantTemplate(
                                    "Kurdish_Portfolio_Web",
                                    buildSamplePortfolioFiles()
                                )
                            },
                            label = { Text("دروستکردنی خێرای وێبسایتی پۆرتفۆلیۆ", color = TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CyberSurfaceVariant),
                            modifier = Modifier.testTag("instant_template_portfolio")
                        )
                        AssistChip(
                            onClick = {
                                onCreateInstantTemplate(
                                    "Python_AI_Automation",
                                    buildSamplePythonFiles()
                                )
                            },
                            label = { Text("پڕۆژەی Python + Script", color = TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CyberSurfaceVariant),
                            modifier = Modifier.testTag("instant_template_python")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (codePrompt.isNotBlank()) {
                                onGenerateViaAiPrompt("${wakePrefix}کۆد و فۆڵدەری تەواوم بۆ دروست بکە بۆ: $codePrompt")
                                codePrompt = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReactorCyan,
                            contentColor = CyberObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_code_project_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "دروستکردنی فۆڵدەر و نووسینی کۆدەکان",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // List of Generated Project Folders
        item {
            Text(
                text = "فۆڵدەرە دروستکراوەکان لە بیرگەی مۆبایل (${projects.size})",
                style = MaterialTheme.typography.titleLarge,
                color = SolarGold
            )
        }

        if (projects.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = SolarGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هێشتا هیچ فۆڵدەرێکی کۆد دروست نەکراوە",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "بە دەنگ بڵێ «کۆدێکی وێبسایتم بۆ دروست بکە» یان دوگمەی سەرەوە دابگرە.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { proj ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProject(proj) }
                        .testTag("project_folder_card_${proj.projectName}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = SolarGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = proj.projectName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = proj.folderPath,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = ReactorCyan
                                )
                                Text(
                                    text = "${proj.fileCount} فایل خەزن کراوە لە بیرگەدا • کرتە بکە بۆ کردنەوە",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldPulse
                                )
                            }
                        }

                        IconButton(onClick = { onDeleteProject(proj.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "سڕینەوە",
                                tint = CrimsonAlert
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailExplorer(
    project: GeneratedProjectEntity,
    files: List<GeneratedFileSpec>,
    onBack: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var showWebPreview by remember { mutableStateOf(false) }
    val currentFile = files.getOrNull(selectedFileIndex) ?: files.firstOrNull()
    val htmlFile = files.firstOrNull { it.filename.endsWith(".html", ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberObsidian)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "گەڕانەوە",
                        tint = ReactorCyan
                    )
                }
                Column {
                    Text(
                        text = "فۆڵدەر: ${project.projectName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarGold
                    )
                    Text(
                        text = project.actualSavedPath,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            if (htmlFile != null) {
                Button(
                    onClick = { showWebPreview = !showWebPreview },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showWebPreview) SolarGold else ReactorCyan,
                        contentColor = CyberObsidian
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Language, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showWebPreview) "پیشاندانی کۆد" else "بینینی زیندووی وێبسایت",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // File tabs inside the folder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            files.forEachIndexed { idx, fileSpec ->
                val selected = idx == selectedFileIndex && !showWebPreview
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedFileIndex = idx
                        showWebPreview = false
                    },
                    label = {
                        Text(
                            text = fileSpec.filename,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ReactorCyan.copy(alpha = 0.25f),
                        selectedLabelColor = ReactorCyan,
                        containerColor = CyberSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (showWebPreview && htmlFile != null) {
            // Combine CSS & JS inline for seamless WebView preview
            val cssContent = files.firstOrNull { it.filename.endsWith(".css", true) }?.content.orEmpty()
            val jsContent = files.firstOrNull { it.filename.endsWith(".js", true) }?.content.orEmpty()
            val combinedHtml = remember(htmlFile, cssContent, jsContent) {
                var html = htmlFile.content
                if (cssContent.isNotBlank() && !html.contains(cssContent)) {
                    html = html.replace("</head>", "<style>$cssContent</style></head>", ignoreCase = true)
                }
                if (jsContent.isNotBlank() && !html.contains(jsContent)) {
                    html = html.replace("</body>", "<script>$jsContent</script></body>", ignoreCase = true)
                }
                html
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, ReactorCyan)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            loadDataWithBaseURL(null, combinedHtml, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, combinedHtml, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (currentFile != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050811)),
                border = BorderStroke(1.dp, CyberCardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSurface)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentFile.filename,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ReactorCyan
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText(currentFile.filename, currentFile.content)
                                )
                                Toast.makeText(
                                    context,
                                    "کۆدی ${currentFile.filename} کۆپی کرا",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "کۆپیکردنی کۆد",
                                tint = TextSecondary
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        item {
                            Text(
                                text = currentFile.content,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = EmeraldPulse
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildSamplePortfolioFiles(): List<GeneratedFileSpec> {
    return listOf(
        GeneratedFileSpec(
            filename = "index.html",
            content = """
<!DOCTYPE html>
<html lang="ckb" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>پۆرتفۆلیۆی گەشەپێدەری کورد</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header class="hero">
        <h1>سڵاو، من گەشەپێدەری سیستەمی زیرەکم</h1>
        <p>ئەم وێبسایتە بە شێوەی خۆکار لە لایەن Jarvis AI لە ناو بیرگەی مۆبایلەکەت دروست کراوە.</p>
        <button onclick="greetVisitor()">پەیوەندیم پێوە بکە</button>
        <div id="msg"></div>
    </header>
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
    font-family: system-ui, -apple-system, sans-serif;
    background: #070B14;
    color: #F1F5F9;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
}
.hero {
    background: #0E1626;
    border: 2px solid #00E5FF;
    border-radius: 20px;
    padding: 36px;
    max-width: 520px;
    text-align: center;
    box-shadow: 0 10px 30px rgba(0, 229, 255, 0.2);
}
h1 { color: #00E5FF; margin-bottom: 12px; }
button {
    background: #FBBF24;
    color: #070B14;
    border: none;
    padding: 12px 28px;
    border-radius: 10px;
    font-size: 16px;
    font-weight: bold;
    cursor: pointer;
    margin-top: 16px;
}
            """.trimIndent()
        ),
        GeneratedFileSpec(
            filename = "app.js",
            content = """
function greetVisitor() {
    document.getElementById('msg').innerHTML =
        '<p style="color:#10B981;margin-top:16px;font-weight:bold;">سوپاس! نامەکەت بە سەرکەوتوویی تۆمار کرا.</p>';
}
            """.trimIndent()
        )
    )
}

private fun buildSamplePythonFiles(): List<GeneratedFileSpec> {
    return listOf(
        GeneratedFileSpec(
            filename = "main.py",
            content = """
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

class JarvisAutonomousTask:
    def __init__(self, project_name: str):
        self.project_name = project_name

    def execute_analysis(self, items: list[int]) -> dict:
        total = sum(items)
        avg = total / len(items) if items else 0
        return {
            "project": self.project_name,
            "count": len(items),
            "sum": total,
            "average": round(avg, 2)
        }

if __name__ == "__main__":
    agent = JarvisAutonomousTask("Kurdish_AI_Automation")
    result = agent.execute_analysis([12, 24, 36, 48, 60])
    print("ئەنجامی شیکردنەوە:", result)
            """.trimIndent()
        ),
        GeneratedFileSpec(
            filename = "README.md",
            content = """
# Python AI Automation Project
Generated directly on Android storage by **Jarvis AI**.

## Run Instructions
```bash
python3 main.py
```
            """.trimIndent()
        )
    )
}
