package com.mahavtaar.jarvis.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val streamingText by viewModel.currentStreamingText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Conversation Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
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
                ArcReactorVisualizer(state = assistantState)
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

        // Push to talk button
        Button(
            onClick = {
                if (assistantState == AssistantState.LISTENING) {
                    viewModel.stopListening()
                } else {
                    viewModel.startListening()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (assistantState == AssistantState.LISTENING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = if (assistantState == AssistantState.LISTENING) "STOP" else "PUSH TO TALK",
                style = MaterialTheme.typography.labelSmall
            )
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
                containerColor = if (isJarvis) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
fun ArcReactorVisualizer(state: AssistantState) {
    val infiniteTransition = rememberInfiniteTransition()

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (state) {
            is AssistantState.LISTENING -> 1.5f
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

    Canvas(modifier = Modifier.size(120.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.width / 3f

        // Outer rings
        for (i in 0..3) {
            val scale = 1f + (i * 0.2f)
            val alpha = when (state) {
                is AssistantState.LISTENING -> (1f - i * 0.2f)
                is AssistantState.SPEAKING -> pulse * (1f - i * 0.15f)
                is AssistantState.THINKING -> pulse * (1f - i * 0.15f)
                else -> (1f - i * 0.2f) * 0.6f
            }
            val color = if (state is AssistantState.ERROR) Color(0xFFFF3333) else Color(0xFF00BFFF)

            drawCircle(
                color = color.copy(alpha = alpha * 0.3f),
                radius = baseRadius * scale * pulse,
                center = center,
                style = Stroke(width = (4 - i).dp.toPx())
            )
        }

        // Rotating arc segments
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

        // Center core
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
