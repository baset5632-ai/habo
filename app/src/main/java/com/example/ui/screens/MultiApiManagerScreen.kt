package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ApiConfigEntity
import com.example.model.ApiProviderType
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberObsidian
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ReactorCyan
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MultiApiManagerScreen(
    apiConfigs: List<ApiConfigEntity>,
    assignedName: String,
    requireWakeWord: Boolean,
    onUpdateName: (String) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSelectProvider: (String) -> Unit,
    onSaveProviderConfig: (
        provider: ApiProviderType,
        apiKey: String,
        modelName: String,
        baseUrl: String,
        makeActive: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nameDraft by remember(assignedName) { mutableStateOf(assignedName) }

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
                    text = "بەڕێوەبەری گشتیی API و مۆدێلەکان (Universal Multi-API Manager)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ReactorCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "بە یەک کلیک لە نێوان Gemini, OpenAI GPT-4o, Anthropic Claude 3.5, و مۆدێلە دەرەکییەکان (DeepSeek, OpenRouter, Groq) بگۆڕە.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Dynamic Assistant Name & Wake-Word Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = SolarGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ناسنامەی یاریدەدەر و ناولێنانی دڵخواز",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "دەتوانیت بە دەنگ بڵێیت «تۆ ناوت [ناوێک]ـە» یان لێرە ناوی فەرمی بنووسیت:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            placeholder = {
                                Text("بێ ناو (بۆ نموونە: کارزان، زانا، دیاکۆ)", color = TextMuted)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SolarGold,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_name_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateName(nameDraft)
                                Toast.makeText(
                                    context,
                                    if (nameDraft.isBlank()) "ناوی یاریدەدەر سڕایەوە" else "ناوی یاریدەدەر کرا بە: $nameDraft",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolarGold,
                                contentColor = CyberObsidian
                            ),
                            modifier = Modifier.testTag("save_custom_name_button")
                        ) {
                            Text("خەزنکردن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Provider Cards
        items(ApiProviderType.entries, key = { it.id }) { provider ->
            val savedEntity = apiConfigs.firstOrNull { it.providerId == provider.id }
            val isActive = savedEntity?.isActive == true

            ApiProviderConfigCard(
                provider = provider,
                savedEntity = savedEntity,
                isActive = isActive,
                onActivateOneClick = {
                    onSelectProvider(provider.id)
                    Toast.makeText(
                        context,
                        "مۆدێلی چالاک گۆڕدرا بۆ: ${provider.titleKu}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSaveAndActivate = { apiKey, modelName, baseUrl ->
                    onSaveProviderConfig(provider, apiKey, modelName, baseUrl, true)
                    Toast.makeText(
                        context,
                        "ڕێکخستنی ${provider.titleKu} خەزن کرا و چالاک کرا",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun ApiProviderConfigCard(
    provider: ApiProviderType,
    savedEntity: ApiConfigEntity?,
    isActive: Boolean,
    onActivateOneClick: () -> Unit,
    onSaveAndActivate: (apiKey: String, modelName: String, baseUrl: String) -> Unit
) {
    var apiKey by remember(savedEntity?.apiKey) {
        mutableStateOf(savedEntity?.apiKey ?: "")
    }
    var modelName by remember(savedEntity?.modelName) {
        mutableStateOf(savedEntity?.modelName ?: provider.defaultModel)
    }
    var baseUrl by remember(savedEntity?.baseUrl) {
        mutableStateOf(savedEntity?.baseUrl ?: provider.defaultEndpoint)
    }
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(savedEntity) {
        if (savedEntity != null) {
            apiKey = savedEntity.apiKey
            modelName = savedEntity.modelName
            baseUrl = savedEntity.baseUrl
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) CyberSurfaceVariant else CyberSurface
        ),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) ReactorCyan else CyberCardBorder
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.titleKu,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive) ReactorCyan else TextPrimary
                    )
                    Text(
                        text = provider.subtitleKu,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onActivateOneClick,
                    border = BorderStroke(
                        1.dp,
                        if (isActive) EmeraldPulse else ReactorCyan
                    ),
                    modifier = Modifier.testTag("activate_btn_${provider.id}")
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isActive) EmeraldPulse else ReactorCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "چالاکە" else "هەڵبژاردن",
                        color = if (isActive) EmeraldPulse else ReactorCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick presets for Custom Endpoint (DeepSeek, OpenRouter, Groq)
            if (provider == ApiProviderType.CUSTOM_ENDPOINT) {
                Text(
                    text = "قاڵبە ئامادەکراوەکان (1-Click Presets):",
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            baseUrl = "https://api.deepseek.com/v1"
                            modelName = "deepseek-chat"
                        },
                        label = { Text("DeepSeek V3", color = TextPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = CyberObsidian)
                    )
                    AssistChip(
                        onClick = {
                            baseUrl = "https://openrouter.ai/api/v1"
                            modelName = "meta-llama/llama-3.3-70b-instruct"
                        },
                        label = { Text("OpenRouter", color = TextPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = CyberObsidian)
                    )
                    AssistChip(
                        onClick = {
                            baseUrl = "https://api.groq.com/openai/v1"
                            modelName = "llama-3.3-70b-versatile"
                        },
                        label = { Text("Groq Fast", color = TextPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = CyberObsidian)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (provider.requiresApiKey) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("کلیلی تایبەت (API Key)") },
                    placeholder = { Text("sk-... / AIza...", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = SolarGold
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "پیشاندانی کلیل",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReactorCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_${provider.id}")
                )

                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text(
                    text = "✓ ئەم مۆدێلە ڕاستەوخۆ لە ڕێگەی Firebase AI Logic بەستراوەتەوە و پێویستی بە داخڵکردنی API Key نییە.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldPulse
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("ناوی مۆدێل (Model ID)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReactorCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (provider == ApiProviderType.CUSTOM_ENDPOINT ||
                    provider == ApiProviderType.OPENAI ||
                    provider == ApiProviderType.ANTHROPIC_CLAUDE ||
                    provider == ApiProviderType.GOOGLE_GEMINI_CUSTOM
                ) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("ئێندپۆینت (Base URL)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ReactorCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onSaveAndActivate(apiKey, modelName, baseUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReactorCyan,
                    contentColor = CyberObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_provider_btn_${provider.id}")
            ) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "خەزنکردن و چالاککردنی ئەم مۆدێلە",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
