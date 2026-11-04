package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class SettingsShortcut(
    val titleKu: String,
    val keyword: String,
    val icon: ImageVector
)

private data class AppAutomationPreset(
    val appTitleKu: String,
    val commandKu: String,
    val packageLabel: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceAutomationScreen(
    isAccessibilityConnected: Boolean,
    activeWindowPackage: String,
    visibleElements: List<String>,
    stepLogs: List<String>,
    floatingWidgetEnabled: Boolean,
    assignedName: String,
    requireWakeWord: Boolean,
    onToggleFloatingWidget: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onTriggerSettingsShortcut: (String) -> Unit,
    onRunAutomationCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wakePrefix = if (requireWakeWord && assignedName.isNotBlank()) "$assignedName " else ""

    val settingsShortcuts = listOf(
        SettingsShortcut("وایفای (Wi-Fi)", "wifi", Icons.Filled.Wifi),
        SettingsShortcut("بلوتوز (Bluetooth)", "bluetooth", Icons.Filled.Bluetooth),
        SettingsShortcut("ڕووناکی و شاشە", "display", Icons.Filled.Brightness6),
        SettingsShortcut("دەنگ و زەنگ", "sound", Icons.Filled.VolumeUp),
        SettingsShortcut("پاتری و وزە", "battery", Icons.Filled.BatteryChargingFull),
        SettingsShortcut("شوێن (GPS)", "location", Icons.Filled.LocationOn),
        SettingsShortcut("بیرگە (Storage)", "storage", Icons.Filled.Storage),
        SettingsShortcut("سێتینگی گشتی", "settings", Icons.Filled.Settings)
    )

    val appPresets = listOf(
        AppAutomationPreset(
            appTitleKu = "تێلیگرام (Telegram)",
            commandKu = "${wakePrefix}بچۆ ناو تێلیگرام و لە بەشی گەڕان نامەیەک بنێرە",
            packageLabel = "org.telegram.messenger"
        ),
        AppAutomationPreset(
            appTitleKu = "تیکتۆک (TikTok)",
            commandKu = "${wakePrefix}بچۆ ناو تیکتۆک و ڤیدیۆکان سکڕۆڵ بکە بۆ خوارەوە",
            packageLabel = "com.zhiliaoapp.musically"
        ),
        AppAutomationPreset(
            appTitleKu = "یوتیوب (YouTube)",
            commandKu = "${wakePrefix}بچۆ ناو یوتیوب و بگەڕێ بۆ فێرکاری کۆدینگ",
            packageLabel = "com.google.android.youtube"
        ),
        AppAutomationPreset(
            appTitleKu = "واتسئەپ (WhatsApp)",
            commandKu = "${wakePrefix}واتسئەپ بکەرەوە و دوایین نامەکان بخوێنەرەوە",
            packageLabel = "com.whatsapp"
        ),
        AppAutomationPreset(
            appTitleKu = "ئینستاگرام (Instagram)",
            commandKu = "${wakePrefix}ئینستاگرام بکەرەوە و پۆستەکان سکڕۆڵ بکە",
            packageLabel = "com.instagram.android"
        ),
        AppAutomationPreset(
            appTitleKu = "وێبگەڕی کرۆم (Chrome)",
            commandKu = "${wakePrefix}کرۆم بکەرەوە و ماڵپەڕی هەواڵی تەکنەلۆژیا بکەرەوە",
            packageLabel = "com.android.chrome"
        )
    )

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
                    text = "کۆنترۆڵی تەواوی مۆبایل و ئەپەکان (Full Device Control)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ReactorCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "کۆنترۆڵی ڕاستەوخۆی شاشە لە ڕێگەی Accessibility Service، چوونە ناو ئەپەکان، و گۆڕانکاری خێرا لە سێتینگی مۆبایلەکەدا.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // 1. Accessibility Service & Floating Widget Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(
                    1.5.dp,
                    if (isAccessibilityConnected) EmeraldPulse else ReactorCyan
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessibilityNew,
                                contentDescription = null,
                                tint = if (isAccessibilityConnected) EmeraldPulse else ReactorCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "بزوێنەری خوێندنەوەی شاشە (Accessibility Service)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isAccessibilityConnected) {
                                        "چالاکە • پەنجەرەی ئێستا: $activeWindowPackage"
                                    } else {
                                        "ئامادەیە بۆ بەستنەوە لە سێتینگی ئەندرۆید"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isAccessibilityConnected) EmeraldPulse else SolarGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenAccessibilitySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReactorCyan,
                            contentColor = CyberObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_accessibility_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "کردنەوەی سێتینگی Accessibility بۆ چالاککردن",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ئایکۆنی خێرای سەر شاشە (Floating Text & Code Widget)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "دوگمەی بچووکی سەر شاشە بۆ نووسینی کۆد و فەرمانی خێرا",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = floatingWidgetEnabled,
                            onCheckedChange = onToggleFloatingWidget,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberObsidian,
                                checkedTrackColor = ReactorCyan
                            )
                        )
                    }
                }
            }
        }

        // 2. Deep Settings Control Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberCardBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "گەڕان و گۆڕانکاری لە سێتینگ (Deep Settings Control)",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بە یەک کلیک یان بە دەنگ بچۆ ناو هەر بەشێکی سێتینگی مۆبایلەکە:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settingsShortcuts.forEach { shortcut ->
                            AssistChip(
                                onClick = { onTriggerSettingsShortcut(shortcut.keyword) },
                                label = {
                                    Text(
                                        text = shortcut.titleKu,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = shortcut.icon,
                                        contentDescription = null,
                                        tint = ReactorCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = CyberSurfaceVariant
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = CyberCardBorder
                                ),
                                modifier = Modifier.testTag("settings_chip_${shortcut.keyword}")
                            )
                        }
                    }
                }
            }
        }

        // 3. App Automation Presets (Telegram, TikTok, YouTube, Social Media)
        item {
            Text(
                text = "کۆنترۆڵکردنی ئەپەکان (Telegram, TikTok, Social Media)",
                style = MaterialTheme.typography.titleLarge,
                color = ReactorCyan
            )
        }

        items(appPresets, key = { it.packageLabel }) { preset ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberCardBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.appTitleKu,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = preset.packageLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = ReactorBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "«${preset.commandKu}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = { onRunAutomationCommand(preset.commandKu) },
                        border = BorderStroke(1.dp, ReactorCyan),
                        modifier = Modifier.testTag("run_app_preset_${preset.packageLabel}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = null,
                            tint = ReactorCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جێبەجێکردن", color = ReactorCyan)
                    }
                }
            }
        }

        // 4. Live Screen Nodes & UI Step Execution Console
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberCardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ڕاپۆرتی زیندووی هەنگاوەکانی سەر شاشە (UI Steps Log)",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldPulse
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (visibleElements.isNotEmpty()) {
                        Text(
                            text = "توخمە بینراوەکانی سەر شاشە: ${visibleElements.joinToString(" • ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ReactorBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF050811))
                            .padding(12.dp)
                    ) {
                        if (stepLogs.isEmpty()) {
                            Text(
                                text = "هێشتا هیچ هەنگاوێکی ئۆتۆماتیکی تۆمار نەکراوە. یەکێک لە ئەپەکان یان سێتینگەکان تاقی بکەرەوە.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stepLogs.take(12).forEach { logLine ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPulse)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = logLine,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
