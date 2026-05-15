package com.mahavtaar.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mahavtaar.jarvis.ui.JarvisNavGraph
import com.mahavtaar.jarvis.domain.voice.SoundEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mahavtaar.jarvis.ui.theme.JarvisTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            SoundEffects.playStartupSound()
        }

        setContent {
            JarvisTheme {
                JarvisNavGraph()
            }
        }
    }
}
