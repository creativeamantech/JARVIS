package com.mahavtaar.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.jarvis.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val modelPath by viewModel.modelPath.collectAsState()
    val ttsPitch by viewModel.ttsPitch.collectAsState()
    val contextWindowSize by viewModel.contextWindowSize.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("J.A.R.V.I.S Settings", color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Model Path", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(
                value = modelPath,
                onValueChange = { viewModel.updateModelPath(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val formattedPitch = String.format(java.util.Locale.US, "%.2f", ttsPitch)
            Text("TTS Pitch (\$formattedPitch)", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            Slider(
                value = ttsPitch,
                onValueChange = { viewModel.updateTtsPitch(it) },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Context Window Size", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            var contextWindowText by remember(contextWindowSize) { mutableStateOf(contextWindowSize.toString()) }
            OutlinedTextField(
                value = contextWindowText,
                onValueChange = { newValue ->
                    contextWindowText = newValue
                    newValue.toIntOrNull()?.let { viewModel.updateContextWindowSize(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Version Chip
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "v" + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
