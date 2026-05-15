package com.mahavtaar.jarvis.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val streamingText by viewModel.currentStreamingText.collectAsState()
    val rmsAmplitude by viewModel.rmsAmplitude.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        HudOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // Conversation Feed
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 64.dp, bottom = 16.dp),
                reverseLayout = true
            ) {
                if (streamingText.isNotEmpty()) {
                    item {
                        MessageBubble(Message(text = streamingText, isUser = false), isStreaming = true)
                    }
                }
                items(messages.reversed()) { message ->
                    MessageBubble(message = message, isStreaming = false)
                }
            }

            // Visualizer and Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcReactorVisualizer(state = assistantState, rmsAmplitude = rmsAmplitude)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (val state = assistantState) {
                            is AssistantState.IDLE -> "SYSTEM STANDBY"
                            is AssistantState.LISTENING -> "LISTENING..."
                            is AssistantState.PROCESSING_STT -> "PROCESSING AUDIO"
                            is AssistantState.THINKING -> "ANALYZING..."
                            is AssistantState.SPEAKING -> "TRANSMITTING"
                            is AssistantState.ERROR -> state.message.uppercase()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (assistantState is AssistantState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Hold to talk button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(64.dp)
                    .background(
                        color = if (assistantState == AssistantState.LISTENING) MaterialTheme.colorScheme.error.copy(alpha=0.8f) else MaterialTheme.colorScheme.primary.copy(alpha=0.8f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .pointerInput(assistantState) {
                        if (assistantState == AssistantState.IDLE || assistantState == AssistantState.LISTENING || assistantState is AssistantState.ERROR || assistantState == AssistantState.SPEAKING || assistantState == AssistantState.THINKING) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val holdJob = kotlinx.coroutines.GlobalScope.launch {
                                    delay(300)
                                    viewModel.startListening()
                                }
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    if (holdJob.isActive) {
                                        // Tap was too quick, abort
                                        holdJob.cancel()
                                    } else {
                                        viewModel.stopListening()
                                    }
                                } else {
                                     viewModel.stopListening()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (assistantState == AssistantState.LISTENING) "RELEASE TO SEND" else "HOLD TO TALK",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .padding(24.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MessageBubble(message: Message, isStreaming: Boolean) {
    val isJarvis = !message.isUser

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isJarvis) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isJarvis) 4.dp else 16.dp,
                bottomEnd = if (isJarvis) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isJarvis) MaterialTheme.colorScheme.surface.copy(alpha=0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text + if (isStreaming) " █" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isJarvis) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ArcReactorVisualizer(state: AssistantState, rmsAmplitude: Float) {
    val infiniteTransition = rememberInfiniteTransition()

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val normalizedAmplitude = max(0f, (rmsAmplitude + 2f) / 10f)
    val amplitudeScale = 1f + (normalizedAmplitude * 0.5f)

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (state) {
            is AssistantState.LISTENING -> 1.2f
            is AssistantState.THINKING -> 1.3f
            is AssistantState.SPEAKING -> 1.8f
            else -> 1.1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(when (state) {
                is AssistantState.LISTENING -> 500
                is AssistantState.THINKING -> 1000
                is AssistantState.SPEAKING -> 300
                else -> 2000
            }, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val finalScale = if (state is AssistantState.LISTENING) pulse * amplitudeScale else pulse

    Canvas(modifier = Modifier.size(120.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.width / 3f

        for (i in 0..3) {
            val scale = 1f + (i * 0.2f)
            val alpha = when (state) {
                is AssistantState.LISTENING -> (1f - i * 0.2f)
                is AssistantState.SPEAKING -> finalScale * (1f - i * 0.15f)
                is AssistantState.THINKING -> finalScale * (1f - i * 0.15f)
                else -> (1f - i * 0.2f) * 0.6f
            }
            val color = if (state is AssistantState.ERROR) Color(0xFFFF3333) else Color(0xFF00BFFF)

            drawCircle(
                color = color.copy(alpha = alpha * 0.3f),
                radius = baseRadius * scale * finalScale,
                center = center,
                style = Stroke(width = (4 - i).dp.toPx())
            )
        }

        val ringColor = if (state is AssistantState.ERROR) Color(0xFFFF3333) else Color(0xFF00FFD1)
        rotate(baseRotation, center) {
            for (i in 0..5) {
                val angle = i * 60f
                drawArc(
                    color = ringColor.copy(alpha = 0.7f),
                    startAngle = angle, sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.6f, center.y - baseRadius * 0.6f),
                    size = Size(baseRadius * 1.2f, baseRadius * 1.2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        val coreColors = if (state is AssistantState.ERROR) {
            listOf(Color(0xFFFF3333), Color(0xFF660000))
        } else {
            listOf(Color(0xFF00BFFF), Color(0xFF003366))
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = coreColors,
                center = center, radius = baseRadius * 0.25f
            ),
            radius = baseRadius * 0.25f, center = center
        )
    }
}
