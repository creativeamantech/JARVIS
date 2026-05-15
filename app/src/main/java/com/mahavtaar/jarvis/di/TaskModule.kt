package com.mahavtaar.jarvis.di

import android.content.Context
import com.mahavtaar.jarvis.domain.task.IntentParser
import com.mahavtaar.jarvis.domain.task.TaskExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskModule {

    @Provides
    @Singleton
    fun provideIntentParser(): IntentParser {
        return IntentParser()
    }

    @Provides
    @Singleton
    fun provideTaskExecutor(@ApplicationContext context: Context): TaskExecutor {
        return TaskExecutor(context)
    }
}
