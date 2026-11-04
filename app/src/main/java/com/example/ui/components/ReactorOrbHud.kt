package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import kotlin.math.sin

@Composable
fun ReactorOrbVisualizer(
    isSessionStarted: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioLevel: Float,
    assignedName: String,
    onToggleStartStop: () -> Unit,
    onTapMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_reactor")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSessionStarted) 8000 else 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSessionStarted) 5000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    val coreColor = when {
        isProcessing -> SolarGold
        isSpeaking -> EmeraldPulse
        isListening -> ReactorCyan
        isSessionStarted -> ReactorBlue
        else -> Color(0xFF334155)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleStartStop)
                .testTag("reactor_orb_core"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val dynamicScale = if (isSessionStarted) {
                    1f + 0.04f * sin(pulsePhase) + (audioLevel * 0.08f)
                } else {
                    1f
                }

                // Ambient Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = if (isSessionStarted) 0.32f else 0.12f),
                            coreColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * dynamicScale
                    ),
                    radius = maxRadius
                )

                // Outer Segmented HUD Ring
                rotate(degrees = outerRotation, pivot = center) {
                    val arcRadius = maxRadius * 0.86f
                    val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                    val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                    for (angle in 0 until 360 step 60) {
                        drawArc(
                            color = coreColor.copy(alpha = 0.75f),
                            startAngle = angle.toFloat(),
                            sweepAngle = 36f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Middle Counter-Rotating Telemetry Ring
                rotate(degrees = innerRotation, pivot = center) {
                    val midRadius = maxRadius * 0.70f
                    val topLeft = Offset(center.x - midRadius, center.y - midRadius)
                    val arcSize = Size(midRadius * 2f, midRadius * 2f)
                    for (angle in 0 until 360 step 90) {
                        drawArc(
                            color = if (isSessionStarted) SolarGold.copy(alpha = 0.85f) else coreColor.copy(alpha = 0.4f),
                            startAngle = angle.toFloat() + 15f,
                            sweepAngle = 52f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Core Circle
                drawCircle(
                    color = CyberSurface,
                    radius = maxRadius * 0.54f * dynamicScale,
                    center = center
                )
                drawCircle(
                    color = coreColor,
                    radius = maxRadius * 0.54f * dynamicScale,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Voice Waveform Bars inside the Orb
                val barCount = 7
                val spacing = 9.dp.toPx()
                val totalWidth = (barCount - 1) * spacing
                val startX = center.x - totalWidth / 2f
                for (i in 0 until barCount) {
                    val waveFactor = if (isSpeaking || isListening || isProcessing) {
                        val anim = sin(pulsePhase * 2f + i * 0.9f)
                        0.35f + 0.65f * kotlin.math.abs(anim) + (audioLevel * 0.4f)
                    } else if (isSessionStarted) {
                        0.25f + 0.15f * kotlin.math.abs(sin(pulsePhase + i))
                    } else {
                        0.15f
                    }
                    val barHeight = (maxRadius * 0.42f * waveFactor).coerceAtLeast(6.dp.toPx())
                    val x = startX + i * spacing
                    drawLine(
                        color = coreColor,
                        start = Offset(x, center.y - barHeight / 2f),
                        end = Offset(x, center.y + barHeight / 2f),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Displayed Identity Label
        val displayTitle = if (assignedName.isBlank()) {
            "یاریدەدەری بێ ناو (تۆ ناوم لێ بنێ)"
        } else {
            "ناوی فەرمی: $assignedName"
        }

        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = if (assignedName.isBlank()) TextSecondary else SolarGold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Main START / STOP & Voice Trigger Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleStartStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSessionStarted) CrimsonAlert else ReactorCyan,
                    contentColor = if (isSessionStarted) Color.White else CyberObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("start_stop_button")
            ) {
                Icon(
                    imageVector = if (isSessionStarted) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isSessionStarted) "Stop Jarvis" else "Start Jarvis"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSessionStarted) "STOP (ڕاگرتن)" else "START (دەستپێکردن)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                onClick = onTapMic,
                shape = RoundedCornerShape(14.dp),
                color = if (isListening) EmeraldPulse.copy(alpha = 0.2f) else CyberSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isListening) EmeraldPulse else CyberCardBorder
                ),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("mic_listen_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "تۆمارکردنی دەنگ",
                        tint = if (isListening) EmeraldPulse else ReactorCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "گوێ دەگرێت..." else "مایکرۆفۆن",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun IdentityAndWakeWordBar(
    assignedName: String,
    requireWakeWord: Boolean,
    onToggleWakeWord: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (requireWakeWord) SolarGold.copy(alpha = 0.6f) else CyberCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (requireWakeWord) SolarGold.copy(alpha = 0.18f)
                            else ReactorCyan.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (requireWakeWord) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = "Wake Word Lock",
                        tint = if (requireWakeWord) SolarGold else ReactorCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (requireWakeWord) {
                            "قوفڵی ناو چالاکە (Strict Wake-Word)"
                        } else {
                            "گوێگرتنی کراوە (بێ قوفڵی ناو)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (requireWakeWord) SolarGold else TextPrimary
                    )
                    Text(
                        text = if (requireWakeWord) {
                            if (assignedName.isNotBlank()) {
                                "تەنیا کاتێک وشەی \"$assignedName\" دەبیستێت وەڵام دەداتەوە"
                            } else {
                                "تکایە ناوێک دابنێ بۆ نموونە: «تۆ ناوت کارزانـە»"
                            }
                        } else {
                            "وەڵامی هەموو فەرمانێکی دەنگی و نووسین دەداتەوە"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Switch(
                checked = requireWakeWord,
                onCheckedChange = onToggleWakeWord,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberObsidian,
                    checkedTrackColor = SolarGold,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = CyberSurfaceVariant
                ),
                modifier = Modifier.testTag("wake_word_switch")
            )
        }
    }
}

@Composable
fun FloatingQuickCommandWidget(
    assignedName: String,
    onSubmitQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var quickInput by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        Surface(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(24.dp),
            color = CyberSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ReactorCyan),
            shadowElevation = 10.dp,
            modifier = Modifier.testTag("floating_quick_widget")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = "Floating AI Widget",
                    tint = ReactorCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (assignedName.isBlank()) "فەرمانی خێرا / کۆد" else "$assignedName • کۆد",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ReactorCyan)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ئایکۆنی خێرای سەر شاشە (Floating Widget)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ReactorCyan
                        )
                        IconButton(onClick = { showDialog = false }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "داخستن",
                                tint = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "ڕاستەوخۆ فۆڵدەر و کۆد دروست بکە یان فەرمان بە مۆبایلەکە بکە:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = quickInput,
                        onValueChange = { quickInput = it },
                        placeholder = {
                            Text(
                                text = "بۆ نموونە: کۆدێکی وێبسایتی بازرگانیم بۆ دروست بکە...",
                                color = TextMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("floating_widget_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (quickInput.isNotBlank()) {
                                    val prefix = if (assignedName.isNotBlank()) "$assignedName " else ""
                                    onSubmitQuickCommand("$prefix$quickInput")
                                    quickInput = ""
                                    showDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ReactorCyan,
                                contentColor = CyberObsidian
                            ),
                            modifier = Modifier.testTag("floating_widget_send")
                        ) {
                            Icon(imageVector = Icons.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جێبەجێکردن")
                        }
                    }
                }
            }
        }
    }
}
