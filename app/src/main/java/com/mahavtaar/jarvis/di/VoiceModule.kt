package com.mahavtaar.jarvis.di

import android.content.Context
import com.mahavtaar.jarvis.domain.voice.JarvisTTS
import com.mahavtaar.jarvis.domain.voice.VoiceRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideVoiceRecognizer(@ApplicationContext context: Context): VoiceRecognizer {
        return VoiceRecognizer(context)
    }

    @Provides
    @Singleton
    fun provideJarvisTTS(@ApplicationContext context: Context): JarvisTTS {
        return JarvisTTS(context)
    }
}
