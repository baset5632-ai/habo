package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.model.ApiProviderType
import com.example.ui.components.FloatingQuickCommandWidget
import com.example.ui.screens.CodeWorkspaceScreen
import com.example.ui.screens.DeviceAutomationScreen
import com.example.ui.screens.HudCommandScreen
import com.example.ui.screens.MultiApiManagerScreen
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ReactorCyan
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisAutonomousApp()
            }
        }
    }
}

private enum class JarvisTab(
    val index: Int,
    val titleKu: String,
    val icon: ImageVector,
    val tag: String
) {
    HUD(0, "ناوەندی زیرەک", Icons.Filled.GraphicEq, "nav_tab_hud"),
    MULTI_API(1, "بەڕێوەبەری API", Icons.Filled.Api, "nav_tab_api"),
    AUTOMATION(2, "کۆنترۆڵی مۆبایل", Icons.Filled.PhoneAndroid, "nav_tab_automation"),
    CODE_FILES(3, "فۆڵدەر و کۆد", Icons.Filled.FolderSpecial, "nav_tab_files")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisAutonomousApp(
    jarvisViewModel: JarvisViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTabIndex by remember { mutableIntStateOf(0) }

    val isSessionStarted by jarvisViewModel.isSessionStarted.collectAsStateWithLifecycle()
    val isListening by jarvisViewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by jarvisViewModel.isSpeaking.collectAsStateWithLifecycle()
    val partialTranscript by jarvisViewModel.partialTranscript.collectAsStateWithLifecycle()
    val audioLevel by jarvisViewModel.audioLevel.collectAsStateWithLifecycle()
    val assignedName by jarvisViewModel.assignedName.collectAsStateWithLifecycle()
    val requireWakeWord by jarvisViewModel.requireWakeWord.collectAsStateWithLifecycle()
    val floatingWidgetEnabled by jarvisViewModel.floatingWidgetEnabled.collectAsStateWithLifecycle()
    val agentState by jarvisViewModel.agentState.collectAsStateWithLifecycle()
    val latestStrictJson by jarvisViewModel.latestStrictJson.collectAsStateWithLifecycle()
    val apiConfigs by jarvisViewModel.apiConfigs.collectAsStateWithLifecycle()
    val conversationLogs by jarvisViewModel.conversationLogs.collectAsStateWithLifecycle()
    val generatedProjects by jarvisViewModel.generatedProjects.collectAsStateWithLifecycle()
    val selectedProject by jarvisViewModel.selectedProject.collectAsStateWithLifecycle()

    val isAccessibilityConnected by jarvisViewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val activeWindowPackage by jarvisViewModel.activeWindowPackage.collectAsStateWithLifecycle()
    val visibleScreenElements by jarvisViewModel.visibleScreenElements.collectAsStateWithLifecycle()
    val accessibilityStepLogs by jarvisViewModel.accessibilityStepLogs.collectAsStateWithLifecycle()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            jarvisViewModel.startSingleVoiceCapture()
        }
    }

    val startStopPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        jarvisViewModel.toggleStartStop(hasAudioPermission = granted)
    }

    if (currentTabIndex != 0) {
        BackHandler {
            if (currentTabIndex == JarvisTab.CODE_FILES.index && selectedProject != null) {
                jarvisViewModel.selectProjectForInspection(null)
            } else {
                currentTabIndex = 0
            }
        }
    }

    val activeProvider = remember(apiConfigs) {
        val id = apiConfigs.firstOrNull { it.isActive }?.providerId
            ?: ApiProviderType.FIREBASE_GEMINI.id
        ApiProviderType.fromId(id)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = CyberObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurface,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSessionStarted) EmeraldPulse else ReactorCyan
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (assignedName.isBlank()) {
                                        "JARVIS AI • یاریدەدەری سەربەخۆ"
                                    } else {
                                        "$assignedName • یاریدەدەری سەربەخۆ"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = activeProvider.titleKu,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ReactorCyan
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (requireWakeWord) SolarGold else CyberCardBorder
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = if (requireWakeWord) "قوفڵی ناو: چالاک" else "گوێگرتنی کراوە",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (requireWakeWord) SolarGold else EmeraldPulse,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 8.dp
            ) {
                JarvisTab.entries.forEach { tab ->
                    val selected = currentTabIndex == tab.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTabIndex = tab.index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.titleKu
                            )
                        },
                        label = {
                            Text(
                                text = tab.titleKu,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberObsidian,
                            selectedTextColor = ReactorCyan,
                            indicatorColor = ReactorCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTabIndex) {
                JarvisTab.HUD.index -> {
                    HudCommandScreen(
                        isSessionStarted = isSessionStarted,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        partialTranscript = partialTranscript,
                        audioLevel = audioLevel,
                        assignedName = assignedName,
                        requireWakeWord = requireWakeWord,
                        agentState = agentState,
                        latestStrictJson = latestStrictJson,
                        apiConfigs = apiConfigs,
                        conversationLogs = conversationLogs,
                        onToggleStartStop = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm || isSessionStarted) {
                                jarvisViewModel.toggleStartStop(hasAudioPermission = hasPerm)
                            } else {
                                // Immediately greet even before or during permission check
                                startStopPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onTapMic = {
                            if (isListening) {
                                jarvisViewModel.stopVoiceCapture()
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    jarvisViewModel.startSingleVoiceCapture()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onToggleWakeWord = { jarvisViewModel.setWakeWordLock(it) },
                        onSelectProvider = { jarvisViewModel.selectProvider(it) },
                        onSubmitCommand = { jarvisViewModel.submitCommand(it) },
                        onSpeakText = { jarvisViewModel.voiceEngine.speakKurdish(it) },
                        onClearLogs = { jarvisViewModel.clearLogs() },
                        onNavigateToFiles = {
                            currentTabIndex = JarvisTab.CODE_FILES.index
                        }
                    )
                }

                JarvisTab.MULTI_API.index -> {
                    MultiApiManagerScreen(
                        apiConfigs = apiConfigs,
                        assignedName = assignedName,
                        requireWakeWord = requireWakeWord,
                        onUpdateName = { jarvisViewModel.updateCustomName(it) },
                        onToggleWakeWord = { jarvisViewModel.setWakeWordLock(it) },
                        onSelectProvider = { jarvisViewModel.selectProvider(it) },
                        onSaveProviderConfig = { provider, apiKey, model, baseUrl, makeActive ->
                            jarvisViewModel.saveProviderSettings(
                                provider = provider,
                                apiKey = apiKey,
                                modelName = model,
                                baseUrl = baseUrl,
                                makeActive = makeActive
                            )
                        }
                    )
                }

                JarvisTab.AUTOMATION.index -> {
                    DeviceAutomationScreen(
                        isAccessibilityConnected = isAccessibilityConnected,
                        activeWindowPackage = activeWindowPackage,
                        visibleElements = visibleScreenElements,
                        stepLogs = accessibilityStepLogs,
                        floatingWidgetEnabled = floatingWidgetEnabled,
                        assignedName = assignedName,
                        requireWakeWord = requireWakeWord,
                        onToggleFloatingWidget = { jarvisViewModel.setFloatingWidgetActive(it) },
                        onOpenAccessibilitySettings = { jarvisViewModel.openAccessibilitySettings() },
                        onTriggerSettingsShortcut = { jarvisViewModel.triggerDirectSettingsAction(it) },
                        onRunAutomationCommand = { cmd ->
                            jarvisViewModel.submitCommand(cmd)
                            currentTabIndex = JarvisTab.HUD.index
                        }
                    )
                }

                JarvisTab.CODE_FILES.index -> {
                    CodeWorkspaceScreen(
                        projects = generatedProjects,
                        selectedProject = selectedProject,
                        assignedName = assignedName,
                        requireWakeWord = requireWakeWord,
                        parseProjectFiles = { jarvisViewModel.parseProjectFiles(it) },
                        onSelectProject = { jarvisViewModel.selectProjectForInspection(it) },
                        onDeleteProject = { jarvisViewModel.deleteProject(it) },
                        onGenerateViaAiPrompt = { cmd ->
                            jarvisViewModel.submitCommand(cmd)
                            currentTabIndex = JarvisTab.HUD.index
                        },
                        onCreateInstantTemplate = { name, files ->
                            jarvisViewModel.createInstantProjectTemplate(name, files)
                        }
                    )
                }
            }

            // Floating Quick-Command & Code Widget overlay
            if (floatingWidgetEnabled) {
                FloatingQuickCommandWidget(
                    assignedName = assignedName,
                    onSubmitQuickCommand = { cmd ->
                        jarvisViewModel.submitCommand(cmd)
                        currentTabIndex = JarvisTab.HUD.index
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 16.dp)
                )
            }
        }
    }
}
