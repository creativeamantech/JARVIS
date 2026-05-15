package com.mahavtaar.jarvis.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.jarvis.domain.appcontrol.JarvisAccessibilityService
import com.mahavtaar.jarvis.domain.appcontrol.UsageStatsHelper

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isModelAvailable = viewModel.isModelAvailable

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "J.A.R.V.I.S",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Cinematic Arc Reactor Initializing
        ArcReactorLoading()

        Spacer(modifier = Modifier.height(32.dp))

        // System Checklist
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SYSTEM CHECKLIST", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                ChecklistItem(label = "Microphone Access", isGranted = hasMicPermission)
                ChecklistItem(label = "Gemma LLM Offline Model", isGranted = isModelAvailable)
                ChecklistItem(
                    label = "Accessibility Control",
                    isGranted = JarvisAccessibilityService.instance != null,
                    isOptional = true
                )
                ChecklistItem(
                    label = "Usage Stats Access",
                    isGranted = checkUsageStatsPermission(context),
                    isOptional = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasMicPermission) {
            Button(
                onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("GRANT MICROPHONE PERMISSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.background)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!isModelAvailable) {
            Text(
                text = "Model missing. Download Gemma 4 2B IT INT4 to:\n/sdcard/jarvis/models/gemma4-2b-it-int4.bin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onContinue,
            enabled = hasMicPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = if (isModelAvailable) "INITIALIZE SYSTEM" else "CONTINUE WITH MOCK PIPELINE",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ChecklistItem(label: String, isGranted: Boolean, isOptional: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label + if (isOptional) " (Optional)" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isGranted) "✅" else if (isOptional) "⚠" else "❌",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ArcReactorLoading() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.size(100.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.width / 3f

        for (i in 0..2) {
            val scale = 1f + (i * 0.2f)
            drawCircle(
                color = Color(0xFF00BFFF).copy(alpha = 0.2f),
                radius = baseRadius * scale * pulse,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        rotate(rotation, center) {
            for (i in 0..5) {
                val angle = i * 60f
                drawArc(
                    color = Color(0xFF00FFD1).copy(alpha = 0.5f),
                    startAngle = angle, sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.6f, center.y - baseRadius * 0.6f),
                    size = Size(baseRadius * 1.2f, baseRadius * 1.2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00BFFF), Color(0xFF003366)),
                center = center, radius = baseRadius * 0.25f
            ),
            radius = baseRadius * 0.25f, center = center
        )
    }
}
