package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.ApiConfigEntity
import com.example.data.ConversationLogEntity
import com.example.model.ApiProviderType
import com.example.ui.components.IdentityAndWakeWordBar
import com.example.ui.components.ReactorOrbVisualizer
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
import com.example.viewmodel.AgentUiState

@Composable
fun HudCommandScreen(
    isSessionStarted: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    partialTranscript: String,
    audioLevel: Float,
    assignedName: String,
    requireWakeWord: Boolean,
    agentState: AgentUiState,
    latestStrictJson: String,
    apiConfigs: List<ApiConfigEntity>,
    conversationLogs: List<ConversationLogEntity>,
    onToggleStartStop: () -> Unit,
    onTapMic: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSelectProvider: (String) -> Unit,
    onSubmitCommand: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onClearLogs: () -> Unit,
    onNavigateToFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var commandText by remember { mutableStateOf("") }
    var showStrictJsonPanel by remember { mutableStateOf(true) }

    val activeProviderId = apiConfigs.firstOrNull { it.isActive }?.providerId
        ?: ApiProviderType.FIREBASE_GEMINI.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberObsidian)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. One-Click Active API Model Switcher Bar
            item {
                Column {
                    Text(
                        text = "گۆڕینی خێرای بزوێنەری زیرەک (1-Click API Switcher):",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ApiProviderType.entries.forEach { provider ->
                            val isSelected = activeProviderId == provider.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectProvider(provider.id) },
                                label = {
                                    Text(
                                        text = provider.titleKu,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ReactorCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ReactorCyan,
                                    selectedLeadingIconColor = ReactorCyan,
                                    containerColor = CyberSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = CyberCardBorder,
                                    selectedBorderColor = ReactorCyan
                                ),
                                modifier = Modifier.testTag("quick_provider_${provider.id}")
                            )
                        }
                    }
                }
            }

            // 2. Central Reactor Core & START / STOP Button
            item {
                ReactorOrbVisualizer(
                    isSessionStarted = isSessionStarted,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isProcessing = agentState is AgentUiState.Processing,
                    audioLevel = audioLevel,
                    assignedName = assignedName,
                    onToggleStartStop = onToggleStartStop,
                    onTapMic = onTapMic
                )
            }

            // Live Partial Speech Banner if listening
            if (partialTranscript.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                        border = BorderStroke(1.dp, ReactorCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "گوێگرتنی زیندوو: \"$partialTranscript\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ReactorCyan,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 3. Dynamic Name & Wake-Word Lock Status Card
            item {
                IdentityAndWakeWordBar(
                    assignedName = assignedName,
                    requireWakeWord = requireWakeWord,
                    onToggleWakeWord = onToggleWakeWord
                )
            }

            // 4. Quick Test Scenarios (Kurdish Voice & Command Rules)
            item {
                val wakePrefix = if (requireWakeWord && assignedName.isNotBlank()) "$assignedName " else ""
                val quickCommands = listOf(
                    "تۆ ناوت کارزانـە" to "ناولێنان (Dynamic Name)",
                    "تا ناوت نەهێنم هیچ کارێک مەکە" to "چالاککردنی قوفڵی ناو",
                    "ئەمڕۆ کەشوهەوا چۆنە؟" to "تاقیکردنەوەی ژاوەژاو (بێ ناو)",
                    "${wakePrefix}بچۆ ناو تێلیگرام و نامە بنێرە" to "ئۆتۆماتیکی تێلیگرام",
                    "${wakePrefix}لە سێتینگ وایفای بکەرەوە" to "کردنەوەی سێتینگی وایفای",
                    "${wakePrefix}کۆدێکی وێبسایتم بۆ دروست بکە" to "دروستکردنی فۆڵدەر و کۆد"
                )

                Column {
                    Text(
                        text = "فەرمانە ئامادەکراوەکان (بۆ تاقیکردنەوەی خێرا):",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickCommands.forEachIndexed { idx, (cmd, label) ->
                            AssistChip(
                                onClick = { onSubmitCommand(cmd) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = CyberSurfaceVariant
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = CyberCardBorder
                                ),
                                modifier = Modifier.testTag("quick_scenario_$idx")
                            )
                        }
                    }
                }
            }

            // 5. Current Agent Status / Spoken Reply Banner
            item {
                AgentStatusCard(
                    agentState = agentState,
                    onSpeakText = onSpeakText,
                    onNavigateToFiles = onNavigateToFiles
                )
            }

            // 6. Strict JSON Output Inspector Card
            if (latestStrictJson.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyberCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStrictJsonPanel = !showStrictJsonPanel },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Code,
                                        contentDescription = null,
                                        tint = ReactorCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "قاڵبی فەرمیی وەڵام (Strict JSON Output)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ReactorCyan
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            copyToClipboard(context, "Jarvis JSON", latestStrictJson)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "کۆپیکردنی JSON",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showStrictJsonPanel) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = "Toggle JSON",
                                        tint = TextSecondary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showStrictJsonPanel) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF050811))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = latestStrictJson,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = EmeraldPulse,
                                        modifier = Modifier.testTag("strict_json_output_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Conversation & Command Execution History Header
            if (conversationLogs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تۆماری فەرمان و گفتوگۆکان (${conversationLogs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        IconButton(onClick = onClearLogs) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "پاککردنەوەی تۆمار",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                items(conversationLogs, key = { it.id }) { log ->
                    ConversationLogItemCard(
                        log = log,
                        onSpeak = { onSpeakText(log.voiceReply) }
                    )
                }
            }
        }

        // Bottom Voice & Text Command Input Bar
        Surface(
            color = CyberSurface,
            border = BorderStroke(1.dp, CyberCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    placeholder = {
                        Text(
                            text = if (requireWakeWord && assignedName.isNotBlank()) {
                                "فەرمان بنووسە (بە ناوی $assignedName دەست پێ بکە)..."
                            } else {
                                "بە کوردی یان ئینگلیزی فەرمان بنووسە..."
                            },
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReactorCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedContainerColor = CyberObsidian,
                        unfocusedContainerColor = CyberObsidian,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (commandText.isNotBlank()) {
                            onSubmitCommand(commandText)
                            commandText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ReactorCyan)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ناردنی فەرمان",
                        tint = CyberObsidian
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentStatusCard(
    agentState: AgentUiState,
    onSpeakText: (String) -> Unit,
    onNavigateToFiles: () -> Unit
) {
    when (agentState) {
        is AgentUiState.Idle -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberCardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ئامادەیە — دوگمەی START دابگرە یان فەرمانێکی دەنگی و نووسین بنێرە.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        is AgentUiState.Processing -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                border = BorderStroke(1.dp, SolarGold),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = SolarGold,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "لێکدانەوەی فەرمان و ئامادەکردنی کردار...",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarGold
                    )
                }
            }
        }

        is AgentUiState.IgnoredNoise -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1624)),
                border = BorderStroke(1.dp, SolarGold),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeOff,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "دۆخی بێدەنگی (IGNORE) — ناوی یاریدەدەر نەهێنراوە",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarGold
                        )
                        Text(
                            text = "ڕستەی \"${agentState.input}\" فەرامۆش کرا چونکە قوفڵی ناو چالاکە و ناوی \"${agentState.response.assignedName}\"ـی تێدا نەبوو.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        is AgentUiState.Success -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                border = BorderStroke(1.5.dp, ReactorCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("latest_voice_reply_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPulse)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "وەڵامی دەنگی • ${agentState.response.command.type}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ReactorCyan
                            )
                        }
                        IconButton(
                            onClick = { onSpeakText(agentState.response.voiceReply) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "خوێندنەوەی دەنگی",
                                tint = ReactorCyan
                            )
                        }
                    }

                    Text(
                        text = agentState.response.voiceReply,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.testTag("spoken_reply_text")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ئەنجامی جێبەجێکردن: ${agentState.executionSummary}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldPulse
                    )

                    if (agentState.response.command.type == "CREATE_CODE_FILES" ||
                        agentState.response.command.fileOperations.files.isNotEmpty()
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AssistChip(
                            onClick = onNavigateToFiles,
                            label = {
                                Text(
                                    text = "کردنەوەی فۆڵدەر و بینینی کۆدەکان",
                                    color = CyberObsidian,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SolarGold
                            )
                        )
                    }
                }
            }
        }

        is AgentUiState.Error -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A121A)),
                border = BorderStroke(1.dp, CrimsonAlert),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = agentState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CrimsonAlert,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ConversationLogItemCard(
    log: ConversationLogEntity,
    onSpeak: () -> Unit
) {
    var showItemJson by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(
            1.dp,
            if (log.wasIgnored) SolarGold.copy(alpha = 0.4f) else CyberCardBorder
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بەکارهێنەر: ${log.userPrompt}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ReactorBlue,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = log.commandType,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (log.wasIgnored) SolarGold else EmeraldPulse
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.voiceReply,
                style = MaterialTheme.typography.bodyLarge,
                color = if (log.wasIgnored) TextSecondary else TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.executionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    if (!log.wasIgnored && log.voiceReply.isNotBlank()) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "دووبارە خوێندنەوە",
                                tint = ReactorCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { showItemJson = !showItemJson },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = "پیشاندانی JSON",
                            tint = SolarGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showItemJson) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF050811))
                        .padding(10.dp)
                ) {
                    Text(
                        text = log.strictJson,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = EmeraldPulse
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "کۆپی کرا بۆ کلیپبۆرد", Toast.LENGTH_SHORT).show()
}
