package com.mahavtaar.jarvis.di

import android.content.Context
import com.mahavtaar.jarvis.domain.llm.GemmaEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @Provides
    @Singleton
    fun provideGemmaEngine(@ApplicationContext context: Context): GemmaEngine {
        return GemmaEngine(context)
    }
}
